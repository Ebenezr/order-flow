package com.blind.orderflow.kitchen.controller;

import com.blind.orderflow.kitchen.service.KitchenService;
import com.blind.orderflow.shared.utils.apis.ResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/kitchen")
public class KitchenController {

    private final KitchenService kitchenService;

    @GetMapping("/orders")
    public Mono<?> getPendingOrders() {

        return kitchenService.getPendingOrders()
                .collectList()
                .flatMap(orders ->
                        ResponseFactory.success(orders, ResponseFactory.newRequestRefId())
                );
    }

    @GetMapping("/orders/status")
    public Mono<?> getOrdersByStatus(@RequestParam String status) {
        return kitchenService.getOrdersByStatus(status)
                .collectList()
                .flatMap(orders ->
                        ResponseFactory.success(orders, ResponseFactory.newRequestRefId())
                );
    }

    @PutMapping("/orders/{orderId}/start")
    public Mono<?> startPreparing(@PathVariable String orderId) {

        return kitchenService.startPreparing(orderId)
                .flatMap(order ->
                        ResponseFactory.success(order, ResponseFactory.newRequestRefId())
                );
    }

    @PutMapping("/orders/{orderId}/ready")
    public Mono<?> markReady(@PathVariable String orderId) {

        return kitchenService.markReady(orderId)
                .flatMap(order ->
                        ResponseFactory.success(order, ResponseFactory.newRequestRefId())
                );
    }
}