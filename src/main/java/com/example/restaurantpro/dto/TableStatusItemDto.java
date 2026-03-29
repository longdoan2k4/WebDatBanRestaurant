package com.example.restaurantpro.dto;

import java.time.LocalDateTime;

public class TableStatusItemDto {

    private final Long tableId;
    private final String tableNumber;
    private final String tableName;
    private final String floor;
    private final String roomType;
    private final Integer capacity;
    private final String tableType;
    private final LocalDateTime availableUntil;

    public TableStatusItemDto(Long tableId,
                              String tableNumber,
                              String tableName,
                              String floor,
                              String roomType,
                              Integer capacity,
                              String tableType,
                              LocalDateTime availableUntil) {
        this.tableId = tableId;
        this.tableNumber = tableNumber;
        this.tableName = tableName;
        this.floor = floor;
        this.roomType = roomType;
        this.capacity = capacity;
        this.tableType = tableType;
        this.availableUntil = availableUntil;
    }

    public Long getTableId() {
        return tableId;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public String getTableName() {
        return tableName;
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

    public String getTableType() {
        return tableType;
    }

    public LocalDateTime getAvailableUntil() {
        return availableUntil;
    }
}
