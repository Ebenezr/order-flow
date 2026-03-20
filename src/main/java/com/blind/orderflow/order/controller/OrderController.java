package com.blind.orderflow.order.controller;

import com.blind.orderflow.order.dto.CreateOrderItemRequest;
import com.blind.orderflow.order.entity.Order;
import com.blind.orderflow.order.entity.OrderItem;
import com.blind.orderflow.order.service.OrderService;
import com.blind.orderflow.shared.utils.apis.ApiResponse;
import com.blind.orderflow.shared.utils.apis.ResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Mono<ApiResponse<Order>> createOrder(
            @RequestParam String customerId,
            @RequestBody Flux<CreateOrderItemRequest> items
    ) {

        return orderService
                .createOrder(customerId, items)
                .flatMap(order ->
                        ResponseFactory.getRequestRefId()
                                .flatMap(requestId ->
                                        ResponseFactory.success(order, requestId)
                                )
                );
    }

    @PostMapping("/{orderId}/complete")
    public Mono<?> completeOrder(@PathVariable String orderId) {

        return orderService.completeOrder(orderId)
                .flatMap(order ->
                        ResponseFactory.success(order, ResponseFactory.newRequestRefId())
                );
    }

    @GetMapping("/{orderId}")
    public Mono<ApiResponse<Order>> getOrder(@PathVariable String orderId) {

        return orderService
                .getOrder(orderId)
                .flatMap(order ->
                        ResponseFactory.getRequestRefId()
                                .flatMap(requestId ->
                                        ResponseFactory.success(order, requestId)
                                )
                );
    }

    @GetMapping("/{orderId}/items")
    public Mono<ApiResponse<List<OrderItem>>> getOrderItems(@PathVariable String orderId) {

        return orderService.getOrderItems(orderId)
                .collectList()
                .flatMap(items ->
                        ResponseFactory.getRequestRefId()
                                .flatMap(requestId ->
                                        ResponseFactory.success(items, requestId)
                                )
                );
    }

        @PostMapping("/{orderId}/cancel")
        public Mono<ApiResponse<Order>> cancelOrder(@PathVariable String orderId,
        @RequestBody String reason
        ) {
            return orderService
                    .cancelOrder(orderId,reason)
                    .flatMap(order ->
                            ResponseFactory.getRequestRefId()
                                    .flatMap(requestId ->
                                            ResponseFactory.success(order, requestId)
                                    )
                    );
        }
}