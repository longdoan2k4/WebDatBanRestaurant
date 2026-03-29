package com.example.restaurantpro.controller;

import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.restaurantpro.model.AppUser;
import com.example.restaurantpro.service.AppUserService;
import com.example.restaurantpro.service.OtpService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class OtpController {

    private static final String OTP_PENDING_EMAIL = "OTP_PENDING_EMAIL";
    private static final String SET_PASSWORD_PENDING_EMAIL = "SET_PASSWORD_PENDING_EMAIL";
    private static final HttpSessionSecurityContextRepository SECURITY_CONTEXT_REPOSITORY =
            new HttpSessionSecurityContextRepository();

    private final OtpService otpService;
    private final AppUserService appUserService;

    public OtpController(OtpService otpService, AppUserService appUserService) {
        this.otpService = otpService;
        this.appUserService = appUserService;
    }

    @GetMapping("/otp/verify")
    public String verifyPage(HttpSession session, Model model) {
        String pendingEmail = getPendingEmail(session);
        if (pendingEmail == null) {
            return "redirect:/login";
        }
        model.addAttribute("maskedEmail", maskEmail(pendingEmail));
        return "otp/verify";
    }

    @PostMapping("/otp/verify")
    public String verifyOtp(@RequestParam String otp,
                            HttpServletRequest request,
                            HttpServletResponse response,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        String pendingEmail = getPendingEmail(session);
        if (pendingEmail == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phiên xác thực đã hết hạn, vui lòng đăng nhập lại.");
            return "redirect:/login";
        }

        boolean valid = otpService.verifyOtp(pendingEmail, otp);
        if (!valid) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã OTP không hợp lệ hoặc đã hết hạn.");
            return "redirect:/otp/verify";
        }

        String normalizedEmail = pendingEmail.trim().toLowerCase();
        AppUser user = appUserService.findByEmail(normalizedEmail).orElse(null);

        session.removeAttribute(OTP_PENDING_EMAIL);

        if (user == null && session.getAttribute(SET_PASSWORD_PENDING_EMAIL) != null) {
            redirectAttributes.addFlashAttribute("infoMessage", "Xác thực OTP thành công. Vui lòng bổ sung số điện thoại và mật khẩu.");
            return "redirect:/set-password";
        }

        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tài khoản Google. Vui lòng đăng nhập lại.");
            return "redirect:/login";
        }

        String loginId = user.getPhone() != null && !user.getPhone().isBlank() ? user.getPhone() : user.getEmail();
        if (loginId == null || loginId.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xác thực tài khoản Google. Vui lòng thử lại.");
            return "redirect:/login";
        }
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                loginId,
                null,
                user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.name())).collect(Collectors.toSet())
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        SECURITY_CONTEXT_REPOSITORY.saveContext(context, request, response);

        redirectAttributes.addFlashAttribute("successMessage", "Xác thực OTP thành công.");
        return "redirect:/booking/start";
    }

    @PostMapping("/otp/resend")
    public String resendOtp(HttpSession session, RedirectAttributes redirectAttributes) {
        String pendingEmail = getPendingEmail(session);
        if (pendingEmail == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phiên xác thực đã hết hạn, vui lòng đăng nhập lại.");
            return "redirect:/login";
        }

        otpService.generateAndSendOtp(pendingEmail);
        redirectAttributes.addFlashAttribute("infoMessage", "Đã gửi lại mã OTP qua email.");
        return "redirect:/otp/verify";
    }

    private String getPendingEmail(HttpSession session) {
        Object value = session.getAttribute(OTP_PENDING_EMAIL);
        if (value == null) {
            return null;
        }
        String email = value.toString();
        return email.isBlank() ? null : email;
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        String prefix = email.substring(0, Math.min(2, atIndex));
        return prefix + "***" + email.substring(atIndex);
    }
}
