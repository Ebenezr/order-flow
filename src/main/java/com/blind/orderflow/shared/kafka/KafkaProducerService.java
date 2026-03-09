package com.blind.orderflow.shared.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static com.blind.orderflow.config.CorrelationIdFilter.CONTEXT_KEY;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Mono<Void> send(String topic, String key, Object event) {

        return Mono.deferContextual(ctx -> {

            String correlationId = ctx.getOrDefault(CONTEXT_KEY, "N/A");

            Message<Object> message =
                    MessageBuilder.withPayload(event)
                            .setHeader("kafka_topic", topic)
                            .setHeader("kafka_messageKey", key)
                            .setHeader("correlationId", correlationId)
                            .build();

            return Mono.fromFuture(kafkaTemplate.send(message))
                    .then();
        });
    }
}