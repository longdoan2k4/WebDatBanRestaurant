package com.example.restaurantpro.service;

import java.util.stream.Collectors;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.restaurantpro.model.AppUser;
import com.example.restaurantpro.repository.AppUserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public CustomUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        AppUser user = resolveUser(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + loginId));

        String username = user.getPhone() != null && !user.getPhone().isBlank() ? user.getPhone() : user.getEmail();
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Tài khoản chưa có định danh đăng nhập hợp lệ.");
        }

        if (user.isLocked()) {
            throw new org.springframework.security.authentication.LockedException("Tài khoản của bạn đã bị khóa do vi phạm chính sách!");
        }

        return User.withUsername(username)
                .password(user.getPassword())
                .disabled(!user.isEnabled())
                .authorities(user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.name()))
                        .collect(Collectors.toSet()))
                .build();
    }

    private java.util.Optional<AppUser> resolveUser(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            return java.util.Optional.empty();
        }
        if (loginId.contains("@")) {
            java.util.Optional<AppUser> byEmail = appUserRepository.findByEmail(loginId);
            if (byEmail.isPresent()) {
                return byEmail;
            }
        }
        return appUserRepository.findByPhone(loginId);
    }
}
