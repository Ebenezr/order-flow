package com.blind.orderflow.inventory.service;

import com.blind.orderflow.config.KafkaConfig;
import com.blind.orderflow.inventory.entity.InventoryReservation;
import com.blind.orderflow.inventory.repository.InventoryRepository;
import com.blind.orderflow.inventory.repository.InventoryReservationRepository;
import com.blind.orderflow.order.repository.OrderItemRepository;
import com.blind.orderflow.shared.events.BaseEvent;
import com.blind.orderflow.shared.events.InventoryFailedPayload;
import com.blind.orderflow.shared.events.InventoryReservedPayload;
import com.blind.orderflow.shared.kafka.KafkaProducerService;
import com.blind.orderflow.shared.utils.logging.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final KafkaProducerService kafkaProducerService;
    private final OrderItemRepository orderItemRepository;

    public Mono<Void> reserveStock(String orderId) {

        Logger.info(
                orderId,
                "INVENTORY",
                "ENTRY_RESERVE_STOCK",
                "START",
                "Reserving stock for order"
        );

        return orderItemRepository.findByOrderId(orderId)
                .flatMap(item ->
                        inventoryRepository.reserveStock(
                                        item.getProductId(),
                                        item.getQuantity()
                                )
                                .flatMap(rows -> {
                                    if (rows == 0) {
                                        return publishInventoryFailed(orderId, item.getProductId());
                                    }
                                    InventoryReservation reservation =
                                            InventoryReservation.builder()
                                                    .reservationId(UUID.randomUUID().toString())
                                                    .orderId(orderId)
                                                    .productId(item.getProductId())
                                                    .quantity(item.getQuantity())
                                                    .status("RESERVED")
                                                    .createdAt(LocalDateTime.now())
                                                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                                                    .build();
                                    return reservationRepository.save(reservation);
                                })
                )
                .then(Mono.defer(() -> {

                    BaseEvent<InventoryReservedPayload> event =
                            BaseEvent.<InventoryReservedPayload>builder()
                                    .eventId(UUID.randomUUID())
                                    .eventType("InventoryReserved")
                                    .version(1)
                                    .occurredAt(Instant.now())
                                    .payload(
                                            InventoryReservedPayload.builder()
                                                    .orderId(orderId)
                                                    .build()
                                    )
                                    .build();

                    return kafkaProducerService.send(
                            KafkaConfig.INVENTORY_RESERVED_TOPIC,
                            orderId,
                            event
                    );
                }));
    }

    public Mono<Void> confirmReservation(String orderId) {

        Logger.info(
                orderId,
                "INVENTORY",
                "CONFIRM_RESERVATION",
                "START",
                "Confirming inventory reservation for order"
        );

        // convert RESERVED → CONFIRMED
        return reservationRepository.findByOrderId(orderId)

                .flatMap(res -> {

                    res.setStatus("CONFIRMED");

                    return reservationRepository.save(res);
                })
                .then();
    }

    public Mono<Void> releaseReservation(String orderId) {
        Logger.info(
                orderId,
                "INVENTORY",
                "RELEASE_RESERVATION",
                "START",
                "Releasing inventory reservation for order"
        );

        return reservationRepository.findByOrderId(orderId)

                .flatMap(res ->

                        inventoryRepository.findByProductId(res.getProductId())

                                .flatMap(inv -> {

                                    inv.setAvailableQuantity(
                                            inv.getAvailableQuantity() + res.getQuantity()
                                    );

                                    return inventoryRepository.save(inv);
                                })

                                .then(reservationRepository.delete(res))
                )
                .then();
    }

    private Mono<Void> publishInventoryFailed(String orderId, String productId) {

        Logger.info(
                orderId,
                "INVENTORY",
                "PUBLISH_INVENTORY_FAILED",
                "START",
                "Publishing inventory failed event for order"
        );

        BaseEvent<InventoryFailedPayload> event =
                BaseEvent.<InventoryFailedPayload>builder()
                        .eventId(UUID.randomUUID())
                        .eventType("InventoryFailed")
                        .version(1)
                        .occurredAt(Instant.now())
                        .payload(
                                InventoryFailedPayload.builder()
                                        .orderId(orderId)
                                        .reason("Out of stock for " + productId)
                                        .build()
                        )
                        .build();

        return kafkaProducerService.send(
                KafkaConfig.INVENTORY_FAILED_TOPIC,
                orderId,
                event
        );
    }

}
