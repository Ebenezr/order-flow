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


        String orderId = payload.getOrderId();
        paymentService.processPayment(orderId)
                .doOnError(e -> Logger.error(
                        orderId,
                        "PAYMENT",
                        "ERROR_PROCESS_PAYMENT",
                        "ERROR",
                        e.getMessage()
                ))
                .doOnSuccess(v -> Logger.info(
                        orderId,
                        "PAYMENT",
                        "PROCESS_PAYMENT_COMPLETED",
                        "INFO",
                        "Payment flow finished (check next events for result)"
                ))
                .subscribe();
    }

}