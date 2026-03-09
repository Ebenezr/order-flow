package com.blind.orderflow.shared.utils.logging;

import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

public class ReactiveLogger {

    public static String getCorrelationId(ContextView context) {
        return context.getOrDefault("correlationId", "N/A");
    }

    public static <T> Mono<T> withLogging(Mono<T> mono, String module, String process) {

        return Mono.deferContextual(context -> {

            String correlationId = getCorrelationId(context);

            Logger.info(
                    correlationId,
                    module,
                    process,
                    "START",
                    "Process started"
            );

            return mono
                    .doOnSuccess(result ->
                            Logger.info(
                                    correlationId,
                                    module,
                                    process,
                                    "END",
                                    "Process completed"
                            )
                    )
                    .doOnError(error ->
                            Logger.error(
                                    correlationId,
                                    module,
                                    process,
                                    "ERROR",
                                    error.getMessage()
                            )
                    );
        });
    }
}