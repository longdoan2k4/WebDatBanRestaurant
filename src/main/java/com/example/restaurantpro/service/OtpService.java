package com.example.restaurantpro.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.restaurantpro.model.OtpCode;
import com.example.restaurantpro.repository.OtpCodeRepository;

@Service
public class OtpService {

    private final EmailService emailService;
    private final AppUserService appUserService;
    private final OtpCodeRepository otpCodeRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.otp.expiry-minutes:5}")
    private long expiryMinutes;

    public OtpService(EmailService emailService,
                      AppUserService appUserService,
                      OtpCodeRepository otpCodeRepository,
                      PasswordEncoder passwordEncoder) {
        this.emailService = emailService;
        this.appUserService = appUserService;
        this.otpCodeRepository = otpCodeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void generateAndSendOtp(String email) {
        otpCodeRepository.deleteByExpiresAtBefore(LocalDateTime.now());

        String otp = emailService.generateOtpCode();

        OtpCode code = new OtpCode();
        code.setEmail(email);
        code.setOtpHash(passwordEncoder.encode(otp));
        code.setExpiresAt(LocalDateTime.now().plusMinutes(expiryMinutes));
        code.setUsed(false);
        otpCodeRepository.save(code);

        String recipientName = appUserService.findByEmail(email)
            .map(user -> user.getFullName())
            .orElse("Quý khách");
        emailService.sendOtpEmail(email, recipientName, otp, expiryMinutes);
    }

    @Transactional
    public boolean verifyOtp(String email, String rawOtp) {
        return otpCodeRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email)
                .filter(code -> code.getExpiresAt().isAfter(LocalDateTime.now()))
                .filter(code -> passwordEncoder.matches(rawOtp, code.getOtpHash()))
                .map(code -> {
                    code.setUsed(true);
                    otpCodeRepository.save(code);
                    return true;
                })
                .orElse(false);
    }
}
