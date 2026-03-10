package com.blind.orderflow.inventory.consumer;

import com.blind.orderflow.config.KafkaConfig;
import com.blind.orderflow.inventory.service.InventoryService;
import com.blind.orderflow.shared.events.BaseEvent;
import com.blind.orderflow.shared.events.PaymentCompletedPayload;
import com.blind.orderflow.shared.utils.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryPaymentCompletedConsumer {

    private final InventoryService inventoryService;
    private final ObjectMapper mapper;

    @KafkaListener(topics = KafkaConfig.PAYMENT_COMPLETED_TOPIC)
    public void handlePaymentCompleted(BaseEvent<?> event) {

        PaymentCompletedPayload payload =
                mapper.convertValue(event.getPayload(), PaymentCompletedPayload.class);

        Logger.info(
                payload.getOrderId(),
                "INVENTORY",
                "PAYMENT_COMPLETED_RECEIVED",
                "INFO",
                "Received payment completed event, confirming inventory reservation"
        );

        inventoryService.confirmReservation(payload.getOrderId())
                .subscribe();
    }
}