package com.blind.orderflow.shared.events;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseEvent<T> {

    private UUID eventId;

    private String correlationId;

    private String eventType;

    private int version;

    private Instant occurredAt;

    private T payload;
}