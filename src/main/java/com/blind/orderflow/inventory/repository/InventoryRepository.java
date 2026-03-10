package com.blind.orderflow.inventory.repository;

import com.blind.orderflow.inventory.entity.Inventory;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface InventoryRepository extends ReactiveCrudRepository<Inventory, Long> {

    @Modifying
    @Query("""
        UPDATE inventory
        SET available_quantity = available_quantity - :qty
        WHERE product_id = :productId
        AND available_quantity >= :qty
    """)
    Mono<Integer> reserveStock(String productId, Integer qty);

    Mono<Inventory> findByProductId(String productId);
}