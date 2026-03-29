package com.example.restaurantpro.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Entity
@Table(name = "menu_items")
public class MenuItem {

    private static final Locale VI_LOCALE = Locale.forLanguageTag("vi-VN");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private MenuCategory category;

    private String imageUrl;

    @Column(length = 1200)
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    private boolean available = true;

    public MenuItem() {
    }

    public MenuItem(String name, MenuCategory category, String imageUrl, String description, BigDecimal price, boolean available) {
        this.name = name;
        this.category = category;
        this.imageUrl = imageUrl;
        this.description = description;
        this.price = price;
        this.available = available;
    }

    public String getPriceDisplay() {
        return NumberFormat.getCurrencyInstance(VI_LOCALE).format(price);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public MenuCategory getCategory() {
        return category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCategory(MenuCategory category) {
        this.category = category;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
