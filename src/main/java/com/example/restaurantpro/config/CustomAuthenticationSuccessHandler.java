package com.example.restaurantpro.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

import com.example.restaurantpro.model.AppUser;
import com.example.restaurantpro.service.AppUserService;
import com.example.restaurantpro.service.OtpService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationSuccessHandler.class);

    private static final String OTP_PENDING_EMAIL = "OTP_PENDING_EMAIL";
    private static final String OTP_PENDING_NAME = "OTP_PENDING_NAME";
    private static final String SET_PASSWORD_PENDING_EMAIL = "SET_PASSWORD_PENDING_EMAIL";
    private static final String SET_PASSWORD_PENDING_NAME = "SET_PASSWORD_PENDING_NAME";
    private static final String SET_PASSWORD_PENDING_GOOGLE_ID = "SET_PASSWORD_PENDING_GOOGLE_ID";
    private static final String GOOGLE_OTP_ERROR_DETAIL = "GOOGLE_OTP_ERROR_DETAIL";

    private final AppUserService appUserService;
    private final OtpService otpService;

    public CustomAuthenticationSuccessHandler(AppUserService appUserService, OtpService otpService) {
        this.appUserService = appUserService;
        this.otpService = otpService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            String fullName = oauth2User.getAttribute("name");
            String googleId = oauth2User.getAttribute("sub");
            if (email != null && !email.isBlank()) {
                String normalizedEmail = email.trim().toLowerCase();
                HttpSession session = request.getSession(true);
                AppUser existingUser = appUserService.findByEmail(normalizedEmail).orElse(null);
                if (existingUser == null) {
                    session.setAttribute(SET_PASSWORD_PENDING_EMAIL, normalizedEmail);
                    session.setAttribute(SET_PASSWORD_PENDING_NAME, fullName);
                    session.setAttribute(SET_PASSWORD_PENDING_GOOGLE_ID, googleId);
                } else {
                    appUserService.registerOrUpdateGoogleUser(normalizedEmail, fullName, googleId);
                }

                try {
                    otpService.generateAndSendOtp(normalizedEmail);
                    session.setAttribute(OTP_PENDING_EMAIL, normalizedEmail);
                    session.setAttribute(OTP_PENDING_NAME, fullName);

                    logoutKeepSession(request, response, authentication);
                    response.sendRedirect("/otp/verify");
                } catch (MailException ex) {
                    log.error("Google login succeeded but OTP email sending failed for {}", email, ex);
                    session.setAttribute(GOOGLE_OTP_ERROR_DETAIL, rootCauseMessage(ex));
                    logoutKeepSession(request, response, authentication);
                    response.sendRedirect("/login?googleOtpError=true");
                } catch (RuntimeException ex) {
                    log.error("Google login flow failed unexpectedly for {}", email, ex);
                    session.setAttribute(GOOGLE_OTP_ERROR_DETAIL, rootCauseMessage(ex));
                    logoutKeepSession(request, response, authentication);
                    response.sendRedirect("/login?googleOtpError=true");
                }
                return;
            }

            logoutKeepSession(request, response, authentication);
            response.sendRedirect("/login?googleEmailMissing=true");
            return;
        }

        boolean adminSide = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")
                                || authority.getAuthority().equals("ROLE_TABLE_MANAGER")
                                || authority.getAuthority().equals("ROLE_MENU_MANAGER"));

        if (adminSide) {
            response.sendRedirect("/admin/dashboard");
            return;
        }

        response.sendRedirect("/booking/start");
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return current.getClass().getSimpleName();
        }
        return message;
    }

    private void logoutKeepSession(HttpServletRequest request,
                                   HttpServletResponse response,
                                   Authentication authentication) {
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.setInvalidateHttpSession(false);
        logoutHandler.logout(request, response, authentication);
    }
}
