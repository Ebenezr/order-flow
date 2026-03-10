package com.blind.orderflow.order.repository;


import com.blind.orderflow.order.entity.OrderItem;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.List;

public interface OrderItemRepository extends ReactiveCrudRepository<OrderItem, Long> {

    Flux<OrderItem> findByOrderId(String orderId);

    Flux<OrderItem> findByOrderIdIn(List<String> orderIds);
}