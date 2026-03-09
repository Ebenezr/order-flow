package com.blind.orderflow.order.repository;

import com.blind.orderflow.order.entity.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {

    Mono<Order> findByOrderId(String orderId);
}