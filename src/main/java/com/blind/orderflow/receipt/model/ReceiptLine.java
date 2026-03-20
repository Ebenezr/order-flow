package com.blind.orderflow.receipt.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ReceiptLine {

    private String itemName;
    private int quantity;
    private BigDecimal price;

}