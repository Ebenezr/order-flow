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

        Logger.info(orderId,"KITCHEN","ENTRY_CREATE_KITCHEN_ORDER","START","Creating kitchen order");

        KitchenOrder order =
                KitchenOrder.builder()
                        .kitchenOrderId(UUID.randomUUID().toString())
                        .orderId(orderId)
                        .status("RECEIVED")
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

        return kitchenRepository.findFirstByOrderId(orderId)
                .flatMap(existing -> Mono.just(existing)) // skip
                .switchIfEmpty(
                        kitchenRepository.save(order)
                );
    }

    public Flux<KitchenOrder> getPendingOrders() {
        return kitchenRepository.findByStatus("RECEIVED");
    }

    public Flux<KitchenOrder> getOrdersByStatus(String status) {
        return kitchenRepository.findByStatus(status);
    }

    public Mono<KitchenOrder> startPreparing(String kitchenOrderId) {

        Logger.info(
                kitchenOrderId,
                "KITCHEN",
                "ENTRY_START_PREPARING",
                "START",
                "Starting to prepare kitchen order"
        );

        return kitchenRepository.findByKitchenOrderId(kitchenOrderId)

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
    }

    public Mono<KitchenOrder> markReady(String orderId) {

        Logger.info(
                orderId,
                "KITCHEN",
                "ENTRY_MARK_READY",
                "START",
                "Marking kitchen order as ready"
        );

        return kitchenRepository.findByKitchenOrderId(orderId)
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
    }


}