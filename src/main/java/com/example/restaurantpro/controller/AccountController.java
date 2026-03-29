package com.example.restaurantpro.controller;

import org.springframework.mail.MailException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.restaurantpro.dto.ChangePasswordRequest;
import com.example.restaurantpro.dto.CustomerProfileDto;
import com.example.restaurantpro.dto.ResetPasswordRequest;
import com.example.restaurantpro.dto.SetPasswordRequest;
import com.example.restaurantpro.model.AppUser;
import com.example.restaurantpro.service.LoyaltyService;
import com.example.restaurantpro.service.AppUserService;
import com.example.restaurantpro.service.OtpService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class AccountController {

    private static final String SET_PASSWORD_PENDING_EMAIL = "SET_PASSWORD_PENDING_EMAIL";
    private static final String SET_PASSWORD_PENDING_NAME = "SET_PASSWORD_PENDING_NAME";
    private static final String SET_PASSWORD_PENDING_GOOGLE_ID = "SET_PASSWORD_PENDING_GOOGLE_ID";
    private static final String PROFILE_UPDATE_PENDING_USER_ID = "PROFILE_UPDATE_PENDING_USER_ID";
    private static final String PROFILE_UPDATE_PENDING_FULL_NAME = "PROFILE_UPDATE_PENDING_FULL_NAME";
    private static final String PROFILE_UPDATE_PENDING_EMAIL = "PROFILE_UPDATE_PENDING_EMAIL";
    private static final String PROFILE_UPDATE_PENDING_PHONE = "PROFILE_UPDATE_PENDING_PHONE";
    private static final String PROFILE_UPDATE_VERIFY_EMAIL = "PROFILE_UPDATE_VERIFY_EMAIL";
    private static final String FORGOT_PASSWORD_EMAIL = "FORGOT_PASSWORD_EMAIL";
    private static final String FORGOT_PASSWORD_OTP_VERIFIED = "FORGOT_PASSWORD_OTP_VERIFIED";
    private static final HttpSessionSecurityContextRepository SECURITY_CONTEXT_REPOSITORY =
            new HttpSessionSecurityContextRepository();

    private final AppUserService appUserService;
    private final OtpService otpService;
    private final LoyaltyService loyaltyService;

    public AccountController(AppUserService appUserService, OtpService otpService, LoyaltyService loyaltyService) {
        this.appUserService = appUserService;
        this.otpService = otpService;
        this.loyaltyService = loyaltyService;
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        if (!model.containsAttribute("email")) {
            model.addAttribute("email", "");
        }
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String submitForgotPassword(@RequestParam("email") String email,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập email.");
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/forgot-password";
        }

        if (appUserService.findByEmail(normalizedEmail).isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Email chưa được đăng ký trong hệ thống.");
            redirectAttributes.addFlashAttribute("email", normalizedEmail);
            return "redirect:/forgot-password";
        }

        try {
            otpService.generateAndSendOtp(normalizedEmail);
            session.setAttribute(FORGOT_PASSWORD_EMAIL, normalizedEmail);
            session.setAttribute(FORGOT_PASSWORD_OTP_VERIFIED, Boolean.FALSE);
            redirectAttributes.addFlashAttribute("infoMessage", "Đã gửi mã OTP xác thực tới email của bạn.");
            return "redirect:/forgot-password/verify-otp";
        } catch (MailException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể gửi OTP lúc này. Vui lòng thử lại sau.");
            redirectAttributes.addFlashAttribute("email", normalizedEmail);
            return "redirect:/forgot-password";
        }
    }

    @GetMapping("/forgot-password/verify-otp")
    public String forgotPasswordVerifyOtpPage(HttpSession session,
                                              Model model,
                                              RedirectAttributes redirectAttributes) {
        String email = getSessionValue(session, FORGOT_PASSWORD_EMAIL);
        if (email == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập email để bắt đầu quên mật khẩu.");
            return "redirect:/forgot-password";
        }
        model.addAttribute("maskedEmail", maskEmail(email));
        return "verify-forgot-otp";
    }

    @PostMapping("/forgot-password/verify-otp")
    public String forgotPasswordVerifyOtp(@RequestParam("otp") String otp,
                                          HttpSession session,
                                          RedirectAttributes redirectAttributes) {
        String email = getSessionValue(session, FORGOT_PASSWORD_EMAIL);
        if (email == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phiên quên mật khẩu đã hết hạn.");
            return "redirect:/forgot-password";
        }

        if (!otpService.verifyOtp(email, otp)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã OTP không hợp lệ hoặc đã hết hạn.");
            return "redirect:/forgot-password/verify-otp";
        }

        session.setAttribute(FORGOT_PASSWORD_OTP_VERIFIED, Boolean.TRUE);
        redirectAttributes.addFlashAttribute("successMessage", "Xác thực OTP thành công. Vui lòng đặt lại mật khẩu.");
        return "redirect:/forgot-password/reset";
    }

    @PostMapping("/forgot-password/resend-otp")
    public String resendForgotPasswordOtp(HttpSession session, RedirectAttributes redirectAttributes) {
        String email = getSessionValue(session, FORGOT_PASSWORD_EMAIL);
        if (email == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phiên quên mật khẩu đã hết hạn.");
            return "redirect:/forgot-password";
        }
        try {
            otpService.generateAndSendOtp(email);
            redirectAttributes.addFlashAttribute("infoMessage", "Đã gửi lại mã OTP.");
        } catch (MailException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể gửi lại OTP lúc này.");
        }
        return "redirect:/forgot-password/verify-otp";
    }

    @GetMapping("/forgot-password/reset")
    public String resetPasswordPage(HttpSession session,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        if (!isForgotPasswordOtpVerified(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng xác thực OTP trước khi đặt lại mật khẩu.");
            return "redirect:/forgot-password";
        }
        if (!model.containsAttribute("resetPasswordRequest")) {
            model.addAttribute("resetPasswordRequest", new ResetPasswordRequest());
        }
        model.addAttribute("maskedEmail", maskEmail(getSessionValue(session, FORGOT_PASSWORD_EMAIL)));
        return "reset-password";
    }

    @PostMapping("/forgot-password/reset")
    public String submitResetPassword(@Valid @ModelAttribute("resetPasswordRequest") ResetPasswordRequest request,
                                      BindingResult bindingResult,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes,
                                      Model model) {
        String email = getSessionValue(session, FORGOT_PASSWORD_EMAIL);
        if (email == null || !isForgotPasswordOtpVerified(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phiên đặt lại mật khẩu đã hết hạn. Vui lòng thao tác lại.");
            return "redirect:/forgot-password";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("maskedEmail", maskEmail(email));
            return "reset-password";
        }

        try {
            appUserService.resetPasswordByEmail(email, request.getPassword(), request.getConfirmPassword());
            clearForgotPasswordSession(session);
            redirectAttributes.addFlashAttribute("successMessage", "Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("maskedEmail", maskEmail(email));
            model.addAttribute("errorMessage", ex.getMessage());
            return "reset-password";
        }
    }

    @GetMapping("/set-password")
    public String setPasswordPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (getSessionValue(session, SET_PASSWORD_PENDING_EMAIL) == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phiên đăng ký Google đã hết hạn. Vui lòng đăng nhập lại.");
            return "redirect:/login";
        }
        if (!model.containsAttribute("setPasswordRequest")) {
            model.addAttribute("setPasswordRequest", new SetPasswordRequest());
        }
        model.addAttribute("pendingEmail", getSessionValue(session, SET_PASSWORD_PENDING_EMAIL));
        model.addAttribute("pendingName", getSessionValue(session, SET_PASSWORD_PENDING_NAME));
        return "set-password";
    }

    @PostMapping("/set-password")
    public String submitSetPassword(@Valid @ModelAttribute("setPasswordRequest") SetPasswordRequest request,
                                    BindingResult bindingResult,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        String email = getSessionValue(session, SET_PASSWORD_PENDING_EMAIL);
        if (email == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phiên đăng ký Google đã hết hạn. Vui lòng đăng nhập lại.");
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("pendingEmail", email);
            model.addAttribute("pendingName", getSessionValue(session, SET_PASSWORD_PENDING_NAME));
            return "set-password";
        }

        try {
            appUserService.createGoogleUserWithPassword(
                    email,
                    getSessionValue(session, SET_PASSWORD_PENDING_NAME),
                    getSessionValue(session, SET_PASSWORD_PENDING_GOOGLE_ID),
                    request.getPhone(),
                    request.getPassword(),
                    request.getConfirmPassword()
            );
            clearPendingGoogleSession(session);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo tài khoản thành công. Bạn có thể đăng nhập bằng Email + Mật khẩu hoặc Google.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("pendingEmail", email);
            model.addAttribute("pendingName", getSessionValue(session, SET_PASSWORD_PENDING_NAME));
            model.addAttribute("errorMessage", ex.getMessage());
            return "set-password";
        }
    }

    @GetMapping("/profile")
    public String profilePage(Model model, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        AppUser user = appUserService.findByLoginId(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản hiện tại."));

        if (!model.containsAttribute("profile")) {
            model.addAttribute("profile", toProfileDto(user));
        }
        if (!model.containsAttribute("changePasswordRequest")) {
            model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        }
        model.addAttribute("myVouchers", loyaltyService.getAvailableVouchers(authentication.getName()));
        return "profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@Valid @ModelAttribute("changePasswordRequest") ChangePasswordRequest request,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        AppUser currentUser = appUserService.findByLoginId(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản hiện tại."));

        if (bindingResult.hasErrors()) {
            model.addAttribute("profile", toProfileDto(currentUser));
            model.addAttribute("changePasswordRequest", request);
            return "profile";
        }

        try {
            appUserService.changePassword(currentUser.getId(),
                    request.getCurrentPassword(),
                    request.getNewPassword(),
                    request.getConfirmNewPassword());
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công.");
            return "redirect:/profile";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("profile", toProfileDto(currentUser));
            model.addAttribute("changePasswordRequest", request);
            model.addAttribute("errorMessage", ex.getMessage());
            return "profile";
        }
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profile") CustomerProfileDto profile,
                                BindingResult bindingResult,
                                HttpSession session,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        AppUser currentUser = appUserService.findByLoginId(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản hiện tại."));

        if (bindingResult.hasErrors()) {
            model.addAttribute("profile", profile);
            return "profile";
        }

        try {
            String newFullName = profile.getFullName() == null ? null : profile.getFullName().trim();
            String newEmail = profile.getEmail() == null ? null : profile.getEmail().trim().toLowerCase();
            String newPhone = profile.getPhone() == null ? null : profile.getPhone().trim();

            boolean changed = !safeEquals(currentUser.getFullName(), newFullName)
                    || !safeEquals(currentUser.getEmail(), newEmail)
                    || !safeEquals(currentUser.getPhone(), newPhone);

            if (!changed) {
                redirectAttributes.addFlashAttribute("infoMessage", "Bạn chưa thay đổi thông tin nào.");
                return "redirect:/profile";
            }

            if (currentUser.getEmail() == null || currentUser.getEmail().isBlank()) {
                model.addAttribute("errorMessage", "Tài khoản chưa có email để xác thực OTP.");
                model.addAttribute("profile", profile);
                return "profile";
            }

            session.setAttribute(PROFILE_UPDATE_PENDING_USER_ID, currentUser.getId());
            session.setAttribute(PROFILE_UPDATE_PENDING_FULL_NAME, newFullName);
            session.setAttribute(PROFILE_UPDATE_PENDING_EMAIL, newEmail);
            session.setAttribute(PROFILE_UPDATE_PENDING_PHONE, newPhone);
            session.setAttribute(PROFILE_UPDATE_VERIFY_EMAIL, currentUser.getEmail().trim().toLowerCase());

            otpService.generateAndSendOtp(currentUser.getEmail().trim().toLowerCase());
            redirectAttributes.addFlashAttribute("infoMessage", "Đã gửi mã OTP tới email hiện tại. Vui lòng xác thực để lưu thay đổi.");
            return "redirect:/profile/verify-otp";
        } catch (MailException ex) {
            model.addAttribute("errorMessage", "Không thể gửi OTP xác thực. Vui lòng kiểm tra cấu hình email.");
            model.addAttribute("profile", profile);
            return "profile";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("profile", profile);
            return "profile";
        }
    }

    @GetMapping("/profile/verify-otp")
    public String verifyProfileOtpPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String verifyEmail = getSessionValue(session, PROFILE_UPDATE_VERIFY_EMAIL);
        if (verifyEmail == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không có yêu cầu cập nhật thông tin cần xác thực.");
            return "redirect:/profile";
        }
        model.addAttribute("maskedEmail", maskEmail(verifyEmail));
        return "profile-verify-otp";
    }

    @PostMapping("/profile/verify-otp")
    public String verifyProfileOtp(@RequestParam String otp,
                                   HttpSession session,
                                   Authentication authentication,
                                   HttpServletRequest request,
                                   HttpServletResponse response,
                                   RedirectAttributes redirectAttributes) {
        String verifyEmail = getSessionValue(session, PROFILE_UPDATE_VERIFY_EMAIL);
        Long userId = getSessionLong(session, PROFILE_UPDATE_PENDING_USER_ID);

        if (verifyEmail == null || userId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phiên xác thực đã hết hạn. Vui lòng thao tác lại.");
            return "redirect:/profile";
        }

        boolean valid = otpService.verifyOtp(verifyEmail, otp);
        if (!valid) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã OTP không hợp lệ hoặc đã hết hạn.");
            return "redirect:/profile/verify-otp";
        }

        try {
            AppUser updated = appUserService.updateProfileWithValidation(
                    userId,
                    getSessionValue(session, PROFILE_UPDATE_PENDING_FULL_NAME),
                    getSessionValue(session, PROFILE_UPDATE_PENDING_EMAIL),
                    getSessionValue(session, PROFILE_UPDATE_PENDING_PHONE)
            );

            refreshAuthentication(authentication, updated, request, response);
            clearPendingProfileUpdateSession(session);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thành công.");
            return "redirect:/profile";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/profile";
        }
    }

    @PostMapping("/profile/verify-otp/resend")
    public String resendProfileOtp(HttpSession session, RedirectAttributes redirectAttributes) {
        String verifyEmail = getSessionValue(session, PROFILE_UPDATE_VERIFY_EMAIL);
        if (verifyEmail == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không có yêu cầu cập nhật thông tin cần xác thực.");
            return "redirect:/profile";
        }

        try {
            otpService.generateAndSendOtp(verifyEmail);
            redirectAttributes.addFlashAttribute("infoMessage", "Đã gửi lại mã OTP xác thực thay đổi thông tin.");
        } catch (MailException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể gửi lại OTP. Vui lòng thử lại sau.");
        }
        return "redirect:/profile/verify-otp";
    }

    private CustomerProfileDto toProfileDto(AppUser user) {
        CustomerProfileDto dto = new CustomerProfileDto();
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        return dto;
    }

    private void refreshAuthentication(Authentication currentAuthentication,
                                       AppUser updatedUser,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        if (currentAuthentication == null) {
            return;
        }

        String loginId = updatedUser.getPhone() != null && !updatedUser.getPhone().isBlank()
                ? updatedUser.getPhone()
                : updatedUser.getEmail();

        UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                loginId,
                currentAuthentication.getCredentials(),
                updatedUser.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.name())).toList()
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(newAuth);
        SecurityContextHolder.setContext(context);
        SECURITY_CONTEXT_REPOSITORY.saveContext(context, request, response);
    }

    private String getSessionValue(HttpSession session, String key) {
        Object value = session.getAttribute(key);
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return text.isBlank() ? null : text;
    }

    private Long getSessionLong(HttpSession session, String key) {
        Object value = session.getAttribute(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        String prefix = email.substring(0, Math.min(2, atIndex));
        return prefix + "***" + email.substring(atIndex);
    }

    private void clearPendingGoogleSession(HttpSession session) {
        session.removeAttribute(SET_PASSWORD_PENDING_EMAIL);
        session.removeAttribute(SET_PASSWORD_PENDING_NAME);
        session.removeAttribute(SET_PASSWORD_PENDING_GOOGLE_ID);
    }

    private void clearPendingProfileUpdateSession(HttpSession session) {
        session.removeAttribute(PROFILE_UPDATE_PENDING_USER_ID);
        session.removeAttribute(PROFILE_UPDATE_PENDING_FULL_NAME);
        session.removeAttribute(PROFILE_UPDATE_PENDING_EMAIL);
        session.removeAttribute(PROFILE_UPDATE_PENDING_PHONE);
        session.removeAttribute(PROFILE_UPDATE_VERIFY_EMAIL);
    }

    private boolean isForgotPasswordOtpVerified(HttpSession session) {
        Object verified = session.getAttribute(FORGOT_PASSWORD_OTP_VERIFIED);
        return verified instanceof Boolean flag && flag;
    }

    private void clearForgotPasswordSession(HttpSession session) {
        session.removeAttribute(FORGOT_PASSWORD_EMAIL);
        session.removeAttribute(FORGOT_PASSWORD_OTP_VERIFIED);
    }

    private boolean safeEquals(String left, String right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right);
    }
}
