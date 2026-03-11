package com.blind.orderflow.payment.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("payment_transactions")
public class PaymentTransaction {

    @Id
    private Long id;

    private String transactionId;

    private String orderId;

    private Double amount;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}