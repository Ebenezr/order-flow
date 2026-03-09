package com.blind.orderflow.receipt.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReceiptLine {

    private String itemName;
    private int quantity;
    private double price;

}