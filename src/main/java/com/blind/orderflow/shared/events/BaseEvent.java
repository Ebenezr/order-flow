package com.blind.orderflow.shared.events;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class BaseEvent<T> {

    private UUID eventId;

    private String eventType;

    private int version;

    private Instant occurredAt;

    private T payload;
}