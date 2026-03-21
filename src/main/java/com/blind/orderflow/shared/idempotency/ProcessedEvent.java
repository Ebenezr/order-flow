package com.blind.orderflow.shared.idempotency;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table("processed_events")
public class ProcessedEvent {

    @Id
    private String eventId;

    private LocalDateTime processedAt;
}