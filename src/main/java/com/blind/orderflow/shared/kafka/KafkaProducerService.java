package com.blind.orderflow.shared.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Mono<Void> send(String topic, String key, Object event) {

        return Mono.fromCallable(() -> kafkaTemplate.send(topic, key, event))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}