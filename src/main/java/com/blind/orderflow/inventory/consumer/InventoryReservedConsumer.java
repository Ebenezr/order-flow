package com.blind.orderflow.inventory.consumer;

import com.blind.orderflow.config.KafkaConfig;
import com.blind.orderflow.payment.service.PaymentService;
import com.blind.orderflow.shared.events.BaseEvent;
import com.blind.orderflow.shared.events.InventoryReservedPayload;
import com.blind.orderflow.shared.utils.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryReservedConsumer {

    private final PaymentService paymentService;
    private final ObjectMapper mapper;


    @KafkaListener(topics = KafkaConfig.INVENTORY_RESERVED_TOPIC)
    public void handleInventoryReserved(BaseEvent<?> event) {

        InventoryReservedPayload payload =
                mapper.convertValue(event.getPayload(), InventoryReservedPayload.class);


        paymentService.processPayment(payload.getOrderId())
                .doOnError(e -> Logger.error(
                        payload.getOrderId(),
                        "PAYMENT",
                        "EVENT_ERROR_PAYMENT_PROCESSING_FAILED",
                        "ERROR",
                        "Payment processing failed: " + e.getMessage()
                ))
                .doOnSuccess(e -> Logger.info(
                        payload.getOrderId(),
                        "PAYMENT",
                        "EVENT_SUCCESS_PAYMENT_PROCESSED",
                        "INFO",
                        "Payment processed successfully"
                ))
                .subscribe();
    }

}