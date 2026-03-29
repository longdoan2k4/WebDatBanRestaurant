package com.example.restaurantpro.model;

public enum PaymentMethod {
    PAY_AT_RESTAURANT("Thanh toan tai nha hang"),
    VNPAY("Thanh toan qua VNPAY");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
