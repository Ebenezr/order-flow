package com.blind.orderflow.shared.idempotency;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface ProcessedEventRepository extends ReactiveCrudRepository<ProcessedEvent, String> {


}