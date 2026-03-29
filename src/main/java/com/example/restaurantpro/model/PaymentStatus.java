package com.example.restaurantpro.model;

public enum PaymentStatus {
    UNPAID("Chua thanh toan"),
    PENDING("Dang cho thanh toan"),
    PAID("Da thanh toan"),
    FAILED("Thanh toan that bai"),
    REFUND_PENDING("Dang cho hoan tien"),
    REFUNDED("Da hoan tien");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
