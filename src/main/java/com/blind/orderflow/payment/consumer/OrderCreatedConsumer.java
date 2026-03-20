package com.blind.orderflow.payment.consumer;

import com.blind.orderflow.config.KafkaConfig;
import com.blind.orderflow.inventory.service.InventoryService;
import com.blind.orderflow.payment.service.PaymentService;
import com.blind.orderflow.shared.events.BaseEvent;
import com.blind.orderflow.shared.events.InventoryReservedPayload;
import com.blind.orderflow.shared.events.OrderCreatedPayload;
import com.blind.orderflow.shared.utils.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final PaymentService paymentService;
    private final InventoryService inventoryService;

    private final ObjectMapper mapper;

    @KafkaListener(topics = KafkaConfig.ORDER_CREATED_TOPIC,groupId = "payment-group")
    public void handleOrderCreated(BaseEvent<?> event) {

        OrderCreatedPayload payload =
                mapper.convertValue(event.getPayload(), OrderCreatedPayload.class);

        inventoryService.reserveStock(payload.getOrderId())
                .doOnSuccess(
                        v -> Logger.info(
                                payload.getOrderId(),
                                "PAYMENT",
                                "SUCCESS_STOCK_RESERVED",
                                "INFO",
                                "Stock reserved successfully, proceeding to payment"
                        )
                ).doOnError(
                        e -> Logger.error(
                                payload.getOrderId(),
                                "PAYMENT",
                                "ERROR_STOCK_RESERVATION_FAILED",
                                "ERROR",
                                "Stock reservation failed: " + e.getMessage()
                        )
                )
                .subscribe();
    }

    @KafkaListener(topics = KafkaConfig.INVENTORY_RESERVED_TOPIC,groupId = "inventory-group")
    public void handleInventoryReserved(BaseEvent<?> event) {

        InventoryReservedPayload payload =
                mapper.convertValue(event.getPayload(), InventoryReservedPayload.class);


        paymentService.processPayment(payload.getOrderId())
                .doOnError(e -> Logger.error(
                        payload.getOrderId(),
                        "PAYMENT",
                        "ERROR_PAYMENT_PROCESSING_FAILED",
                        "ERROR",
                        "Payment processing failed: " + e.getMessage()
                ))
                .doOnSuccess(e -> Logger.info(
                        payload.getOrderId(),
                        "PAYMENT",
                        "SUCCESS_PAYMENT_PROCESSED",
                        "INFO",
                        "Payment processed successfully"
                ))
                .subscribe();
    }

}