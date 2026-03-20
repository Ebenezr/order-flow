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
        SET available_quantity = available_quantity - :quantity
        WHERE ingredient_id = :ingredientId
        AND available_quantity >= :quantity
    """)
    Mono<Integer> reserveStock(String ingredientId, Integer qty);

    @Modifying
    @Query("""
    UPDATE inventory
    SET available_quantity = available_quantity + :quantity,
        updated_at = NOW()
    WHERE ingredient_id = :ingredientId
""")
    Mono<Integer> releaseStock(String ingredientId, int quantity);


    Mono<Inventory> findByIngredientId(String ingredientId);
}