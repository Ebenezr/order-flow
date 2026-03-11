package com.blind.orderflow.order.entity;


import com.blind.orderflow.shared.utils.enums.OrderStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

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
    private Double subtotal;

    @Column("vat")
    private Double vat;

    @Column("service_charge")
    private Double serviceCharge;

    @Column("discount")
    private Double discount;

    @Column("total_amount")
    private Double totalAmount;

    @Column("cancellation_reason")
    private String cancellationReason;
}


