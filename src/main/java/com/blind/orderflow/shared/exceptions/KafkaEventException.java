package com.blind.orderflow.shared.exceptions;

public class KafkaEventException extends BusinessException {

    public KafkaEventException(String eventType) {
        super("Failed to publish event: " + eventType, "EVENT_PUBLISH_FAILED");
    }
}