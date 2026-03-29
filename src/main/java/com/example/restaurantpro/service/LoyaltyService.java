package com.example.restaurantpro.service;

import com.example.restaurantpro.model.AppUser;
import com.example.restaurantpro.model.Booking;
import com.example.restaurantpro.model.BookingStatus;
import com.example.restaurantpro.model.PaymentStatus;
import com.example.restaurantpro.model.Voucher;
import com.example.restaurantpro.repository.AppUserRepository;
import com.example.restaurantpro.repository.BookingRepository;
import com.example.restaurantpro.repository.VoucherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class LoyaltyService {

    private final AppUserRepository appUserRepository;
    private final VoucherRepository voucherRepository;
    private final BookingRepository bookingRepository;

    public LoyaltyService(AppUserRepository appUserRepository,
                          VoucherRepository voucherRepository,
                          BookingRepository bookingRepository) {
        this.appUserRepository = appUserRepository;
        this.voucherRepository = voucherRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public void addPointsForBooking(Booking booking) {
        if (booking == null || booking.getCustomer() == null || booking.getPaymentStatus() != PaymentStatus.PAID) {
            return;
        }

        BigDecimal totalAmount = booking.getTotalAmount();
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        int pointsToAdd = totalAmount.divide(BigDecimal.valueOf(15000), 0, RoundingMode.DOWN)
                .multiply(BigDecimal.TEN)
                .intValue();

        if (pointsToAdd > 0) {
            AppUser customer = appUserRepository.findById(booking.getCustomer().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Khong tim thay khach hang."));
            customer.setLoyaltyPoints(customer.getLoyaltyPoints() + pointsToAdd);
            appUserRepository.save(customer);
        }
    }

    @Transactional
    public Voucher redeemVoucher(String loginId) {
        AppUser customer;
        if (loginId.contains("@")) {
            customer = appUserRepository.findByEmailIgnoreCase(loginId)
                    .orElseThrow(() -> new IllegalArgumentException("Khong tim thay khach hang."));
        } else {
            customer = appUserRepository.findByPhone(loginId)
                    .orElseThrow(() -> new IllegalArgumentException("Khong tim thay khach hang."));
        }

        if (customer.getLoyaltyPoints() < 100) {
            throw new IllegalArgumentException("Bạn không đủ 100 điểm để đổi voucher.");
        }

        customer.setLoyaltyPoints(customer.getLoyaltyPoints() - 100);
        appUserRepository.save(customer);

        String code = "RIVIERE-50K-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Voucher voucher = new Voucher(code, 50000, customer);
        return voucherRepository.save(voucher);
    }

    @Transactional(readOnly = true)
    public java.util.List<Voucher> getAvailableVouchers(String loginId) {
        AppUser customer = resolveCustomer(loginId);
        return voucherRepository.findByUserAndIsUsedFalse(customer);
    }

    @Transactional(readOnly = true)
    public Voucher validateVoucherForUser(String loginId, String voucherCode) {
        if (voucherCode == null || voucherCode.isBlank()) {
            throw new IllegalArgumentException("Vui long nhap ma voucher.");
        }
        AppUser customer = resolveCustomer(loginId);
        return voucherRepository.findByCodeAndUserAndIsUsedFalse(voucherCode.trim(), customer)
                .orElseThrow(() -> new IllegalArgumentException("Voucher khong hop le hoac da duoc su dung."));
    }

    @Transactional
    public Booking applyVoucherToBooking(String loginId, Long bookingId, String voucherCode) {
        if (voucherCode == null || voucherCode.isBlank()) {
            throw new IllegalArgumentException("Vui long nhap ma voucher.");
        }

        AppUser customer = resolveCustomer(loginId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay booking."));

        if (booking.getCustomer() == null || !booking.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Booking nay khong thuoc tai khoan cua ban.");
        }
        if (booking.getPaymentStatus() == PaymentStatus.PAID || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking khong con hop le de ap dung voucher.");
        }

        Voucher voucher = voucherRepository.findByCodeAndUserAndIsUsedFalse(voucherCode.trim(), customer)
                .orElseThrow(() -> new IllegalArgumentException("Voucher khong hop le hoac da duoc su dung."));

        booking.setAppliedVoucherCode(voucher.getCode());
        booking.setDiscountAmount(BigDecimal.valueOf(voucher.getDiscountAmount()));
        return bookingRepository.save(booking);
    }

    @Transactional
    public void markVoucherUsedIfApplied(Booking booking) {
        if (booking == null || booking.getAppliedVoucherCode() == null || booking.getAppliedVoucherCode().isBlank()) {
            return;
        }
        voucherRepository.findByCode(booking.getAppliedVoucherCode()).ifPresent(voucher -> {
            if (!voucher.isUsed()) {
                voucher.setUsed(true);
                voucherRepository.save(voucher);
            }
        });
    }

    private AppUser resolveCustomer(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("Khong tim thay thong tin dang nhap.");
        }
        if (loginId.contains("@")) {
            return appUserRepository.findByEmailIgnoreCase(loginId)
                    .orElseThrow(() -> new IllegalArgumentException("Khong tim thay khach hang."));
        }
        return appUserRepository.findByPhone(loginId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay khach hang."));
    }
}
