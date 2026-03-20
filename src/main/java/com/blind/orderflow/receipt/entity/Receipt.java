package com.blind.orderflow.receipt.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("receipts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Receipt {

    @Id
    private Long id;

    private String receiptNumber;

    private String orderId;

    private String transactionId;

    private BigDecimal subtotal;

    private BigDecimal vat;

    private BigDecimal serviceCharge;

    private BigDecimal discount;

    private BigDecimal total;

    private String vatNumber;

    private String businessName;

    private LocalDateTime createdAt;
}