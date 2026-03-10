package com.blind.orderflow.inventory.repository;

import com.blind.orderflow.inventory.entity.InventoryReservation;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface InventoryReservationRepository
        extends ReactiveCrudRepository<InventoryReservation, Long> {

    @Query("""
    SELECT * FROM inventory_reservations
    WHERE status = 'RESERVED'
    AND expires_at <= NOW()
    """)

    Flux<InventoryReservation> findExpiredReservations();

    Flux<InventoryReservation> findByOrderId(String orderId);
}