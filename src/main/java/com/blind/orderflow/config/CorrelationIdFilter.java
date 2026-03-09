package com.blind.orderflow.config;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdFilter implements WebFilter {

    public static final String HEADER = "X-Correlation-ConversationID";
    public static final String CONTEXT_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String correlationId = exchange
                .getRequest()
                .getHeaders()
                .getFirst(HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        String finalCorrelationId = correlationId;

        // add to response header
        exchange.getResponse().getHeaders().add(HEADER, finalCorrelationId);

        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(CONTEXT_KEY, finalCorrelationId));
    }
}