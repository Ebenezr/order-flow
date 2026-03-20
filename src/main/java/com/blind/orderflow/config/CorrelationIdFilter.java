package com.blind.orderflow.config;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class CorrelationIdFilter implements WebFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String CONTEXT_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String correlationId = exchange
                .getRequest()
                .getHeaders()
                .getFirst(HEADER);

        if (correlationId == null || correlationId.isBlank()) {
//            return Mono.error(new IllegalArgumentException("Missing X-Correlation-Id header"));
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

            byte[] bytes = """
        {"error":"BAD_REQUEST","message":"Missing X-Correlation-Id header"}
        """.getBytes(StandardCharsets.UTF_8);

            DataBuffer buffer = exchange.getResponse()
                    .bufferFactory()
                    .wrap(bytes);

            return exchange.getResponse().writeWith(Mono.just(buffer));
        }

        String finalCorrelationId = correlationId;

        // add to response header
        exchange.getResponse().getHeaders().add(HEADER, finalCorrelationId);

        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(CONTEXT_KEY, finalCorrelationId));
    }
}