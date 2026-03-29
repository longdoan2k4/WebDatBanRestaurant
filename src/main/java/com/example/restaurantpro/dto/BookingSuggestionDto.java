package com.example.restaurantpro.dto;

import java.time.LocalDateTime;
import java.util.List;

public class BookingSuggestionDto {

    private final List<AlternativeTableSuggestion> alternativeTables;
    private final List<AvailableTimeSlotSuggestion> availableTimeSlots;

    public BookingSuggestionDto(List<AlternativeTableSuggestion> alternativeTables,
                                List<AvailableTimeSlotSuggestion> availableTimeSlots) {
        this.alternativeTables = alternativeTables;
        this.availableTimeSlots = availableTimeSlots;
    }

    public List<AlternativeTableSuggestion> getAlternativeTables() {
        return alternativeTables;
    }

    public List<AvailableTimeSlotSuggestion> getAvailableTimeSlots() {
        return availableTimeSlots;
    }

    public static class AlternativeTableSuggestion {
        private final Long tableId;
        private final String tableName;
        private final Integer capacity;
        private final String location;

        public AlternativeTableSuggestion(Long tableId, String tableName, Integer capacity, String location) {
            this.tableId = tableId;
            this.tableName = tableName;
            this.capacity = capacity;
            this.location = location;
        }

        public Long getTableId() {
            return tableId;
        }

        public String getTableName() {
            return tableName;
        }

        public Integer getCapacity() {
            return capacity;
        }

        public String getLocation() {
            return location;
        }
    }

    public static class AvailableTimeSlotSuggestion {
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;

        public AvailableTimeSlotSuggestion(LocalDateTime startTime, LocalDateTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }
    }
}
