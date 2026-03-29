package com.example.restaurantpro.model;

public enum PaymentTransactionType {
    PAYMENT("Thanh toán"),
    REFUND("Hoàn tiền");

    private final String displayName;

    PaymentTransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
