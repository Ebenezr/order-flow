package com.blind.orderflow.kitchen.consumer;


import com.blind.orderflow.config.KafkaConfig;
import com.blind.orderflow.kitchen.service.KitchenService;
import com.blind.orderflow.shared.events.BaseEvent;
import com.blind.orderflow.shared.events.PaymentCompletedPayload;
import com.blind.orderflow.shared.utils.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KitchenPaymentCompleteConsumer {

    private final KitchenService kitchenService;
    private final ObjectMapper mapper;

    @KafkaListener(topics = KafkaConfig.PAYMENT_COMPLETED_TOPIC,groupId = "kitchen-group")
    public void handlePaymentCompleted(BaseEvent<?> event) {

        PaymentCompletedPayload payload =
                mapper.convertValue(event.getPayload(), PaymentCompletedPayload.class);

        kitchenService.createKitchenOrder(payload.getOrderId())
                .doOnSuccess(order ->
                        Logger.info(
                                payload.getOrderId(),
                                "KITCHEN",
                                "PAYMENT_COMPLETED_CONSUMER",
                                "SUCCESS",
                                "Created kitchen order for payment completed event"
                        ))
                .doOnError(e -> Logger.error(
                        payload.getOrderId(),
                        "KITCHEN",
                        "PAYMENT_COMPLETED_CONSUMER",
                        "ERROR",
                        "Failed to create kitchen order for payment completed event: " + e.getMessage()
                ))
                .subscribe();
    }
}
