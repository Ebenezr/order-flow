package com.blind.orderflow.inventory.service;

import com.blind.orderflow.inventory.repository.InventoryRepository;
import com.blind.orderflow.inventory.repository.InventoryReservationRepository;
import com.blind.orderflow.shared.utils.logging.Logger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationCleanupService {

    private final InventoryReservationRepository reservationRepository;
    private final InventoryRepository inventoryRepository;

    @Scheduled(fixedRate = 60000) // every 1 minute
    public void cleanupExpiredReservations() {

        reservationRepository.findExpiredReservations()

            .flatMap(res ->

                inventoryRepository.findByProductId(res.getProductId())

                    .flatMap(inv -> {

                        inv.setAvailableQuantity(
                                inv.getAvailableQuantity() + res.getQuantity()
                        );

                        res.setStatus("RELEASED");

                        return inventoryRepository.save(inv)
                                .then(reservationRepository.save(res));
                    })
            )
            .doOnNext(r ->
                    Logger.info(
                            r.getOrderId(),
                            "INVENTORY",
                            "RESERVATION_EXPIRED",
                            "INFO",
                            "Released expired reservation for product " + r.getProductId()
                    ))
            .subscribe();
    }
}