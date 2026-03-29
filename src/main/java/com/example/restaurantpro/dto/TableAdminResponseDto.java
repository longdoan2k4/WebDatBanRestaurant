package com.example.restaurantpro.dto;

public class TableAdminResponseDto {

    private final Long id;
    private final String name;
    private final String tableNumber;
    private final String tableType;
    private final String floor;
    private final String roomType;
    private final String areaPosition;
    private final String locationDisplay;
    private final Integer capacity;
    private final boolean availableNow;
    private final boolean active;

    public TableAdminResponseDto(Long id,
                                 String name,
                                 String tableNumber,
                                 String tableType,
                                 String floor,
                                 String roomType,
                                 String areaPosition,
                                 String locationDisplay,
                                 Integer capacity,
                                 boolean availableNow,
                                 boolean active) {
        this.id = id;
        this.name = name;
        this.tableNumber = tableNumber;
        this.tableType = tableType;
        this.floor = floor;
        this.roomType = roomType;
        this.areaPosition = areaPosition;
        this.locationDisplay = locationDisplay;
        this.capacity = capacity;
        this.availableNow = availableNow;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public String getTableType() {
        return tableType;
    }

    public String getFloor() {
        return floor;
    }

    public String getRoomType() {
        return roomType;
    }

    public String getAreaPosition() {
        return areaPosition;
    }

    public String getLocationDisplay() {
        return locationDisplay;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public boolean isAvailableNow() {
        return availableNow;
    }

    public boolean isActive() {
        return active;
    }
}
