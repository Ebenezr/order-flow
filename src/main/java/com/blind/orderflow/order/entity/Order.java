package com.blind.orderflow.order.entity;


import com.blind.orderflow.shared.utils.enums.OrderStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("orders")
public class Order {

    @Id
    private Long id;

    private String orderId;

    private String customerId;

    private OrderStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column("subtotal")
    private BigDecimal subtotal;

    @Column("vat")
    private BigDecimal vat;

    @Column("service_charge")
    private BigDecimal serviceCharge;

    @Column("discount")
    private BigDecimal discount;

    @Column("total_amount")
    private BigDecimal totalAmount;

    @Column("cancellation_reason")
    private String cancellationReason;
}


