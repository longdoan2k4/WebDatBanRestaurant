package com.example.restaurantpro.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.restaurantpro.model.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByPhone(String phone);

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByEmailIgnoreCase(String email);

    Optional<AppUser> findByGoogleId(String googleId);
}
