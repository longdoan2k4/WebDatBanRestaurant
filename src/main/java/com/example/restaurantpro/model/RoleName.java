package com.example.restaurantpro.model;

public enum RoleName {
    ROLE_CUSTOMER("Khách hàng"),
    ROLE_TABLE_MANAGER("Quản lý bàn"),
    ROLE_MENU_MANAGER("Quản lý menu"),
    ROLE_ADMIN("Admin");

    private final String displayName;

    RoleName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
