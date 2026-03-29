package com.example.restaurantpro.model;

public enum BookingStatus {
    PENDING("Đang chờ"),
    CONFIRMED("Đã xác nhận"),
    CANCELLED("Đã hủy"),
    NO_SHOW("Không đến");

    private final String displayName;

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
