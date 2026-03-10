package com.blind.orderflow.inventory.consumer;


import com.blind.orderflow.config.KafkaConfig;
import com.blind.orderflow.inventory.service.InventoryService;
import com.blind.orderflow.shared.events.BaseEvent;
import com.blind.orderflow.shared.events.PaymentFailedPayload;
import com.blind.orderflow.shared.utils.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryPaymentFailedConsumer {

    private final InventoryService inventoryService;
    private final ObjectMapper mapper;

    @KafkaListener(topics = KafkaConfig.PAYMENT_FAILED_TOPIC)
    public void handlePaymentFailed(BaseEvent<?> event) {

        PaymentFailedPayload payload =
                mapper.convertValue(event.getPayload(), PaymentFailedPayload.class);

        Logger.info(
                payload.getOrderId(),
                "INVENTORY",
                "PAYMENT_FAILED_RECEIVED",
                "INFO",
                "Received payment failed event, releasing inventory reservation"
        );

        inventoryService.releaseReservation(payload.getOrderId())
                .subscribe();
    }
}