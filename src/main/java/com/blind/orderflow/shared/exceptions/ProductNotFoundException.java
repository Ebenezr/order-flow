package com.blind.orderflow.shared.exceptions;

public class ProductNotFoundException extends NotFoundException {

    public ProductNotFoundException(String productId) {
        super("Product not found: " + productId);
    }
}