package com.example.restaurantpro.dto;

public class KitchenOrderDto {

    private final String tableNumber;
    private final String floor;
    private final String roomType;
    private final String itemName;
    private final Integer quantity;
    private final String note;

    public KitchenOrderDto(String tableNumber,
                           String floor,
                           String roomType,
                           String itemName,
                           Integer quantity,
                           String note) {
        this.tableNumber = tableNumber;
        this.floor = floor;
        this.roomType = roomType;
        this.itemName = itemName;
        this.quantity = quantity;
        this.note = note;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public String getFloor() {
        return floor;
    }

    public String getRoomType() {
        return roomType;
    }

    public String getItemName() {
        return itemName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getNote() {
        return note;
    }
}
