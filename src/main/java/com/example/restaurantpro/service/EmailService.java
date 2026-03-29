package com.example.restaurantpro.service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.example.restaurantpro.model.Booking;
import com.example.restaurantpro.model.PaymentMethod;
import com.example.restaurantpro.repository.BookingRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final BookingRepository bookingRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.mail.from-address:${spring.mail.username:}}")
    private String fromAddress;

    @Value("${app.mail.from-name:Restaurant Pro}")
    private String fromName;

    public EmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine,
            BookingRepository bookingRepository) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.bookingRepository = bookingRepository;
    }

    public String generateOtpCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    public void sendOtpEmail(String toEmail, String recipientName, String otpCode, long expiryMinutes) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(toEmail);
            helper.setSubject("Mã xác thực đăng nhập nhà hàng Rivière");

            Context context = new Context(LocaleContextHolder.getLocale());
            context.setVariable("recipientName", recipientName == null || recipientName.isBlank() ? "Quý khách" : recipientName);
            context.setVariable("otpCode", otpCode);
            context.setVariable("expiryMinutes", expiryMinutes);
            String htmlContent = templateEngine.process("mail/otp-email", context);
            helper.setText(htmlContent, true);

            if (fromAddress != null && !fromAddress.isBlank()) {
                helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            }

            mailSender.send(message);
        } catch (MailException ex) {
            throw ex;
        } catch (MessagingException | UnsupportedEncodingException ex) {
            throw new MailSendException("Không thể gửi email OTP.", ex);
        }
    }

    /**
     * Gửi email xác nhận đặt bàn cho khách hàng.
     * Chạy bất đồng bộ (@Async) để không làm chậm response thanh toán.
     */
    @Async
    public void sendBookingConfirmation(String toEmail, Booking booking) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("[EmailService] Bỏ qua gửi email xác nhận: địa chỉ email trống (bookingId={}).", booking.getId());
            return;
        }
        // Re-fetch booking với JOIN FETCH items để tránh LazyInitializationException
        // trong thread @Async (Hibernate Session đã đóng lúc này)
        Long bookingId = booking.getId();
        Booking freshBooking = bookingRepository.findByIdWithItems(bookingId)
                .orElse(booking); // fallback an toàn nếu không tìm thấy
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(toEmail);
            helper.setSubject("✅ Xác nhận đặt bàn #" + freshBooking.getId() + " | Rivière Restaurant");

            String paymentLabel;
            if (freshBooking.getPaymentMethod() == PaymentMethod.VNPAY) {
                paymentLabel = "Đã thanh toán qua VNPAY";
            } else {
                paymentLabel = "Thanh toán tại nhà hàng";
            }

            Context context = new Context(LocaleContextHolder.getLocale());
            context.setVariable("booking", freshBooking);
            context.setVariable("paymentLabel", paymentLabel);

            String htmlContent = templateEngine.process("mail/booking-confirmation-email", context);
            helper.setText(htmlContent, true);

            if (fromAddress != null && !fromAddress.isBlank()) {
                helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            }

            mailSender.send(message);
            log.info("[EmailService] Đã gửi email xác nhận đặt bàn #{} tới {}.", freshBooking.getId(), toEmail);
        } catch (Exception ex) {
            log.warn("[EmailService] Không thể gửi email xác nhận đặt bàn #{}: {}", bookingId, ex.getMessage());
        }
    }
}