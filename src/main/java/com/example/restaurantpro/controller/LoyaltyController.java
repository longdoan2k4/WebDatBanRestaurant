package com.example.restaurantpro.controller;

import com.example.restaurantpro.model.Voucher;
import com.example.restaurantpro.model.Booking;
import com.example.restaurantpro.service.LoyaltyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/loyalty")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    public LoyaltyController(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    @PostMapping("/redeem")
    public ResponseEntity<Map<String, Object>> redeemVoucher(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("success", false);
            response.put("message", "Bạn cần đăng nhập để thực hiện chức năng này.");
            return ResponseEntity.status(401).body(response);
        }

        try {
            Voucher voucher = loyaltyService.redeemVoucher(authentication.getName());
            response.put("success", true);
            response.put("message", "Đổi Voucher thành công!");
            response.put("voucherCode", voucher.getCode());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/apply-voucher")
    public ResponseEntity<Map<String, Object>> applyVoucher(Authentication authentication,
                                                            @RequestParam Long bookingId,
                                                            @RequestParam String voucherCode) {
        Map<String, Object> response = new HashMap<>();
        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("success", false);
            response.put("message", "Ban can dang nhap de ap dung voucher.");
            return ResponseEntity.status(401).body(response);
        }
        try {
            Booking booking = loyaltyService.applyVoucherToBooking(authentication.getName(), bookingId, voucherCode);
            response.put("success", true);
            response.put("message", "Ap dung voucher thanh cong.");
            response.put("voucherCode", booking.getAppliedVoucherCode());
            response.put("discountAmount", booking.getDiscountAmount());
            response.put("totalAmount", booking.getTotalAmount());
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

    @PostMapping("/validate-voucher")
    public ResponseEntity<Map<String, Object>> validateVoucher(Authentication authentication,
                                                               @RequestParam String voucherCode) {
        Map<String, Object> response = new HashMap<>();
        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("success", false);
            response.put("message", "Ban can dang nhap de ap dung voucher.");
            return ResponseEntity.status(401).body(response);
        }
        try {
            Voucher voucher = loyaltyService.validateVoucherForUser(authentication.getName(), voucherCode);
            response.put("success", true);
            response.put("message", "Voucher hop le.");
            response.put("voucherCode", voucher.getCode());
            response.put("discountAmount", voucher.getDiscountAmount());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
