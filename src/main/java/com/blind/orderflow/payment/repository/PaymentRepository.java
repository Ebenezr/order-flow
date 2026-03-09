package com.blind.orderflow.payment.repository;

import com.blind.orderflow.payment.entity.PaymentTransaction;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PaymentRepository extends ReactiveCrudRepository<PaymentTransaction, Long> {

    Mono<PaymentTransaction> findByOrderId(String orderId);

}