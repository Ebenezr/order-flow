package com.blind.orderflow.order.repository;

import com.blind.orderflow.order.entity.Order;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {

    Mono<Order> findByOrderId(String orderId);


    @Query("SELECT COUNT(*) FROM orders WHERE status IN ('CONFIRMED','IN_KITCHEN','READY','COMPLETED') AND created_at BETWEEN :from AND :to")
    Mono<Long> countCompletedOrdersBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE status IN ('CONFIRMED','IN_KITCHEN','READY','COMPLETED') AND created_at BETWEEN :from AND :to")
    Mono<Double> sumRevenueBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(vat), 0) FROM orders WHERE status IN ('CONFIRMED','IN_KITCHEN','READY','COMPLETED') AND created_at BETWEEN :from AND :to")
    Mono<Double> sumVatBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT o.order_id FROM orders o WHERE o.status IN ('CONFIRMED','IN_KITCHEN','READY','COMPLETED') AND o.created_at BETWEEN :from AND :to")
    Flux<String> findCompletedOrderIdsBetween(LocalDateTime from, LocalDateTime to);
}