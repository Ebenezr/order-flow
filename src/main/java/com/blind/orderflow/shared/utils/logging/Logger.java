package com.blind.orderflow.shared.utils.logging;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
public final class Logger {

    private Logger() {}

    private static final String FORMAT =
            "CorrelationId={} | Module={} | Process={} | Duration={} | Message={}";

    public static void info(String correlationId,
                            String module,
                            String process,
                            String duration,
                            String message) {

        log.info(FORMAT, correlationId, module, process, duration, message);
    }

    public static void warn(String correlationId,
                            String module,
                            String process,
                            String duration,
                            String message) {

        log.warn(FORMAT, correlationId, module, process, duration, message);
    }

    public static void error(String correlationId,
                             String module,
                             String process,
                             String duration,
                             String message) {

        log.error(FORMAT, correlationId, module, process, duration, message);
    }

    public static String processDuration(LocalDateTime startTime) {

        Duration duration = Duration.between(startTime, LocalDateTime.now());

        long millis = duration.toMillis();

        if (millis < 1000) {
            return millis + "ms";
        }

        long seconds = millis / 1000;
        long remainingMillis = millis % 1000;

        return seconds + "s " + remainingMillis + "ms";
    }
}