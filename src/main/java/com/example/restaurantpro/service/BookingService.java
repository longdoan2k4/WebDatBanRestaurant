package com.example.restaurantpro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.restaurantpro.dto.DailyRevenueDto;
import com.example.restaurantpro.model.AppUser;
import com.example.restaurantpro.model.Booking;
import com.example.restaurantpro.model.BookingItem;
import com.example.restaurantpro.model.BookingStatus;
import com.example.restaurantpro.model.DiningTable;
import com.example.restaurantpro.model.MenuItem;
import com.example.restaurantpro.model.PaymentMethod;
import com.example.restaurantpro.model.PaymentStatus;
import com.example.restaurantpro.model.PaymentTransaction;
import com.example.restaurantpro.model.PaymentTransactionType;
import com.example.restaurantpro.repository.BookingRepository;

@Service
public class BookingService {

    private static final BigDecimal LATE_CANCEL_FEE_RATE = new BigDecimal("0.30");

    private final BookingRepository bookingRepository;
    private final AppUserService appUserService;
    private final TableService tableService;
    private final MenuService menuService;
    private final LoyaltyService loyaltyService;

    public BookingService(BookingRepository bookingRepository,
                          AppUserService appUserService,
                          TableService tableService,
                          MenuService menuService,
                          LoyaltyService loyaltyService) {
        this.bookingRepository = bookingRepository;
        this.appUserService = appUserService;
        this.tableService = tableService;
        this.menuService = menuService;
        this.loyaltyService = loyaltyService;
    }

    public BigDecimal getRevenueByDate(LocalDate date) {
        return bookingRepository.getRevenueByDate(date, PaymentStatus.PAID);
    }

    public BigDecimal getRevenueByMonth(Integer month, Integer year) {
        return bookingRepository.getRevenueByMonth(month, year, PaymentStatus.PAID);
    }

    public Long countPaidBookingsByDate(LocalDate date) {
        return bookingRepository.countPaidBookingsByDate(date, PaymentStatus.PAID);
    }

    public Long countPaidBookingsByMonth(Integer month, Integer year) {
        return bookingRepository.countPaidBookingsByMonth(month, year, PaymentStatus.PAID);
    }

    public List<DailyRevenueDto> getRevenueStatsByDaysInMonth(Integer month, Integer year) {
        return bookingRepository.getRevenueStatsByDaysInMonth(month, year, PaymentStatus.PAID);
    }

    public List<Booking> getKitchenOrdersForActiveBookings() {
        LocalDateTime now = LocalDateTime.now();
        return bookingRepository.findKitchenBookingsForActiveOrders(
                now,
                BookingStatus.CONFIRMED,
                PaymentStatus.PAID,
                List.of(BookingStatus.CANCELLED, BookingStatus.NO_SHOW)
        );
    }

    public Booking createBooking(String customerLoginId,
                                 Long tableId,
                                 Integer guestCount,
                                 LocalDateTime bookingDateTime,
                                 Integer durationHours,
                                 String notes,
                                 Map<Long, Integer> selectedItems,
                                 PaymentMethod paymentMethod) {

        if (bookingDateTime == null || !bookingDateTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Ban da chon sai ngay gio. Vui long chon thoi diem lon hon hien tai.");
        }
        int normalizedDuration = (durationHours == null || durationHours < 1) ? 2 : durationHours;
        LocalDateTime bookingEndTime = bookingDateTime.plusHours(normalizedDuration).plusMinutes(30);

        AppUser customer = appUserService.findByLoginId(customerLoginId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay khach hang."));

        int hour = bookingDateTime.getHour();
        if (hour >= 17 && hour <= 20 && !customer.isVip()) {
            throw new IllegalArgumentException("Khung giờ vàng (17h-20h) chỉ dành riêng cho khách hàng VIP!");
        }

        DiningTable table = tableService.getTableById(tableId);
        if (table.getCapacity() == null || !table.getCapacity().equals(guestCount)) {
            throw new IllegalArgumentException("Ban da chon ban khong dung suc chua voi so khach.");
        }

        long countConflict = bookingRepository.countConflictingBookings(tableId, bookingDateTime, bookingEndTime);
        if (countConflict >= 1) {
            throw new IllegalStateException("Het ban trong loai nay trong khung gio ban chon.");
        }

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setDiningTable(table);
        booking.setGuestCount(guestCount);
        booking.setBookingDateTime(bookingDateTime);
        booking.setEndTime(bookingEndTime);
        booking.setNotes(notes);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentMethod(paymentMethod == null ? PaymentMethod.PAY_AT_RESTAURANT : paymentMethod);
        booking.setPaymentStatus(PaymentStatus.UNPAID);

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : selectedItems.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }

            MenuItem menuItem = menuService.findById(entry.getKey());
            BookingItem bookingItem = new BookingItem(menuItem, entry.getValue(), menuItem.getPrice());
            booking.addItem(bookingItem);
            total = total.add(menuItem.getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
        }

        booking.setTotalAmount(total);
        return bookingRepository.save(booking);
    }

    public Booking save(Booking booking) {
        return bookingRepository.save(booking);
    }

    public List<Booking> getBookingsForUser(String loginId) {
        if (loginId != null && loginId.contains("@")) {
            return bookingRepository.findByCustomer_EmailOrderByBookingDateTimeDesc(loginId);
        }
        return bookingRepository.findByCustomer_PhoneOrderByBookingDateTimeDesc(loginId);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByBookingDateTimeDesc();
    }

