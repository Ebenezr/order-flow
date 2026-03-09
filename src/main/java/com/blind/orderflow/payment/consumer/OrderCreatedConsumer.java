package com.blind.orderflow.payment.consumer;

import com.blind.orderflow.config.KafkaConfig;
import com.blind.orderflow.payment.service.PaymentService;
import com.blind.orderflow.shared.events.BaseEvent;
import com.blind.orderflow.shared.events.OrderCreatedPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = KafkaConfig.ORDER_CREATED_TOPIC)
    public void handleOrderCreated(BaseEvent<?> event) {

        ObjectMapper mapper = new ObjectMapper();

        OrderCreatedPayload payload =
                mapper.convertValue(event.getPayload(), OrderCreatedPayload.class);

        paymentService.processPayment(payload).subscribe();
    }
}