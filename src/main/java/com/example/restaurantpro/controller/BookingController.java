package com.example.restaurantpro.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.restaurantpro.exception.BookingConflictException;
import com.example.restaurantpro.model.AppUser;
import com.example.restaurantpro.model.Booking;
import com.example.restaurantpro.model.PaymentMethod;
import com.example.restaurantpro.service.AppUserService;
import com.example.restaurantpro.service.BookingService;
import com.example.restaurantpro.service.EmailService;
import com.example.restaurantpro.service.LoyaltyService;
import com.example.restaurantpro.service.MenuService;
import com.example.restaurantpro.service.TableService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class BookingController {

    private final TableService tableService;
    private final MenuService menuService;
    private final BookingService bookingService;
    private final EmailService emailService;
    private final AppUserService appUserService;
    private final LoyaltyService loyaltyService;

    public BookingController(TableService tableService, MenuService menuService,
            BookingService bookingService, EmailService emailService, AppUserService appUserService,
            LoyaltyService loyaltyService) {
        this.tableService = tableService;
        this.menuService = menuService;
        this.bookingService = bookingService;
        this.emailService = emailService;
        this.appUserService = appUserService;
        this.loyaltyService = loyaltyService;
    }

    @GetMapping("/booking/start")
    public String startBooking(Model model) {
        model.addAttribute("minDateTime", LocalDateTime.now().plusHours(1));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("defaultDurationHours", 2);
        return "booking/start";
    }

    @PostMapping("/booking/tables")
    public String availableTables(@RequestParam Integer guestCount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime bookingDateTime,
            @RequestParam(defaultValue = "2") Integer durationHours,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bookingDateTime == null || !bookingDateTime.isAfter(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ban da chon sai ngay gio. Vui long chon thoi gian lon hon hien tai.");
            return "redirect:/booking/start";
        }

        int hour24 = bookingDateTime.getHour();
        if (hour24 >= 17 && hour24 <= 20 && authentication != null) {
            AppUser user = appUserService.findByLoginId(authentication.getName()).orElse(null);
            if (user != null && !user.isVip()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Khung giờ vàng (17h - 20h) chỉ dành riêng cho khách hàng VIP!");
                return "redirect:/booking/start";
            }
        }

        TableService.AvailableTablesResult availableResult = tableService.findAvailableExactCapacityTables(guestCount,
                bookingDateTime, durationHours);
        model.addAttribute("tables", availableResult.tables());
        model.addAttribute("totalTables", availableResult.totalTables());
        model.addAttribute("availableTables", availableResult.availableTables());
        model.addAttribute("guestCount", guestCount);
        model.addAttribute("bookingDateTime", bookingDateTime);
        model.addAttribute("durationHours", durationHours);
        model.addAttribute("bookingEndTime", bookingDateTime.plusHours(durationHours).plusMinutes(30));
        return "booking/tables";
    }

    @PostMapping("/api/booking/check-availability")
    @ResponseBody
    public Map<String, Object> checkAvailability(@RequestParam Integer guestCount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime bookingDateTime,
            @RequestParam(defaultValue = "2") Integer durationHours,
            Authentication authentication) {
        Map<String, Object> response = new LinkedHashMap<>();
        
        if (bookingDateTime == null || !bookingDateTime.isAfter(LocalDateTime.now())) {
            response.put("available", false);
            response.put("message", "Ngày giờ đến phải lớn hơn thời điểm hiện tại.");
            return response;
        }

        // Logic check VIP (Chốt chặn API Backend)
        int hour24 = bookingDateTime.getHour();
        if (hour24 >= 17 && hour24 <= 20 && authentication != null) {
            AppUser user = appUserService.findByLoginId(authentication.getName()).orElse(null);
            if (user != null && !user.isVip()) {
                response.put("available", false);
                response.put("message", "Khung giờ vàng (17h - 20h) chỉ dành riêng cho khách hàng VIP!");
                return response;
            }
        }

        TableService.AvailableTablesResult availableResult = tableService.findAvailableExactCapacityTables(guestCount,
                bookingDateTime, durationHours);
        
        if (availableResult.tables().isEmpty()) {
            response.put("available", false);
            if (availableResult.totalTables() == 0) {
                response.put("message", "Rất tiếc, hiện không còn loại bàn phù hợp với số lượng " + guestCount + " khách.");
            } else {
                response.put("message", "Rất tiếc, nhà hàng đã kín bàn vào khung giờ này. Quý khách vui lòng chọn một thời gian khác.");
            }
        } else {
            response.put("available", true);
        }
        
        return response;
    }

    @PostMapping("/booking/preorder")
    public String preOrder(@RequestParam Long tableId,
            @RequestParam Integer guestCount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime bookingDateTime,
            @RequestParam(defaultValue = "2") Integer durationHours,
            Model model) {
        model.addAttribute("selectedTable", tableService.getTableById(tableId));
        model.addAttribute("guestCount", guestCount);
        model.addAttribute("bookingDateTime", bookingDateTime);
        model.addAttribute("durationHours", durationHours);
        model.addAttribute("bookingEndTime", bookingDateTime.plusHours(durationHours).plusMinutes(30));
        model.addAttribute("groupedMenu", menuService.getGroupedAvailableMenu());
        model.addAttribute("estimatedBase", BigDecimal.ZERO);
        model.addAttribute("vnpayMinimum", BigDecimal.valueOf(5000));
        return "booking/preorder";
    }

    @PostMapping("/booking/confirm")
    public String confirmBooking(@RequestParam Long tableId,
            @RequestParam Integer guestCount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime bookingDateTime,
            @RequestParam(defaultValue = "2") Integer durationHours,
            @RequestParam(required = false) String notes,
            @RequestParam(defaultValue = "PAY_AT_RESTAURANT") PaymentMethod paymentMethod,
            Authentication authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        Map<Long, Integer> selectedItems = extractSelectedItems(request);

        Booking booking;
        try {
            booking = bookingService.createBooking(
                    authentication.getName(),
                    tableId,
                    guestCount,
                    bookingDateTime,
                    durationHours,
                    notes,
                    selectedItems,
                    paymentMethod);
        } catch (BookingConflictException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("bookingSuggestions", ex.getSuggestions());
            redirectAttributes.addFlashAttribute("guestCount", guestCount);
            redirectAttributes.addFlashAttribute("bookingDateTime", bookingDateTime);
            redirectAttributes.addFlashAttribute("durationHours", durationHours);
            return "redirect:/booking/start";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/booking/start";
        }

        if (paymentMethod == PaymentMethod.VNPAY && booking.hasPayableAmount()) {
            return "redirect:/payment/vnpay/pay?bookingId=" + booking.getId();
        }

        // Luồng thanh toán tại nhà hàng – gửi email xác nhận ngay
        if (booking.getCustomer() != null && booking.getCustomer().getEmail() != null) {
            emailService.sendBookingConfirmation(booking.getCustomer().getEmail(), booking);
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Dat ban thanh cong cho " + booking.getDiningTable().getName() + ". He thong da ghi nhan don cua ban.");
        return "redirect:/booking/my";
    }

    @PostMapping("/booking/confirm-without-menu")
    public String confirmWithoutMenu(@RequestParam Long tableId,
            @RequestParam Integer guestCount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime bookingDateTime,
            @RequestParam(defaultValue = "2") Integer durationHours,
            @RequestParam(required = false) String notes,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Booking savedBooking;
        try {
            savedBooking = bookingService.createBooking(authentication.getName(), tableId, guestCount, bookingDateTime,
                    durationHours, notes, Map.of(), PaymentMethod.PAY_AT_RESTAURANT);
        } catch (BookingConflictException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("bookingSuggestions", ex.getSuggestions());
            redirectAttributes.addFlashAttribute("guestCount", guestCount);
            redirectAttributes.addFlashAttribute("bookingDateTime", bookingDateTime);
            redirectAttributes.addFlashAttribute("durationHours", durationHours);
            return "redirect:/booking/start";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/booking/start";
        }
        // Gửi email xác nhận
        if (savedBooking.getCustomer() != null && savedBooking.getCustomer().getEmail() != null) {
            emailService.sendBookingConfirmation(savedBooking.getCustomer().getEmail(), savedBooking);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Dat ban thanh cong. Ban chua chon mon truoc.");
        return "redirect:/booking/my";
    }

    @GetMapping("/booking/my")
    public String myBookings(Authentication authentication, Model model) {
        model.addAttribute("bookings", bookingService.getBookingsForUser(authentication.getName()));
        return "booking/my-bookings";
    }

    @PostMapping("/booking/my/cancel")
    public String cancelMyBooking(@RequestParam Long bookingId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            BookingService.CancelResult result = bookingService.cancelByCustomer(bookingId, authentication.getName());
            if (result.chargedFee()) {
                redirectAttributes.addFlashAttribute("infoMessage",
                        "Ban da huy don. Theo chinh sach, phi huy muon la 30%: " + result.feeAmount().toPlainString()
                                + " VND.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Ban da huy don dat ban thanh cong.");
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/booking/my";
    }

    @PostMapping("/api/bookings/apply-voucher")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> applyVoucher(@RequestParam Long bookingId,
            @RequestParam String voucherCode,
            Authentication authentication) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("success", false);
            response.put("message", "Ban can dang nhap de ap dung voucher.");
            return ResponseEntity.status(401).body(response);
        }
        try {
            Booking booking = loyaltyService.applyVoucherToBooking(authentication.getName(), bookingId, voucherCode);
            response.put("success", true);
            response.put("message", "Ap dung voucher thanh cong.");
            response.put("bookingId", booking.getId());
            response.put("voucherCode", booking.getAppliedVoucherCode());
            response.put("discountAmount", booking.getDiscountAmount());
            response.put("finalAmount", booking.getFinalAmount());
            response.put("discountDisplay", booking.getDiscountAmountDisplay());
            response.put("finalAmountDisplay", booking.getFinalAmountDisplay());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception ex) {
            response.put("success", false);
            response.put("message", "Da xay ra loi he thong, vui long thu lai.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    private Map<Long, Integer> extractSelectedItems(HttpServletRequest request) {
        Map<Long, Integer> selectedItems = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (key.startsWith("qty_")) {
                try {
                    long itemId = Long.parseLong(key.replace("qty_", ""));
                    int quantity = Integer.parseInt(values[0]);
                    if (quantity > 0) {
                        selectedItems.put(itemId, quantity);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        });
        return selectedItems;
    }
}
