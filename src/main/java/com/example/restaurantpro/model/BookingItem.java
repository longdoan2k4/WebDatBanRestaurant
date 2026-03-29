package com.example.restaurantpro.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Entity
@Table(name = "booking_items")
public class BookingItem {

    private static final Locale VI_LOCALE = Locale.forLanguageTag("vi-VN");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Booking booking;

    @ManyToOne(fetch = FetchType.EAGER)
    private MenuItem menuItem;

    private Integer quantity;

    private BigDecimal priceAtOrder;

    public BookingItem() {
    }

    public BookingItem(MenuItem menuItem, Integer quantity, BigDecimal priceAtOrder) {
        this.menuItem = menuItem;
        this.quantity = quantity;
        this.priceAtOrder = priceAtOrder;
    }

    public BigDecimal getLineTotal() {
        return priceAtOrder.multiply(BigDecimal.valueOf(quantity));
    }

    public String getLineTotalDisplay() {
        return NumberFormat.getCurrencyInstance(VI_LOCALE).format(getLineTotal());
    }

    public Long getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public MenuItem getMenuItem() {
        return menuItem;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getPriceAtOrder() {
        return priceAtOrder;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public void setMenuItem(MenuItem menuItem) {
        this.menuItem = menuItem;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setPriceAtOrder(BigDecimal priceAtOrder) {
        this.priceAtOrder = priceAtOrder;
    }
}
