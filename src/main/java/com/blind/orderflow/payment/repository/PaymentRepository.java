package com.blind.orderflow.payment.repository;

import com.blind.orderflow.payment.entity.PaymentTransaction;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends ReactiveCrudRepository<PaymentTransaction, Long> {

    Mono<PaymentTransaction> findByOrderId(String orderId);

    @Query("SELECT COUNT(*) FROM payment_transactions WHERE status = 'SUCCESS' AND created_at BETWEEN :from AND :to")
    Mono<Long> countSuccessfulBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT COUNT(*) FROM payment_transactions WHERE status = 'FAILED' AND created_at BETWEEN :from AND :to")
    Mono<Long> countFailedBetween(LocalDateTime from, LocalDateTime to);

    Flux<PaymentTransaction> findByStatusInAndCreatedAtBetween(List<String> statuses, LocalDateTime from, LocalDateTime to);

}