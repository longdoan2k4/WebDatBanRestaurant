package com.example.restaurantpro.model;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true)
    private String phone;

    @Column(unique = true)
    private String email;

    @Column(name = "google_id", unique = true)
    private String googleId;

    private String provider;

    private String password;

    private boolean enabled = true;

    private boolean locked = false;

    private boolean isVip = false;

    private int loyaltyPoints = 0;

    private LocalDateTime createdAt = LocalDateTime.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role_name")
    private Set<RoleName> roles = new LinkedHashSet<>();

    public AppUser() {
    }

    public AppUser(String fullName, String phone, String password, Set<RoleName> roles) {
        this.fullName = fullName;
        this.phone = phone;
        this.password = password;
        this.roles = roles;
    }

    public boolean hasRole(RoleName roleName) {
        return roles != null && roles.contains(roleName);
    }

    public String getRoleSummary() {
        return roles.stream()
                .map(RoleName::getDisplayName)
                .collect(Collectors.joining(", "));
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public String getGoogleId() {
        return googleId;
    }

    public String getProvider() {
        return provider;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isLocked() {
        return locked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Set<RoleName> getRoles() {
        return roles;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isVip() {
        return isVip;
    }

    public void setVip(boolean vip) {
        isVip = vip;
    }

    public int getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void setLoyaltyPoints(int loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setRoles(Set<RoleName> roles) {
        this.roles = roles;
    }

    public boolean matchesLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            return false;
        }
        return loginId.equals(phone) || loginId.equalsIgnoreCase(email);
    }
}
