package com.example.restaurantpro.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dining_tables")
public class DiningTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, length = 50)
    private String tableNumber;

    private String tableType;

    private String chairType;

    private Integer capacity;

    @Column(length = 800)
    private String description;

    private String floor;

    private String roomType;

    private String areaPosition;

    private String location;

    private boolean active = true;

    public DiningTable() {
    }

    public DiningTable(String name, String tableType, String chairType, Integer capacity, String description, String location, boolean active) {
        this.name = name;
        this.tableNumber = name;
        this.tableType = tableType;
        this.chairType = chairType;
        this.capacity = capacity;
        this.description = description;
        this.location = location;
        this.active = active;
    }

    public DiningTable(String name,
                      String tableType,
                      String chairType,
                      Integer capacity,
                      String description,
                      String floor,
                      String roomType,
                      String areaPosition,
                      boolean active) {
        this.name = name;
        this.tableNumber = name;
        this.tableType = tableType;
        this.chairType = chairType;
        this.capacity = capacity;
        this.description = description;
        this.floor = floor;
        this.roomType = roomType;
        this.areaPosition = areaPosition;
        this.active = active;
    }

    public String getSummaryLine() {
        return tableType + " - " + chairType + " - Sức chứa " + capacity + " người";
    }

    public String getLocationDisplay() {
        String value = (floor == null ? "" : floor.trim())
                + (roomType == null || roomType.isBlank() ? "" : " / " + roomType.trim())
                + (areaPosition == null || areaPosition.isBlank() ? "" : " / " + areaPosition.trim());
        value = value.strip();
        if (value.startsWith("/")) {
            value = value.substring(1).trim();
        }
        if (value.isBlank()) {
            return location;
        }
        return value;
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

    public String getChairType() {
        return chairType;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public String getDescription() {
        return description;
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

    public String getLocation() {
        String display = getLocationDisplay();
        return display == null || display.isBlank() ? location : display;
    }

    public boolean isActive() {
        return active;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public void setTableType(String tableType) {
        this.tableType = tableType;
    }

    public void setChairType(String chairType) {
        this.chairType = chairType;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public void setAreaPosition(String areaPosition) {
        this.areaPosition = areaPosition;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
