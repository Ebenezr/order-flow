package com.blind.orderflow.kitchen.repository;

import com.blind.orderflow.kitchen.entity.KitchenOrder;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface KitchenOrderRepository
        extends ReactiveCrudRepository<KitchenOrder, Long> {

    Mono<KitchenOrder> findFirstByOrderId(String orderId);

    Flux<KitchenOrder> findByStatus(String status);
    Mono<KitchenOrder> findByKitchenOrderId(String kitchenOrderId);

}