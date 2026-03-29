package com.example.restaurantpro.dto;

import java.math.BigDecimal;

public class DailyRevenueDto {
    private Integer day;
    private BigDecimal revenue;

    public DailyRevenueDto(Integer day, BigDecimal revenue) {
        this.day = day;
        this.revenue = revenue;
    }

    public Integer getDay() {
        return day;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }
}