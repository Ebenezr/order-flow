package com.blind.orderflow.kitchen.repository;

import com.blind.orderflow.kitchen.entity.KitchenOrder;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface KitchenOrderRepository
        extends ReactiveCrudRepository<KitchenOrder, Long> {

    Mono<KitchenOrder> findFirstByOrderId(String orderId);

    Mono<KitchenOrder> findByKitchenOrderId(String kitchenOrderId);

    Flux<KitchenOrder> findByStatusAndCreatedAtBetween(String status, LocalDateTime from, LocalDateTime to);


    @Query("""
    SELECT * FROM kitchen_orders
    WHERE status = :status
    ORDER BY created_at DESC
    LIMIT :limit OFFSET :offset
""")
    Flux<KitchenOrder> findPagedByStatus(String status, int limit, int offset);

    @Query("""
    SELECT * FROM kitchen_orders
    ORDER BY created_at DESC
    LIMIT :limit OFFSET :offset
""")
    Flux<KitchenOrder> findPaged(int limit, int offset);


    @Query("""
    SELECT COUNT(*) FROM kitchen_orders
    WHERE status = :status
""")
    Mono<Long> countByStatus(String status);

    @Query("""
    SELECT COUNT(*) FROM kitchen_orders
    """)
    Mono<Long> countAll();
}