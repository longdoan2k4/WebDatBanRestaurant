package com.example.restaurantpro.task;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.restaurantpro.model.AppUser;
import com.example.restaurantpro.model.Booking;
import com.example.restaurantpro.model.BookingStatus;
import com.example.restaurantpro.repository.AppUserRepository;
import com.example.restaurantpro.repository.BookingRepository;

@Component
public class BookingCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(BookingCleanupTask.class);

    private final BookingRepository bookingRepository;
    private final AppUserRepository appUserRepository;

    public BookingCleanupTask(BookingRepository bookingRepository, AppUserRepository appUserRepository) {
        this.bookingRepository = bookingRepository;
        this.appUserRepository = appUserRepository;
    }

    @Scheduled(fixedRate = 10000)
    @Transactional
    public void cleanupOverdueBookings() {
        System.out.println("==================================================");
        System.out.println("[CRON JOB] Bắt đầu chạy quét đơn quá hạn...");
        log.info("Starting BookingCleanupTask to cancel overdue bookings...");

        // Find bookings overdue by 5 hours
        LocalDateTime timeLimit = LocalDateTime.now().minusHours(5);
        List<BookingStatus> activeStatuses = Arrays.asList(BookingStatus.PENDING, BookingStatus.CONFIRMED);

        List<Booking> overdueBookings = bookingRepository.findOverdueBookings(activeStatuses, timeLimit);

        if (overdueBookings.isEmpty()) {
            System.out.println("[CRON JOB] Không tìm thấy đơn nào quá hạn.");
            System.out.println("==================================================");
            return;
        }

        int canceledCount = 0;
        int lockedUsersCount = 0;

        for (Booking booking : overdueBookings) {
            System.out.println("[CRON JOB] Đang hủy đơn Booking ID: " + booking.getId() + " - Khách hàng: "
                    + (booking.getCustomer() != null ? booking.getCustomer().getFullName() : "N/A"));
            // Change status to CANCELLED
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            canceledCount++;

            AppUser customer = booking.getCustomer();
            if (customer != null && !customer.isLocked()) {
                // Count canceled bookings of this user
                long userCancelCount = bookingRepository.countCanceledBookingsByUserId(customer.getId(),
                        BookingStatus.CANCELLED);
                System.out.println("[CRON JOB] Khách hàng " + customer.getId() + " - Tổng số đơn huỷ hiện tại: "
                        + userCancelCount);

                // If >= 2, lock the user
                if (userCancelCount >= 2) {
                    System.out.println("[CRON JOB] ==> Khóa tài khoản khách hàng ID: " + customer.getId()
                            + " do vi phạm chính sách Spam!");
                    customer.setLocked(true);
                    customer.setEnabled(false); // Also disable user so they cannot login
                    appUserRepository.save(customer);
                    lockedUsersCount++;
                }
            }
        }

        System.out.println(
                "[CRON JOB] Hoàn thành. Đã hủy " + canceledCount + " đơn, khóa " + lockedUsersCount + " tài khoản.");
        System.out.println("==================================================");
    }
}
