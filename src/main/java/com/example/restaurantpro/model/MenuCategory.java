package com.example.restaurantpro.model;

public enum MenuCategory {
    MAIN("Món chính"),
    DESSERT("Tráng miệng"),
    SALAD("Salad"),
    ASIAN("Món Á"),
    EUROPEAN("Món Âu"),
    HOTPOT("Lẩu");

    private final String displayName;

    MenuCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
