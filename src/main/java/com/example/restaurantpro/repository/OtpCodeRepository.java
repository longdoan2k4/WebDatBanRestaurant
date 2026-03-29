package com.example.restaurantpro.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.restaurantpro.model.OtpCode;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);

    void deleteByExpiresAtBefore(LocalDateTime threshold);
}
