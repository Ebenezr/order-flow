package com.blind.orderflow.shared.exceptions;

public class InventoryUnavailableException extends BusinessException {

    public InventoryUnavailableException(String ingredient) {
        super("Insufficient inventory for ingredient: " + ingredient,
                "INVENTORY_UNAVAILABLE");
    }
}