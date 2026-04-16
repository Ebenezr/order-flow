package com.blind.orderflow.shared.idempotency;

import com.blind.orderflow.shared.utils.logging.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedEventRepository processedEventRepository;
//

    public Mono<Void> executeOnceVoid(
            String eventId,
            String correlationId,
            String consumerName,
            Supplier<Mono<Void>> action
    ) {

        return processedEventRepository.save(
                        ProcessedEvent.builder()
                                .eventId(eventId)
                                .processedAt(LocalDateTime.now())
                                .consumerName(consumerName)
                                .build()
                )
                .then(action.get())
                .doOnSuccess(v ->
                        Logger.info(
                                correlationId,
                                consumerName,
                                "EVENT_PROCESSED",
                                "SUCCESS",
                                "Processed event " + eventId
                        )
                )
                .onErrorResume(e -> {

                    if (isDuplicateKey(e)) {
                        Logger.info(
                                correlationId,
                                consumerName,
                                "EVENT_ALREADY_PROCESSED",
                                "INFO",
                                "Skipping duplicate event " + eventId
                        );
                        return Mono.empty();
                    }

                    Logger.error(
                            correlationId,
                            consumerName,
                            "PIPELINE_ERROR",
                            "ERROR",
                            e.getMessage()
                    );

                    return Mono.error(e);
                });
    }

    private boolean isDuplicateKey(Throwable e) {
        return e.getMessage() != null &&
                e.getMessage().contains("Duplicate");
    }

}