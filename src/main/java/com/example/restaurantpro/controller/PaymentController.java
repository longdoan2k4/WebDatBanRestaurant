package com.example.restaurantpro.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.restaurantpro.config.VNPayConfig;
import com.example.restaurantpro.config.VnPayProperties;
import com.example.restaurantpro.dto.VnPayCallbackResult;
import com.example.restaurantpro.model.Booking;
import com.example.restaurantpro.model.BookingStatus;
import com.example.restaurantpro.model.AppUser;
import com.example.restaurantpro.model.PaymentMethod;
import com.example.restaurantpro.model.PaymentStatus;
import com.example.restaurantpro.model.Voucher;
import com.example.restaurantpro.repository.AppUserRepository;
import com.example.restaurantpro.service.BookingService;
import com.example.restaurantpro.service.EmailService;
import com.example.restaurantpro.service.LoyaltyService;
import com.example.restaurantpro.service.VnPayService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class PaymentController {

    private final BookingService bookingService;
    private final VnPayService vnPayService;
    private final VnPayProperties vnPayProperties;
    private final EmailService emailService;
    private final AppUserRepository appUserRepository;
    private final LoyaltyService loyaltyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentController(BookingService bookingService, VnPayService vnPayService,
            VnPayProperties vnPayProperties, EmailService emailService, AppUserRepository appUserRepository,
            LoyaltyService loyaltyService) {
        this.bookingService = bookingService;
        this.vnPayService = vnPayService;
        this.vnPayProperties = vnPayProperties;
        this.emailService = emailService;
        this.appUserRepository = appUserRepository;
        this.loyaltyService = loyaltyService;
    }

    @PostMapping("/api/payment/create")
    @ResponseBody
    public Map<String, String> createPayment(
            @RequestParam Long tableId,
            @RequestParam Integer guestCount,
            @RequestParam String bookingDateTime,
            @RequestParam(defaultValue = "2") Integer durationHours,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) String cartData,
            @RequestParam(required = false) String appliedVoucherCode,
            HttpServletRequest request) {
        if (!vnPayProperties.isReady()) {
            throw new IllegalStateException("VNPAY chua duoc cau hinh. Hay cap nhat payment.vnpay trong application.yml.");
        }

        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("Ban can dang nhap de thanh toan qua VNPAY.");
        }

        Map<Long, Integer> selectedItems = extractSelectedItems(request);
        if (selectedItems.isEmpty() && cartData != null && !cartData.isBlank()) {
            selectedItems = extractSelectedItemsFromCartData(cartData);
        }
        Voucher validatedVoucher = null;
        if (appliedVoucherCode != null && !appliedVoucherCode.isBlank()) {
            validatedVoucher = loyaltyService.validateVoucherForUser(authentication.getName(), appliedVoucherCode);
        }
        LocalDateTime parsedBookingDateTime = LocalDateTime.parse(bookingDateTime);

        Booking booking = bookingService.createBooking(
                authentication.getName(),
                tableId,
                guestCount,
                parsedBookingDateTime,
                durationHours,
                notes,
                selectedItems,
                PaymentMethod.VNPAY);

        if (validatedVoucher != null) {
            booking.setAppliedVoucherCode(validatedVoucher.getCode());
            booking.setDiscountAmount(BigDecimal.valueOf(validatedVoucher.getDiscountAmount()));
            booking = bookingService.save(booking);
        }

        if (!booking.hasPayableAmount() || booking.getTotalAmount().compareTo(new BigDecimal("5000")) < 0) {
            throw new IllegalArgumentException("So tien toi thieu cua VNPAY la 5.000 VND.");
        }

        String txnRef = String.valueOf(booking.getId());
        booking.setStatus(BookingStatus.PENDING);
        booking.setPaymentMethod(PaymentMethod.VNPAY);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setLatestPaymentTxnRef(txnRef);
        bookingService.save(booking);

        String createDate = VNPayConfig.nowVnPayFormat();
        String expireDate = VNPayConfig.plusMinutesVnPayFormat(vnPayProperties.getExpireMinutes());

        String returnUrl = vnPayProperties.getReturnUrl();
        if (returnUrl == null || returnUrl.isBlank()) {
            returnUrl = resolveBaseUrl(request) + "/payment/vnpay-return";
        } else if (returnUrl.endsWith("/payment/vnpay/return")) {
            returnUrl = returnUrl.substring(0, returnUrl.length() - "/payment/vnpay/return".length()) + "/payment/vnpay-return";
        }

        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnPayProperties.getTmnCode());
        vnpParams.put("vnp_Amount", booking.getFinalAmount().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).toPlainString());
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", "Thanh toan dat ban #" + booking.getId());
        vnpParams.put("vnp_OrderType", vnPayProperties.getOrderType());
        vnpParams.put("vnp_Locale", vnPayProperties.getLocale());
        vnpParams.put("vnp_ReturnUrl", returnUrl);
        vnpParams.put("vnp_IpAddr", request.getRemoteAddr() == null ? "127.0.0.1" : request.getRemoteAddr());
        vnpParams.put("vnp_CreateDate", createDate);
        vnpParams.put("vnp_ExpireDate", expireDate);

        String hashData = VNPayConfig.hashAllFields(vnpParams);
        String secureHash = VNPayConfig.hmacSHA512(vnPayProperties.getHashSecret(), hashData);
        String queryUrl = VNPayConfig.buildQueryUrl(vnpParams);
        String paymentUrl = vnPayProperties.getPayUrl() + "?" + queryUrl + "&vnp_SecureHash=" + secureHash;

        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", paymentUrl);
        response.put("bookingId", String.valueOf(booking.getId()));
        return response;
    }

    @GetMapping("/payment/vnpay/pay")
    public String redirectToVnPay(@RequestParam Long bookingId,
            Authentication authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        Booking booking = bookingService.findById(bookingId);
        ensureAccess(authentication, booking);

        try {
            String paymentUrl = vnPayService.createPaymentUrl(booking, request);
            return "redirect:" + paymentUrl;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/booking/my";
        }
    }

    @GetMapping("/payment/vnpay/return")
    public String vnpayReturnLegacy(@RequestParam Map<String, String> allParams, Model model) {
        return handleVnpayReturnInternal(allParams, model);
    }

    @GetMapping("/payment/vnpay-return")
    public String vnpayReturn(@RequestParam Map<String, String> allParams, Model model) {
        return handleVnpayReturnInternal(allParams, model);
    }

    @GetMapping("/payment/vnpay/ipn")
    @ResponseBody
    public Map<String, String> vnpayIpn(HttpServletRequest request) {
        return vnPayService.handleIpn(extractParams(request));
    }

    private void ensureAccess(Authentication authentication, Booking booking) {
        if (authentication == null) {
            throw new AccessDeniedException("Ban chua dang nhap.");
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        boolean isOwner = booking.getCustomer() != null
                && booking.getCustomer().matchesLoginId(authentication.getName());
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Ban khong duoc phep thanh toan booking nay.");
        }
    }

    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return params;
    }

    private String resolveBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        boolean isDefaultPort = ("http".equalsIgnoreCase(scheme) && serverPort == 80)
                || ("https".equalsIgnoreCase(scheme) && serverPort == 443);
        return isDefaultPort
                ? scheme + "://" + serverName
                : scheme + "://" + serverName + ":" + serverPort;
    }

    private String handleVnpayReturnInternal(Map<String, String> allParams, Model model) {
        try {
            Map<String, String> vnpParams = buildVnpParamsForHash(allParams);
            String vnpSecureHash = allParams.get("vnp_SecureHash");

            if (vnpSecureHash == null || vnpSecureHash.isBlank()) {
                model.addAttribute("title", "Thanh toan that bai");
                model.addAttribute("message", "Khong tim thay chu ky bao mat tu VNPAY.");
                model.addAttribute("responseCode", allParams.get("vnp_ResponseCode"));
                return "payment-fail";
            }

            String signData = VNPayConfig.hashAllFields(vnpParams);
            String calculatedHash = VNPayConfig.hmacSHA512(vnPayProperties.getHashSecret(), signData);
            if (!vnpSecureHash.equalsIgnoreCase(calculatedHash)) {
                model.addAttribute("title", "Thanh toan that bai");
                model.addAttribute("message", "Du lieu tra ve khong hop le (sai checksum).");
                model.addAttribute("responseCode", allParams.get("vnp_ResponseCode"));
                return "payment-fail";
            }

            String responseCode = allParams.get("vnp_ResponseCode");
            model.addAttribute("txnRef", allParams.get("vnp_TxnRef"));
            model.addAttribute("amount", formatAmount(allParams.get("vnp_Amount")));
            model.addAttribute("bankCode", allParams.get("vnp_BankCode"));
            model.addAttribute("payDate", allParams.get("vnp_PayDate"));
            model.addAttribute("transactionNo", allParams.get("vnp_TransactionNo"));
            model.addAttribute("responseCode", responseCode);

            if ("00".equals(responseCode)) {
                updateBookingAfterPayment(allParams.get("vnp_TxnRef"), true);
                model.addAttribute("title", "Thanh toan thanh cong");
                model.addAttribute("message", "Giao dich VNPAY da duoc xac nhan thanh cong.");
                return "payment-success";
            }

            updateBookingAfterPayment(allParams.get("vnp_TxnRef"), false);
            if ("24".equals(responseCode)) {
                model.addAttribute("title", "Da huy thanh toan");
                model.addAttribute("message", "Ban da huy giao dich tren cong thanh toan VNPAY.");
                return "payment-fail";
            }

            model.addAttribute("title", "Thanh toan that bai");
            model.addAttribute("message", "Giao dich khong thanh cong. Ma phan hoi: " + responseCode);
            return "payment-fail";
        } catch (Exception ex) {
            model.addAttribute("title", "He thong tam thoi loi");
            model.addAttribute("message", "Khong the xu ly ket qua thanh toan: " + ex.getMessage());
            model.addAttribute("responseCode", allParams.get("vnp_ResponseCode"));
            return "payment-fail";
        }
    }

    private void updateBookingAfterPayment(String txnRef, boolean successful) {
        if (txnRef == null || txnRef.isBlank()) {
            return;
        }
        Long bookingId;
        try {
            bookingId = Long.valueOf(txnRef);
        } catch (NumberFormatException ex) {
            return;
        }

        Booking booking = bookingService.findById(bookingId);
        if (successful) {
            boolean alreadyPaid = booking.getPaymentStatus() == PaymentStatus.PAID;
            if (!alreadyPaid) {
                booking.setStatus(BookingStatus.CONFIRMED);
                booking.setPaymentStatus(PaymentStatus.PAID);
                booking.setPaidAt(LocalDateTime.now());
                bookingService.save(booking);

                AppUser customer = booking.getCustomer();
                if (customer != null) {
                    long pointsToAdd = booking.getTotalAmount()
                            .divide(BigDecimal.valueOf(15000), 0, RoundingMode.DOWN)
                            .multiply(BigDecimal.TEN)
                            .longValue();
                    if (pointsToAdd > 0) {
                        customer.setLoyaltyPoints(customer.getLoyaltyPoints() + (int) pointsToAdd);
                        AppUser savedUser = appUserRepository.save(customer);
                        refreshPrincipalIfCurrentUser(savedUser);
                    }
                }

                if (booking.getCustomer() != null && booking.getCustomer().getEmail() != null) {
                    emailService.sendBookingConfirmation(booking.getCustomer().getEmail(), booking);
                }
                loyaltyService.markVoucherUsedIfApplied(booking);
            }
        } else {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setPaymentStatus(PaymentStatus.FAILED);
            bookingService.save(booking);
        }
    }

    private void refreshPrincipalIfCurrentUser(AppUser updatedUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || updatedUser == null || !updatedUser.matchesLoginId(authentication.getName())) {
            return;
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            return;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String username = updatedUser.getPhone() != null && !updatedUser.getPhone().isBlank()
                ? updatedUser.getPhone()
                : updatedUser.getEmail();
        if (username == null || username.isBlank()) {
            return;
        }

        UserDetails refreshedPrincipal = User.withUsername(username)
                .password(updatedUser.getPassword() == null ? "" : updatedUser.getPassword())
                .disabled(!updatedUser.isEnabled())
                .authorities(authorities)
                .build();

        UsernamePasswordAuthenticationToken refreshedAuthentication = new UsernamePasswordAuthenticationToken(
                refreshedPrincipal,
                authentication.getCredentials(),
                authorities);
        refreshedAuthentication.setDetails(authentication.getDetails());
        SecurityContextHolder.getContext().setAuthentication(refreshedAuthentication);
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

    private Map<Long, Integer> extractSelectedItemsFromCartData(String cartData) {
        Map<Long, Integer> selectedItems = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> items = objectMapper.readValue(
                    cartData,
                    new TypeReference<ArrayList<Map<String, Object>>>() {
                    });
            for (Map<String, Object> item : items) {
                Object menuItemIdValue = item.get("menuItemId");
                Object quantityValue = item.get("quantity");
                if (menuItemIdValue == null || quantityValue == null) {
                    continue;
                }
                long menuItemId = Long.parseLong(String.valueOf(menuItemIdValue));
                int quantity = Integer.parseInt(String.valueOf(quantityValue));
                if (quantity > 0) {
                    selectedItems.put(menuItemId, quantity);
                }
            }
        } catch (Exception ignored) {
        }
        return selectedItems;
    }

    private Map<String, String> buildVnpParamsForHash(Map<String, String> allParams) {
        Map<String, String> vnpParams = new TreeMap<>();
        allParams.forEach((key, value) -> {
            if (key != null
                    && key.startsWith("vnp_")
                    && value != null
                    && !value.isBlank()
                    && !"vnp_SecureHash".equals(key)
                    && !"vnp_SecureHashType".equals(key)) {
                vnpParams.put(key, value);
            }
        });
        return vnpParams;
    }

    private String formatAmount(String rawAmount) {
        if (rawAmount == null || rawAmount.isBlank()) {
            return "";
        }
        try {
            long value = Long.parseLong(rawAmount);
            long vnd = value / 100L;
            return String.format("%,d VND", vnd).replace(',', '.');
        } catch (NumberFormatException ex) {
            return rawAmount;
        }
    }
}
