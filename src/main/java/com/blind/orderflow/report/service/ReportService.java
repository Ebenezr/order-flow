package com.blind.orderflow.report.service;

import com.blind.orderflow.kitchen.entity.KitchenOrder;
import com.blind.orderflow.kitchen.repository.KitchenOrderRepository;
import com.blind.orderflow.order.repository.OrderItemRepository;
import com.blind.orderflow.order.repository.OrderRepository;
import com.blind.orderflow.report.dto.DailySalesReport;
import com.blind.orderflow.report.dto.KitchenPerformanceReport;
import com.blind.orderflow.report.dto.SlowItem;
import com.blind.orderflow.report.dto.TopSellingItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final KitchenOrderRepository kitchenOrderRepository;

    public Mono<DailySalesReport> getDailySalesReport(LocalDate date) {

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        Mono<Long> totalOrders = orderRepository.countCompletedOrdersBetween(startOfDay, endOfDay);
        Mono<Double> totalRevenue = orderRepository.sumRevenueBetween(startOfDay, endOfDay).map(this::round);
        Mono<Double> vatCollected = orderRepository.sumVatBetween(startOfDay, endOfDay).map(this::round);

        Mono<List<TopSellingItem>> topSellingItems = orderRepository
                .findCompletedOrderIdsBetween(startOfDay, endOfDay)
                .collectList()
                .flatMap(orderIds -> {
                    if (orderIds.isEmpty()) {
                        return Mono.just(List.<TopSellingItem>of());
                    }
                    return orderItemRepository.findByOrderIdIn(orderIds)
                            .collectList()
                            .map(items -> items.stream()
                                    .collect(Collectors.groupingBy(
                                            item -> item.getProductId() + "|" + item.getProductName()
                                    ))
                                    .entrySet().stream()
                                    .map(entry -> {
                                        String[] parts = entry.getKey().split("\\|", 2);
                                        long qty = entry.getValue().stream()
                                                .mapToLong(i -> i.getQuantity())
                                                .sum();
                                        double revenue = entry.getValue().stream()
                                                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                                                .sum();
                                        return TopSellingItem.builder()
                                                .productId(parts[0])
                                                .productName(parts.length > 1 ? parts[1] : parts[0])
                                                .totalQuantity(qty)
                                                .totalRevenue(round(revenue))
                                                .build();
                                    })
                                    .sorted(Comparator.comparingLong(TopSellingItem::getTotalQuantity).reversed())
                                    .limit(10)
                                    .collect(Collectors.toList())
                            );
                });

        return Mono.zip(totalOrders, totalRevenue, vatCollected, topSellingItems)
                .map(tuple -> DailySalesReport.builder()
                        .date(date)
                        .totalOrders(tuple.getT1())
                        .totalRevenue(tuple.getT2())
                        .vatCollected(tuple.getT3())
                        .topSellingItems(tuple.getT4())
                        .build()
                );
    }

    public Mono<KitchenPerformanceReport> getKitchenPerformanceReport(LocalDate date) {

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        return kitchenOrderRepository
                .findByStatusAndCreatedAtBetween("READY", startOfDay, endOfDay)
                .collectList()
                .flatMap(kitchenOrders -> {
                    if (kitchenOrders.isEmpty()) {
                        return Mono.just(KitchenPerformanceReport.builder()
                                .date(date)
                                .totalKitchenOrders(0L)
                                .avgPrepTimeMinutes(0.0)
                                .minPrepTimeMinutes(0.0)
                                .maxPrepTimeMinutes(0.0)
                                .slowItems(List.of())
                                .build());
                    }

                    List<Double> prepTimes = kitchenOrders.stream()
                            .map(ko -> (double) Duration.between(ko.getCreatedAt(), ko.getUpdatedAt()).toMinutes())
                            .toList();

                    double avg = round(prepTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0));
                    double min = round(prepTimes.stream().mapToDouble(Double::doubleValue).min().orElse(0));
                    double max = round(prepTimes.stream().mapToDouble(Double::doubleValue).max().orElse(0));

                    // slow orders = those above average prep time, sorted slowest first
                    List<KitchenOrder> slowOrders = kitchenOrders.stream()
                            .filter(ko -> Duration.between(ko.getCreatedAt(), ko.getUpdatedAt()).toMinutes() > avg)
                            .sorted(Comparator.comparingLong(
                                    (KitchenOrder ko) -> Duration.between(ko.getCreatedAt(), ko.getUpdatedAt()).toMinutes()
                            ).reversed())
                            .limit(10)
                            .toList();

                    if (slowOrders.isEmpty()) {
                        return Mono.just(KitchenPerformanceReport.builder()
                                .date(date)
                                .totalKitchenOrders((long) kitchenOrders.size())
                                .avgPrepTimeMinutes(avg)
                                .minPrepTimeMinutes(min)
                                .maxPrepTimeMinutes(max)
                                .slowItems(List.of())
                                .build());
                    }

                    List<String> slowOrderIds = slowOrders.stream()
                            .map(KitchenOrder::getOrderId)
                            .toList();

                    return orderItemRepository.findByOrderIdIn(slowOrderIds)
                            .collectList()
                            .map(items -> {
                                // map orderId -> prep time for lookup
                                var prepTimeByOrderId = slowOrders.stream()
                                        .collect(Collectors.toMap(
                                                KitchenOrder::getOrderId,
                                                ko -> Duration.between(ko.getCreatedAt(), ko.getUpdatedAt()).toMinutes()
                                        ));

                                List<SlowItem> slowItems = items.stream()
                                        .map(item -> SlowItem.builder()
                                                .productId(item.getProductId())
                                                .productName(item.getProductName())
                                                .orderId(item.getOrderId())
                                                .prepTimeMinutes(prepTimeByOrderId.getOrDefault(item.getOrderId(), 0L))
                                                .build())
                                        .sorted(Comparator.comparingLong(SlowItem::getPrepTimeMinutes).reversed())
                                        .collect(Collectors.toList());

                                return KitchenPerformanceReport.builder()
                                        .date(date)
                                        .totalKitchenOrders((long) kitchenOrders.size())
                                        .avgPrepTimeMinutes(avg)
                                        .minPrepTimeMinutes(min)
                                        .maxPrepTimeMinutes(max)
                                        .slowItems(slowItems)
                                        .build();
                            });
                });
    }

    private double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}

