package com.blind.orderflow.kitchen.service;

import com.blind.orderflow.kitchen.entity.KitchenOrder;
import com.blind.orderflow.kitchen.repository.KitchenOrderRepository;
import com.blind.orderflow.order.service.OrderService;
import com.blind.orderflow.shared.exceptions.NotFoundException;
import com.blind.orderflow.shared.utils.enums.OrderStatus;
import com.blind.orderflow.shared.utils.logging.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KitchenService {

    private final KitchenOrderRepository kitchenRepository;
    private final OrderService orderService;

    public Mono<KitchenOrder> createKitchenOrder(String orderId) {

        LocalDateTime start = LocalDateTime.now();

        KitchenOrder order =
                KitchenOrder.builder()
                        .kitchenOrderId(UUID.randomUUID().toString())
                        .orderId(orderId)
                        .status("RECEIVED")
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

        Mono<KitchenOrder> pipeline= kitchenRepository.findFirstByOrderId(orderId)
                .flatMap(existing -> Mono.just(existing)) // skip
                .switchIfEmpty(
                        kitchenRepository.save(order)
                );
        return Logger.logMono(pipeline, "KITCHEN", "CREATE_KITCHEN_ORDER", start);
    }

    public Flux<KitchenOrder> getPendingOrders() {
        LocalDateTime start = LocalDateTime.now();
        Flux<KitchenOrder> pipeline= kitchenRepository.findByStatus("RECEIVED");
        return Logger.logFlux(pipeline, "KITCHEN", "GET_PENDING_KITCHEN_ORDERS", start);

    }

    public Flux<KitchenOrder> getOrdersByStatus(String status) {
        LocalDateTime start = LocalDateTime.now();
        Flux<KitchenOrder> pipeline= kitchenRepository.findByStatus(status);
        return Logger.logFlux(pipeline, "KITCHEN", "GET_KITCHEN_ORDERS_BY_STATUS", start);
    }

    public Mono<KitchenOrder> startPreparing(String kitchenOrderId) {

        LocalDateTime start = LocalDateTime.now();

        Mono<KitchenOrder> pipeline= kitchenRepository.findByKitchenOrderId(kitchenOrderId)

                .switchIfEmpty(
                        Mono.error(new NotFoundException("Kitchen order not found"))
                )

                .flatMap(order -> {

                    order.setStatus("PREPARING");
                    order.setUpdatedAt(LocalDateTime.now());

                    return kitchenRepository.save(order);
                }).flatMap(saved ->
                        orderService.updateOrderStatus(saved.getOrderId(), OrderStatus.PREPARING)
                                .thenReturn(saved)
                );
        return Logger.logMono(pipeline, "KITCHEN", "START_PREP", start);
    }

    public Mono<KitchenOrder> markReady(String orderId) {

        LocalDateTime start = LocalDateTime.now();

        Mono<KitchenOrder> pipeline= kitchenRepository.findByKitchenOrderId(orderId)
                .switchIfEmpty(
                        Mono.error(new NotFoundException("Kitchen order not found"))
                )
                .flatMap(order -> {

                    order.setStatus("READY");
                    order.setUpdatedAt(LocalDateTime.now());

                    return kitchenRepository.save(order);
                }).flatMap(saved ->
                        orderService.updateOrderStatus(saved.getOrderId(), OrderStatus.READY)
                                .thenReturn(saved)
                );

        return Logger.logMono(pipeline, "KITCHEN", "MARK_READY", start);
    }


}