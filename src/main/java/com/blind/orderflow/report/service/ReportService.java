package com.blind.orderflow.report.service;

import com.blind.orderflow.inventory.repository.InventoryReservationRepository;
import com.blind.orderflow.kitchen.entity.KitchenOrder;
import com.blind.orderflow.kitchen.repository.KitchenOrderRepository;
import com.blind.orderflow.order.entity.Order;
import com.blind.orderflow.order.repository.OrderItemRepository;
import com.blind.orderflow.order.repository.OrderRepository;
import com.blind.orderflow.payment.repository.PaymentRepository;
import com.blind.orderflow.report.dto.CancellationReport;
import com.blind.orderflow.report.dto.CategoryRevenue;
import com.blind.orderflow.report.dto.DailySalesReport;
import com.blind.orderflow.report.dto.KitchenPerformanceReport;
import com.blind.orderflow.report.dto.MonthlySummaryReport;
import com.blind.orderflow.report.dto.PaymentReport;
import com.blind.orderflow.report.dto.ProcessingTimeReport;
import com.blind.orderflow.report.dto.SlowItem;
import com.blind.orderflow.report.dto.TopSellingItem;
import com.blind.orderflow.shared.utils.enums.OrderStatus;
import com.blind.orderflow.shared.utils.logging.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final KitchenOrderRepository kitchenOrderRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryReservationRepository inventoryReservationRepository;

    public Mono<DailySalesReport> getDailySalesReport(LocalDate date) {

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        Mono<Long> totalOrders = orderRepository.countCompletedOrdersBetween(startOfDay, endOfDay)
                .defaultIfEmpty(0L);
        Mono<Double> totalRevenue = orderRepository.sumRevenueBetween(startOfDay, endOfDay)
                .defaultIfEmpty(0.0)
                .map(this::round);
        Mono<Double> vatCollected = orderRepository.sumVatBetween(startOfDay, endOfDay)
                .defaultIfEmpty(0.0)
                .map(this::round);

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
                                    .map(entry -> {                                         String[] parts = entry.getKey().split("\\|", 2);
                                         long qty = entry.getValue().stream()
                                                 .mapToLong(i -> i.getQuantity())
                                                 .sum();
                                         BigDecimal revenue = entry.getValue().stream()
                                                 .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                                                 .reduce(BigDecimal.ZERO, BigDecimal::add);
                                         return TopSellingItem.builder()
                                                 .productId(parts[0])
                                                 .productName(parts.length > 1 ? parts[1] : parts[0])
                                                 .totalQuantity(qty)
                                                 .totalRevenue(revenue.setScale(2, RoundingMode.HALF_UP))
                                                 .build();
                                    })
                                    .sorted(Comparator.comparingLong(TopSellingItem::getTotalQuantity).reversed())
                                    .limit(10)
                                    .collect(Collectors.toList())
                            );
                });

        Mono<DailySalesReport> pipeline =  Mono.zip(totalOrders, totalRevenue, vatCollected, topSellingItems)
                .map(tuple -> DailySalesReport.builder()
                        .date(date)
                        .totalOrders(tuple.getT1())
                        .totalRevenue(tuple.getT2())
                        .vatCollected(tuple.getT3())
                        .topSellingItems(tuple.getT4())
                        .build()
                );
        return Logger.logMono(pipeline, "REPORT", "GET_DAILY_SALES_REPORT", start);
    }

    public Mono<KitchenPerformanceReport> getKitchenPerformanceReport(LocalDate date) {

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        Mono<KitchenPerformanceReport> pipeline =  kitchenOrderRepository
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
        return Logger.logMono(pipeline, "REPORT", "GET_KITCHEN_PERFOMANCE_REPORT", start);
    }

    public Mono<PaymentReport> getPaymentReport(LocalDate date) {

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        Mono<Long> successful = paymentRepository.countSuccessfulBetween(startOfDay, endOfDay)
                .defaultIfEmpty(0L);
        Mono<Long> failed = paymentRepository.countFailedBetween(startOfDay, endOfDay)
                .defaultIfEmpty(0L);

        Mono<PaymentReport> pipeline =  Mono.zip(successful, failed)
                .map(tuple -> {
                    long s = tuple.getT1();
                    long f = tuple.getT2();
                    long total = s + f;
                    double failureRate = total == 0 ? 0.0 : round(((double) f / total) * 100);

                    return PaymentReport.builder()
                            .date(date)
                            .successfulPayments(s)
                            .failedPayments(f)
                            .failureRate(failureRate)
                            .build();
                });
        return Logger.logMono(pipeline, "REPORT", "GET_PAYMENT_REPORT", start);
    }

    public Mono<CancellationReport> getCancellationReport(LocalDate date) {

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        Mono<CancellationReport> pipeline =  orderRepository
                .findByStatusAndCreatedAtBetween(OrderStatus.CANCELLED, startOfDay, endOfDay)
                .collectList()
                .map(cancelledOrders -> {
                    Map<String, Long> reasons = cancelledOrders.stream()
                            .collect(Collectors.groupingBy(
                                    order -> order.getCancellationReason() != null
                                            ? order.getCancellationReason()
                                            : "unknown",
                                    Collectors.counting()
                            ))
                            .entrySet().stream()
                            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue,
                                    (a, b) -> a,
                                    LinkedHashMap::new
                            ));

                    return CancellationReport.builder()
                            .date(date)
                            .cancelledOrders((long) cancelledOrders.size())
                            .reasons(reasons)
                            .build();
                });
        return Logger.logMono(pipeline, "REPORT", "GET_CANCELLATION_REPORT", start);
    }

    public Mono<ProcessingTimeReport> getProcessingTimeReport(LocalDate date) {

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        // Avg inventory reserve time (createdAt → expiresAt minus 5 min offset isn't useful;
        // instead compute from reservation createdAt to order updatedAt is complex.
        // Simplest: use Duration between reservation createdAt and expiresAt minus 5 min = processing time)
        // Better approach: reservations store createdAt; the time from order creation to reservation creation = reserve time
        Mono<Double> avgInventoryMs = inventoryReservationRepository
                .findByStatusAndCreatedAtBetween("CONFIRMED", startOfDay, endOfDay)
                .collectList()
                .flatMap(reservations -> {
                    if (reservations.isEmpty()) {
                        return Mono.just(0.0);
                    }
                    // For each reservation, look up the order's createdAt to compute reserve latency
                    var orderIds = reservations.stream()
                            .map(r -> r.getOrderId())
                            .distinct()
                            .toList();

                    return orderRepository.findByStatusAndCreatedAtBetween(OrderStatus.CONFIRMED, startOfDay, endOfDay)
                            .mergeWith(orderRepository.findByStatusAndCreatedAtBetween(OrderStatus.COMPLETED, startOfDay, endOfDay))
                            .mergeWith(orderRepository.findByStatusAndCreatedAtBetween(OrderStatus.IN_KITCHEN, startOfDay, endOfDay))
                            .mergeWith(orderRepository.findByStatusAndCreatedAtBetween(OrderStatus.READY, startOfDay, endOfDay))
                            .collectList()
                            .map(orders -> {
                                var orderCreatedAtMap = orders.stream()
                                        .collect(Collectors.toMap(
                                                o -> o.getOrderId(),
                                                o -> o.getCreatedAt(),
                                                (a, b) -> a
                                        ));

                                double avg = reservations.stream()
                                        .filter(r -> orderCreatedAtMap.containsKey(r.getOrderId()))
                                        .mapToDouble(r -> {
                                            LocalDateTime orderCreated = orderCreatedAtMap.get(r.getOrderId());
                                            return Duration.between(orderCreated, r.getCreatedAt()).toMillis();
                                        })
                                        .average()
                                        .orElse(0.0);
                                return round(avg);
                            });
                });

        // Avg payment processing time (createdAt → updatedAt on payment_transactions)
        Mono<Double> avgPaymentMs = paymentRepository
                .findByStatusInAndCreatedAtBetween(List.of("SUCCESS", "FAILED"), startOfDay, endOfDay)
                .collectList()
                .map(transactions -> {
                    if (transactions.isEmpty()) return 0.0;
                    double avg = transactions.stream()
                            .filter(tx -> tx.getUpdatedAt() != null)
                            .mapToDouble(tx -> Duration.between(tx.getCreatedAt(), tx.getUpdatedAt()).toMillis())
                            .average()
                            .orElse(0.0);
                    return round(avg);
                });

        // Avg kitchen prep time (createdAt → updatedAt on kitchen_orders with status READY)
        Mono<Double> avgKitchenMin = kitchenOrderRepository
                .findByStatusAndCreatedAtBetween("READY", startOfDay, endOfDay)
                .collectList()
                .map(kitchenOrders -> {
                    if (kitchenOrders.isEmpty()) return 0.0;
                    double avg = kitchenOrders.stream()
                            .mapToDouble(ko -> (double) Duration.between(ko.getCreatedAt(), ko.getUpdatedAt()).toMinutes())
                            .average()
                            .orElse(0.0);
                    return round(avg);
                });

        Mono<ProcessingTimeReport> pipeline =  Mono.zip(avgInventoryMs, avgPaymentMs, avgKitchenMin)
                .map(tuple -> ProcessingTimeReport.builder()
                        .date(date)
                        .avgInventoryReserveTimeMs(tuple.getT1())
                        .avgPaymentProcessingMs(tuple.getT2())
                        .avgKitchenPrepMinutes(tuple.getT3())
                        .build()
                );
        return Logger.logMono(pipeline, "REPORT", "GET_PROCESSING_TIME_REPORT", start);
    }

    public Mono<List<CategoryRevenue>> getRevenueByCategoryReport(LocalDate date) {

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        Mono<List<CategoryRevenue>> pipeline =  orderRepository
                .findCompletedOrderIdsBetween(startOfDay, endOfDay)
                .collectList()
                .flatMap(orderIds -> {
                    if (orderIds.isEmpty()) {
                        return Mono.just(List.<CategoryRevenue>of());
                    }
                    return orderItemRepository.findByOrderIdIn(orderIds)
                            .collectList()
                            .map(items -> items.stream()
                                    .collect(Collectors.groupingBy(
                                            item -> item.getCategory() != null ? item.getCategory() : "Uncategorized",
                                            Collectors.summingDouble(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())).doubleValue())
                                    ))
                                    .entrySet().stream()
                                    .map(entry -> CategoryRevenue.builder()
                                            .category(entry.getKey())
                                            .revenue(round(entry.getValue()))
                                            .build())
                                    .sorted(Comparator.comparingDouble(CategoryRevenue::getRevenue).reversed())
                                    .toList()
                            );
                });

        return Logger.logMono(pipeline, "REPORT", "GET_REVENUE_BY_CATEGORY_REPORT", start);
    }

    public Mono<MonthlySummaryReport> getMonthlySummaryReport(YearMonth yearMonth) {

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);

        String period = yearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + yearMonth.getYear();

        Mono<Double> grossSales = orderRepository.sumRevenueBetween(startOfMonth, endOfMonth)
                .defaultIfEmpty(0.0)
                .map(this::round);

        Mono<Double> vatCollected = orderRepository.sumVatBetween(startOfMonth, endOfMonth)
                .defaultIfEmpty(0.0)
                .map(this::round);

        Mono<MonthlySummaryReport> pipeline =  Mono.zip(grossSales, vatCollected)
                .map(tuple -> {
                    double gross = tuple.getT1();
                    double vat = tuple.getT2();
                    double net = round(gross - vat);

                    return MonthlySummaryReport.builder()
                            .period(period)
                            .grossSales(gross)
                            .netSales(net)
                            .vatCollected(vat)
                            .build();
                });

        return Logger.logMono(pipeline, "REPORT", "GET_MONTHLY_SUMMARY_REPORT", start);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
