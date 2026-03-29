package com.example.restaurantpro.dto;

import java.time.LocalDateTime;
import java.util.List;

public class TableStatusResponseDto {

    private final LocalDateTime checkTime;
    private final int totalTables;
    private final int freeCount;
    private final int inUseCount;
    private final int soonCount;
    private final List<TableStatusItemDto> freeTables;
    private final List<TableStatusItemDto> inUseTables;
    private final List<TableStatusItemDto> soonTables;

    public TableStatusResponseDto(LocalDateTime checkTime,
                                  int totalTables,
                                  int freeCount,
                                  int inUseCount,
                                  int soonCount,
                                  List<TableStatusItemDto> freeTables,
                                  List<TableStatusItemDto> inUseTables,
                                  List<TableStatusItemDto> soonTables) {
        this.checkTime = checkTime;
        this.totalTables = totalTables;
        this.freeCount = freeCount;
        this.inUseCount = inUseCount;
        this.soonCount = soonCount;
        this.freeTables = freeTables;
        this.inUseTables = inUseTables;
        this.soonTables = soonTables;
    }

    public LocalDateTime getCheckTime() {
        return checkTime;
    }

    public int getTotalTables() {
        return totalTables;
    }

    public int getFreeCount() {
        return freeCount;
    }

    public int getInUseCount() {
        return inUseCount;
    }

    public int getSoonCount() {
        return soonCount;
    }

    public List<TableStatusItemDto> getFreeTables() {
        return freeTables;
    }

    public List<TableStatusItemDto> getInUseTables() {
        return inUseTables;
    }

    public List<TableStatusItemDto> getSoonTables() {
        return soonTables;
    }
}
