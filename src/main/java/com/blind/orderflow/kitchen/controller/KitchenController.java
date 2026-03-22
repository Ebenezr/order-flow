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
    public Mono<?> getKitchenOrders(
            @RequestParam(defaultValue = "0") int currentPage,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false)String status
    ) {

        return kitchenService.getKitchenOrders(currentPage,pageSize,status)
                .flatMap(ResponseFactory::successWithContext);
    }

    @PutMapping("/orders/{orderId}/start")
    public Mono<?> startPreparing(@PathVariable String orderId) {

        return kitchenService.startPreparing(orderId)
                .flatMap(ResponseFactory::successWithContext);
    }

    @PutMapping("/orders/{orderId}/ready")
    public Mono<?> markReady(@PathVariable String orderId) {

        return kitchenService.markReady(orderId)
                .flatMap(order ->
                        ResponseFactory.getRequestRefId()
                                .flatMap(requestId ->
                        ResponseFactory.success(order, requestId))
                );
    }
}