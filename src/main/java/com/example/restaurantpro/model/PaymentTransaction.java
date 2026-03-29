package com.example.restaurantpro.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Booking booking;

    @Column(nullable = false, unique = true, length = 100)
    private String txnRef;

    @Column(nullable = false, length = 20)
    private String provider = "VNPAY";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentTransactionType type = PaymentTransactionType.PAYMENT;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(length = 20)
    private String responseCode;

    @Column(length = 20)
    private String transactionStatus;

    @Column(length = 50)
    private String transactionNo;

    @Column(length = 50)
    private String bankCode;

    @Column(length = 100)
    private String originalTxnRef;

    @Column(length = 120)
    private String createdBy;

    @Column(length = 1200)
    private String message;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime paidAt;

    private LocalDateTime refundedAt;

    public PaymentTransaction() {
    }

    public PaymentTransaction(Booking booking, String txnRef, BigDecimal amount) {
        this.booking = booking;
        this.txnRef = txnRef;
        this.amount = amount;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public String getTxnRef() {
        return txnRef;
    }

    public String getProvider() {
        return provider;
    }

    public PaymentTransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public String getTransactionStatus() {
        return transactionStatus;
    }

    public String getTransactionNo() {
        return transactionNo;
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getOriginalTxnRef() {
        return originalTxnRef;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public void setTxnRef(String txnRef) {
        this.txnRef = txnRef;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setType(PaymentTransactionType type) {
        this.type = type;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public void setTransactionStatus(String transactionStatus) {
        this.transactionStatus = transactionStatus;
    }

    public void setTransactionNo(String transactionNo) {
        this.transactionNo = transactionNo;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public void setOriginalTxnRef(String originalTxnRef) {
        this.originalTxnRef = originalTxnRef;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }
}
