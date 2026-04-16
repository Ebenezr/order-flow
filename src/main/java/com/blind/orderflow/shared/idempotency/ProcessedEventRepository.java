package com.blind.orderflow.shared.idempotency;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ProcessedEventRepository
        extends ReactiveCrudRepository<ProcessedEvent, Long> {

    Mono<Boolean> existsByEventId(String eventId);
    Mono<Boolean> existsByEventIdAndConsumerName(String eventId, String consumerName);
}