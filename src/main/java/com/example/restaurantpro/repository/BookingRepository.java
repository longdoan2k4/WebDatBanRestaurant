package com.example.restaurantpro.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.restaurantpro.dto.DailyRevenueDto;
import com.example.restaurantpro.model.Booking;
import com.example.restaurantpro.model.BookingStatus;
import com.example.restaurantpro.model.PaymentStatus;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Load booking cùng danh sách món ăn trong một câu query duy nhất.
     * Dùng cho EmailService để tránh LazyInitializationException trong thread @Async.
     */
    @Query("""
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.items bi
        LEFT JOIN FETCH bi.menuItem
        WHERE b.id = :id
    """)
    Optional<Booking> findByIdWithItems(@Param("id") Long id);

    List<Booking> findByCustomer_PhoneOrderByBookingDateTimeDesc(String phone);

    List<Booking> findByCustomer_EmailOrderByBookingDateTimeDesc(String email);

    List<Booking> findAllByOrderByBookingDateTimeDesc();

    List<Booking> findByBookingDateTimeBetweenOrderByBookingDateTimeAsc(LocalDateTime start, LocalDateTime end);

    boolean existsByCustomer_Id(Long userId);

    boolean existsByDiningTable_Id(Long tableId);

    @Query("""
      select case when count(bi) > 0 then true else false end
      from BookingItem bi
      where bi.menuItem.id = :menuItemId
    """)
    boolean existsBookingItemByMenuItemId(@Param("menuItemId") Long menuItemId);

    @Query("""
        select b
        from Booking b
        where b.diningTable.id in :tableIds
          and b.status not in :excludedStatuses
          and b.bookingDateTime < :toTime
          and b.endTime > :fromTime
    """)
    List<Booking> findOverlappingBookings(@Param("tableIds") List<Long> tableIds,
                                          @Param("fromTime") LocalDateTime fromTime,
                                          @Param("toTime") LocalDateTime toTime,
                                          @Param("excludedStatuses") List<BookingStatus> excludedStatuses);

        List<Booking> findByDiningTable_IdAndStatusNotInAndBookingDateTimeBetweenOrderByBookingDateTimeAsc(
        Long tableId,
        List<BookingStatus> excludedStatuses,
        LocalDateTime fromTime,
        LocalDateTime toTime
        );

    @Query("""
        select count(b)
        from Booking b
        where b.diningTable.id = :tableId
          and b.status <> com.example.restaurantpro.model.BookingStatus.CANCELLED
          and b.bookingDateTime < :newEnd
          and b.endTime > :newStart
    """)
    long countConflictingBookings(@Param("tableId") Long tableId,
                                  @Param("newStart") LocalDateTime newStart,
                                  @Param("newEnd") LocalDateTime newEnd);

    @Query("""
        select count(b)
        from Booking b
        where b.diningTable.id = :tableId
          and b.status <> com.example.restaurantpro.model.BookingStatus.CANCELLED
          and b.bookingDateTime <= :atTime
          and b.endTime > :atTime
    """)
    long countActiveBookingsAtTime(@Param("tableId") Long tableId,
                                   @Param("atTime") LocalDateTime atTime);

    @Query("""
        select case when count(b) > 0 then true else false end
        from Booking b
        where b.diningTable.id = :tableId
          and b.status in :activeStatuses
          and b.endTime > :now
    """)
    boolean existsActiveBookingByTableId(@Param("tableId") Long tableId,
                                         @Param("activeStatuses") List<BookingStatus> activeStatuses,
                                         @Param("now") LocalDateTime now);

    @Query("""
        select coalesce(sum(b.totalAmount), 0)
        from Booking b
        where b.paymentStatus = :paidStatus
          and function('date', b.bookingDateTime) = :date
    """)
    BigDecimal getRevenueByDate(@Param("date") LocalDate date,
                                @Param("paidStatus") PaymentStatus paidStatus);

    @Query("""
        select coalesce(sum(b.totalAmount), 0)
        from Booking b
        where b.paymentStatus = :paidStatus
          and month(b.bookingDateTime) = :month
          and year(b.bookingDateTime) = :year
    """)
    BigDecimal getRevenueByMonth(@Param("month") Integer month,
                                 @Param("year") Integer year,
                                 @Param("paidStatus") PaymentStatus paidStatus);

    @Query("""
        select count(b)
        from Booking b
        where b.paymentStatus = :paidStatus
          and function('date', b.bookingDateTime) = :date
    """)
    Long countPaidBookingsByDate(@Param("date") LocalDate date,
                                 @Param("paidStatus") PaymentStatus paidStatus);

    @Query("""
        select count(b)
        from Booking b
        where b.paymentStatus = :paidStatus
          and month(b.bookingDateTime) = :month
          and year(b.bookingDateTime) = :year
    """)
    Long countPaidBookingsByMonth(@Param("month") Integer month,
                                  @Param("year") Integer year,
                                  @Param("paidStatus") PaymentStatus paidStatus);

    @Query("""
        select new com.example.restaurantpro.dto.DailyRevenueDto(
            day(b.bookingDateTime),
            coalesce(sum(b.totalAmount), 0)
        )
        from Booking b
        where b.paymentStatus = :paidStatus
          and month(b.bookingDateTime) = :month
          and year(b.bookingDateTime) = :year
        group by day(b.bookingDateTime)
        order by day(b.bookingDateTime)
    """)
    List<DailyRevenueDto> getRevenueStatsByDaysInMonth(@Param("month") Integer month,
                                                       @Param("year") Integer year,
                                                       @Param("paidStatus") PaymentStatus paidStatus);

    @Query("""
      select distinct b
      from Booking b
      join fetch b.diningTable dt
      join fetch b.items bi
      join fetch bi.menuItem mi
      where (b.status = :confirmedStatus or b.paymentStatus = :paidStatus)
        and b.status not in :excludedStatuses
        and b.endTime > :now
      order by b.bookingDateTime asc, dt.floor asc, dt.tableNumber asc
    """)
    List<Booking> findKitchenBookingsForActiveOrders(
            @Param("now") LocalDateTime now,
            @Param("confirmedStatus") BookingStatus confirmedStatus,
            @Param("paidStatus") PaymentStatus paidStatus,
            @Param("excludedStatuses") List<BookingStatus> excludedStatuses);

    @Query("SELECT b FROM Booking b WHERE b.status IN :statuses AND b.bookingDateTime < :timeLimit")
    List<Booking> findOverdueBookings(
            @Param("statuses") List<BookingStatus> statuses,
            @Param("timeLimit") LocalDateTime timeLimit
    );

    @Query("SELECT count(b) FROM Booking b WHERE b.customer.id = :userId AND b.status = :status")
    long countCanceledBookingsByUserId(@Param("userId") Long userId, @Param("status") BookingStatus status);
}