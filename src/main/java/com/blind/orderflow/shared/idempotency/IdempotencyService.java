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

    public Mono<Void> executeOnceVoid(
            String eventId,
            String correlationId,
            String module,
            Supplier<Mono<Void>> action
    ) {

        return processedEventRepository.existsById(eventId)
                .flatMap(exists -> {

                    if (exists) {
                        Logger.info(
                                correlationId,
                                module,
                                "EVENT_ALREADY_PROCESSED",
                                "INFO",
                                "Skipping duplicate event " + eventId
                        );
                        return Mono.empty();
                    }

                    Logger.info(
                            correlationId,
                            module,
                            "EVENT_RECORDING",
                            "INFO",
                            "Recording event " + eventId
                    );

                    return action.get()
                            .then(
                                    processedEventRepository.save(
                                            ProcessedEvent.builder()
                                                    .eventId(eventId)
                                                    .processedAt(LocalDateTime.now())
                                                    .build()
                                    )
                            )
                            .doOnSuccess(v ->
                                    Logger.info(
                                            correlationId,
                                            module,
                                            "EVENT_RECORDED_SUCCESS",
                                            "SUCCESS",
                                            "Saved event " + eventId
                                    )
                            ).then();
                });
    }
}