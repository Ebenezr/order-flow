package com.blind.orderflow.receipt.repository;

import com.blind.orderflow.receipt.entity.Receipt;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ReceiptRepository extends ReactiveCrudRepository<Receipt, Long> {

    Mono<Receipt> findByOrderId(String orderId);

    Mono<Receipt> findByReceiptNumber(String receiptNumber);
}

