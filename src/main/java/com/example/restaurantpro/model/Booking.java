package com.example.restaurantpro.model;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "bookings")
public class Booking {

    private static final Locale VI_LOCALE = Locale.forLanguageTag("vi-VN");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    private AppUser customer;

    @ManyToOne(fetch = FetchType.EAGER)
    private DiningTable diningTable;

    private LocalDateTime bookingDateTime;

    private LocalDateTime endTime;

    private Integer guestCount;

    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 80)
    private String appliedVoucherCode;

    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(length = 1200)
    private String notes;

    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod = PaymentMethod.PAY_AT_RESTAURANT;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(length = 100)
    private String latestPaymentTxnRef;

    private LocalDateTime paidAt;

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentTransaction> paymentTransactions = new ArrayList<>();

    public Booking() {
    }

    public boolean hasPreOrder() {
        return items != null && !items.isEmpty();
    }

    public boolean hasPayableAmount() {
        return totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean canPayOnline() {
        return hasPayableAmount()
                && paymentStatus != PaymentStatus.PAID
                && paymentStatus != PaymentStatus.REFUND_PENDING
                && paymentStatus != PaymentStatus.REFUNDED
                && status != BookingStatus.CANCELLED
                && status != BookingStatus.NO_SHOW;
    }

    public void addItem(BookingItem item) {
        item.setBooking(this);
        this.items.add(item);
    }

    public void addPaymentTransaction(PaymentTransaction paymentTransaction) {
        paymentTransaction.setBooking(this);
        this.paymentTransactions.add(paymentTransaction);
    }

    public String getTotalAmountDisplay() {
        return NumberFormat.getCurrencyInstance(VI_LOCALE).format(totalAmount);
    }

    public BigDecimal getFinalAmount() {
        BigDecimal base = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        BigDecimal discount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        BigDecimal finalAmount = base.subtract(discount);
        return finalAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : finalAmount;
    }

    public String getDiscountAmountDisplay() {
        BigDecimal discount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        return NumberFormat.getCurrencyInstance(VI_LOCALE).format(discount);
    }

    public String getFinalAmountDisplay() {
        return NumberFormat.getCurrencyInstance(VI_LOCALE).format(getFinalAmount());
    }

    public Long getId() {
        return id;
    }

    public AppUser getCustomer() {
        return customer;
    }

    public DiningTable getDiningTable() {
        return diningTable;
    }

    public LocalDateTime getBookingDateTime() {
        return bookingDateTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public Integer getGuestCount() {
        return guestCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getNotes() {
        return notes;
    }

    public String getAppliedVoucherCode() {
        return appliedVoucherCode;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public String getLatestPaymentTxnRef() {
        return latestPaymentTxnRef;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<BookingItem> getItems() {
        return items;
    }

    public List<PaymentTransaction> getPaymentTransactions() {
        return paymentTransactions;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCustomer(AppUser customer) {
        this.customer = customer;
    }

    public void setDiningTable(DiningTable diningTable) {
        this.diningTable = diningTable;
    }

    public void setBookingDateTime(LocalDateTime bookingDateTime) {
        this.bookingDateTime = bookingDateTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setGuestCount(Integer guestCount) {
        this.guestCount = guestCount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setAppliedVoucherCode(String appliedVoucherCode) {
        this.appliedVoucherCode = appliedVoucherCode;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public void setLatestPaymentTxnRef(String latestPaymentTxnRef) {
        this.latestPaymentTxnRef = latestPaymentTxnRef;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setItems(List<BookingItem> items) {
        this.items = items;
    }

    public void setPaymentTransactions(List<PaymentTransaction> paymentTransactions) {
        this.paymentTransactions = paymentTransactions;
    }
}
