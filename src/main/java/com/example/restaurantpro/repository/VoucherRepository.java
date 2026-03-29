package com.example.restaurantpro.repository;

import com.example.restaurantpro.model.AppUser;
import com.example.restaurantpro.model.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    List<Voucher> findByUserAndIsUsedFalse(AppUser user);

    Optional<Voucher> findByCode(String code);

    Optional<Voucher> findByCodeAndUserAndIsUsedFalse(String code, AppUser user);
}
