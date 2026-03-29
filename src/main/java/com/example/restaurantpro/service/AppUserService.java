package com.example.restaurantpro.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.restaurantpro.dto.RegisterRequest;
import com.example.restaurantpro.model.AppUser;
import com.example.restaurantpro.model.RoleName;
import com.example.restaurantpro.repository.AppUserRepository;
import com.example.restaurantpro.repository.BookingRepository;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepository appUserRepository,
                          BookingRepository bookingRepository,
                          PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUser registerCustomer(RegisterRequest request) {
        if (appUserRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new IllegalArgumentException("Số điện thoại này đã được đăng ký.");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
        }

        AppUser user = new AppUser();
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(new LinkedHashSet<>(Set.of(RoleName.ROLE_CUSTOMER)));

        return appUserRepository.save(user);
    }

    public AppUser createSeedUser(String fullName, String phone, String rawPassword, Set<RoleName> roles) {
        Optional<AppUser> existing = appUserRepository.findByPhone(phone);
        if (existing.isPresent()) {
            return existing.get();
        }

        AppUser user = new AppUser();
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRoles(new LinkedHashSet<>(roles));
        return appUserRepository.save(user);
    }

    public Optional<AppUser> findByPhone(String phone) {
        return appUserRepository.findByPhone(phone);
    }

    public Optional<AppUser> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return appUserRepository.findByEmailIgnoreCase(email.trim());
    }

    public Optional<AppUser> findByLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            return Optional.empty();
        }
        if (loginId.contains("@")) {
            Optional<AppUser> byEmail = appUserRepository.findByEmail(loginId);
            if (byEmail.isPresent()) {
                return byEmail;
            }
        }
        return appUserRepository.findByPhone(loginId);
    }

    public AppUser registerOrUpdateGoogleUser(String email, String fullName, String googleId) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        AppUser user = appUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> {
                    AppUser created = new AppUser();
                    created.setEmail(normalizedEmail);
                    created.setRoles(new LinkedHashSet<>(Set.of(RoleName.ROLE_CUSTOMER)));
                    created.setEnabled(true);
                    created.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    return created;
                });

        if (user.getFullName() == null || user.getFullName().isBlank()) {
            user.setFullName(fullName != null && !fullName.isBlank() ? fullName : "Google User");
        }
        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName);
        }
        user.setEmail(normalizedEmail);
        user.setGoogleId(googleId);
        user.setProvider("GOOGLE");
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(new LinkedHashSet<>(Set.of(RoleName.ROLE_CUSTOMER)));
        }

        return appUserRepository.save(user);
    }

    public AppUser createGoogleUserWithPassword(String email,
                                                String fullName,
                                                String googleId,
                                                String phone,
                                                String rawPassword,
                                                String confirmPassword) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("Email Google không hợp lệ.");
        }
        if (appUserRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email này đã tồn tại. Bạn có thể đăng nhập trực tiếp.");
        }
        String normalizedPhone = phone == null ? null : phone.trim();
        if (normalizedPhone == null || normalizedPhone.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập số điện thoại.");
        }
        if (appUserRepository.findByPhone(normalizedPhone).isPresent()) {
            throw new IllegalArgumentException("Số điện thoại này đã được sử dụng.");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu.");
        }
        if (!rawPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
        }

        AppUser user = new AppUser();
        user.setEmail(normalizedEmail);
        user.setPhone(normalizedPhone);
        user.setFullName((fullName == null || fullName.isBlank()) ? "Google User" : fullName.trim());
        user.setGoogleId(googleId);
        user.setProvider("GOOGLE");
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEnabled(true);
        user.setRoles(new LinkedHashSet<>(Set.of(RoleName.ROLE_CUSTOMER)));
        return appUserRepository.save(user);
    }

    public AppUser updatePhoneByLoginId(String loginId, String phone) {
        AppUser user = findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));

        String normalizedPhone = (phone == null) ? null : phone.trim();
        if (normalizedPhone != null && normalizedPhone.isBlank()) {
            normalizedPhone = null;
        }

        if (normalizedPhone != null) {
            Optional<AppUser> existing = appUserRepository.findByPhone(normalizedPhone);
            if (existing.isPresent() && !existing.get().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Số điện thoại đã được sử dụng bởi tài khoản khác.");
            }
        }

        user.setPhone(normalizedPhone);
        return appUserRepository.save(user);
    }

    public AppUser updateProfileWithValidation(Long userId,
                                               String fullName,
                                               String email,
                                               String phone) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));

        String normalizedFullName = fullName == null ? null : fullName.trim();
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        String normalizedPhone = phone == null ? null : phone.trim();

        if (normalizedFullName == null || normalizedFullName.isBlank()) {
            throw new IllegalArgumentException("Họ và tên không được để trống.");
        }
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("Email không được để trống.");
        }

        Optional<AppUser> byEmail = appUserRepository.findByEmailIgnoreCase(normalizedEmail);
        if (byEmail.isPresent() && !byEmail.get().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Email đã được sử dụng bởi tài khoản khác.");
        }

        if (normalizedPhone != null && normalizedPhone.isBlank()) {
            normalizedPhone = null;
        }
        if (normalizedPhone != null) {
            Optional<AppUser> byPhone = appUserRepository.findByPhone(normalizedPhone);
            if (byPhone.isPresent() && !byPhone.get().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Số điện thoại đã được sử dụng bởi tài khoản khác.");
            }
        }

        user.setFullName(normalizedFullName);
        user.setEmail(normalizedEmail);
        user.setPhone(normalizedPhone);
        return appUserRepository.save(user);
    }

    public void resetPasswordByEmail(String email, String rawPassword, String confirmPassword) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("Email không hợp lệ.");
        }

        AppUser user = appUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản theo email này."));

        validateNewPassword(rawPassword, confirmPassword);
        user.setPassword(passwordEncoder.encode(rawPassword));
        appUserRepository.save(user);
    }

    public void changePassword(Long userId,
                               String currentPassword,
                               String newPassword,
                               String confirmNewPassword) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));

        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu hiện tại.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
        }

        validateNewPassword(newPassword, confirmNewPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        appUserRepository.save(user);
    }

    private void validateNewPassword(String password, String confirmPassword) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu mới.");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
        }
    }

    public List<AppUser> findAllUsers() {
        return appUserRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    public long countCustomers() {
        return appUserRepository.findAll().stream()
                .filter(user -> user.getRoles().contains(RoleName.ROLE_CUSTOMER))
                .count();
    }

    public void grantRole(Long userId, RoleName roleName) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
        user.getRoles().add(roleName);
        appUserRepository.save(user);
    }

    public boolean toggleRole(Long userId, RoleName roleName) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));

        if (user.getRoles().contains(roleName)) {
            if (user.getRoles().size() <= 1) {
                throw new IllegalArgumentException("Tài khoản phải có ít nhất một quyền.");
            }
            user.getRoles().remove(roleName);
            appUserRepository.save(user);
            return false;
        }

        user.getRoles().add(roleName);
        appUserRepository.save(user);
        return true;
    }

    public void deleteUser(Long userId, String operatorLoginId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));

        if (operatorLoginId != null && user.matchesLoginId(operatorLoginId)) {
            throw new IllegalArgumentException("Không thể tự xóa tài khoản đang đăng nhập.");
        }

        if (bookingRepository.existsByCustomer_Id(userId)) {
            throw new IllegalArgumentException("Không thể xóa người dùng này vì đã có booking liên quan.");
        }

        appUserRepository.delete(user);
    }
}
