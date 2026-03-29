package com.example.restaurantpro.dto;

public class TableGroupDto {

    private final String floor;
    private final String roomType;
    private final Integer capacity;
    private final int totalQuantity;
    private final int availableQuantity;

    public TableGroupDto(String floor,
                         String roomType,
                         Integer capacity,
                         int totalQuantity,
                         int availableQuantity) {
        this.floor = floor;
        this.roomType = roomType;
        this.capacity = capacity;
        this.totalQuantity = totalQuantity;
        this.availableQuantity = availableQuantity;
    }

    public String getFloor() {
        return floor;
    }

    public String getRoomType() {
        return roomType;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