    public List<Booking> getBookingsByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return bookingRepository.findByBookingDateTimeBetweenOrderByBookingDateTimeAsc(start, end);
    }

    public Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay don dat ban."));
    }

    public void cancel(Long id) {
        cancelByAdmin(id);
    }

    public void cancelByAdmin(Long id) {
        Booking booking = findById(id);
        ensureNotFinalized(booking);
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Don nay da thanh toan. Admin khong the huy va khong the danh dau khong den.");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    public void markNoShow(Long id) {
        Booking booking = findById(id);
        ensureNotFinalized(booking);
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Don nay da thanh toan. Admin khong the danh dau khong den.");
        }
        booking.setStatus(BookingStatus.NO_SHOW);
        bookingRepository.save(booking);
    }

    public CancelResult cancelByCustomer(Long id, String customerLoginId) {
        Booking booking = findById(id);

        if (booking.getCustomer() == null || !booking.getCustomer().matchesLoginId(customerLoginId)) {
            throw new IllegalArgumentException("Ban khong co quyen huy don dat ban nay.");
        }

        ensureNotFinalized(booking);

        BigDecimal feeAmount = BigDecimal.ZERO;
        boolean chargedFee = false;
        long hoursUntilBooking = ChronoUnit.HOURS.between(LocalDateTime.now(), booking.getBookingDateTime());
        if (booking.getPaymentStatus() == PaymentStatus.PAID && hoursUntilBooking >= 0 && hoursUntilBooking < 4) {
            BigDecimal baseAmount = booking.getTotalAmount() == null ? BigDecimal.ZERO : booking.getTotalAmount();
            feeAmount = baseAmount.multiply(LATE_CANCEL_FEE_RATE).setScale(0, RoundingMode.HALF_UP);
            chargedFee = true;
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        return new CancelResult(chargedFee, feeAmount);
    }

    public void markPaymentPending(Booking booking, String txnRef) {
        booking.setPaymentMethod(PaymentMethod.VNPAY);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setLatestPaymentTxnRef(txnRef);
        bookingRepository.save(booking);
    }

    public void markPaid(Booking booking, String txnRef, LocalDateTime paidAt) {
        booking.setPaymentMethod(PaymentMethod.VNPAY);
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setLatestPaymentTxnRef(txnRef);
        booking.setPaidAt(paidAt == null ? LocalDateTime.now() : paidAt);
        bookingRepository.save(booking);

        loyaltyService.addPointsForBooking(booking);
    }

    public void markPaymentFailed(Booking booking, String txnRef) {
        booking.setPaymentMethod(PaymentMethod.VNPAY);
        booking.setPaymentStatus(PaymentStatus.FAILED);
        booking.setLatestPaymentTxnRef(txnRef);
        bookingRepository.save(booking);
    }

    public void markRefundPending(Booking booking) {
        booking.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        bookingRepository.save(booking);
    }

    public void markRefunded(Booking booking, LocalDateTime refundedAt) {
        booking.setPaymentStatus(PaymentStatus.REFUNDED);
        bookingRepository.save(booking);
    }

    public void manuallyConfirmPaid(Long bookingId, String operatorName) {
        Booking booking = findById(bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.NO_SHOW) {
            throw new IllegalStateException("Don nay da ket thuc, khong the xac nhan thanh toan.");
        }
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            return;
        }
        if (booking.getPaymentStatus() == PaymentStatus.REFUNDED
                || booking.getPaymentStatus() == PaymentStatus.REFUND_PENDING) {
            throw new IllegalStateException("Don nay dang o trang thai hoan tien, khong the xac nhan da thanh toan.");
        }

        booking.setPaymentStatus(PaymentStatus.PAID);
        if (booking.getPaidAt() == null) {
            booking.setPaidAt(LocalDateTime.now());
        }

        PaymentTransaction manualTransaction = new PaymentTransaction(
                booking,
                "MANUAL-" + booking.getId() + "-" + System.currentTimeMillis(),
                booking.getTotalAmount() == null ? BigDecimal.ZERO : booking.getTotalAmount()
        );
        manualTransaction.setProvider("MANUAL");
        manualTransaction.setType(PaymentTransactionType.PAYMENT);
        manualTransaction.setStatus(PaymentStatus.PAID);
        manualTransaction.setCreatedBy(operatorName);
        manualTransaction.setMessage("Admin xac nhan da thanh toan cho booking #" + booking.getId());
        manualTransaction.setPaidAt(booking.getPaidAt());
        booking.addPaymentTransaction(manualTransaction);

        bookingRepository.save(booking);

        loyaltyService.addPointsForBooking(booking);
    }

    public long countBookings() {
        return bookingRepository.count();
    }

    public long countPreOrderedItems() {
        return bookingRepository.findAll().stream()
                .flatMap(booking -> booking.getItems().stream())
                .mapToLong(item -> item.getQuantity())
                .sum();
    }

    public List<Booking> getUpcomingBookings(int limit) {
        LocalDateTime now = LocalDateTime.now();
        return bookingRepository.findAll().stream()
                .filter(booking -> booking.getBookingDateTime() != null && booking.getBookingDateTime().isAfter(now))
                .sorted((a, b) -> a.getBookingDateTime().compareTo(b.getBookingDateTime()))
                .limit(limit)
                .toList();
    }

    private void ensureNotFinalized(Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.NO_SHOW) {
            throw new IllegalStateException("Don dat ban nay da ket thuc, khong the thao tac them.");
        }
    }

    public record CancelResult(boolean chargedFee, BigDecimal feeAmount) {
    }
}