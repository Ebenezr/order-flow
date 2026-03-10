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

    @KafkaListener(topics = KafkaConfig.ORDER_CREATED_TOPIC)
    public void handleOrderCreated(BaseEvent<?> event) {

        OrderCreatedPayload payload =
                mapper.convertValue(event.getPayload(), OrderCreatedPayload.class);

        Logger.info(
                payload.getOrderId(),
                "PAYMENT",
                "ORDER_CREATED_RECEIVED",
                "INFO",
                "Received order created event, reserving stock"
        );

        inventoryService.reserveStock(payload.getOrderId())
                .subscribe();
    }

    @KafkaListener(topics = KafkaConfig.INVENTORY_RESERVED_TOPIC)
    public void handleInventoryReserved(BaseEvent<?> event) {

        InventoryReservedPayload payload =
                mapper.convertValue(event.getPayload(), InventoryReservedPayload.class);

        Logger.info(
                payload.getOrderId(),
                "PAYMENT",
                "INVENTORY_RESERVED_RECEIVED",
                "INFO",
                "Received inventory reserved event, processing payment"
        );

        paymentService.processPayment(payload.getOrderId())
                .subscribe();
    }

}