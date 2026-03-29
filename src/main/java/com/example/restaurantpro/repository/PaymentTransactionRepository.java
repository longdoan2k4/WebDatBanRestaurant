package com.example.restaurantpro.repository;

import com.example.restaurantpro.model.PaymentTransaction;
import com.example.restaurantpro.model.PaymentTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTxnRef(String txnRef);

    List<PaymentTransaction> findByBookingIdOrderByCreatedAtDesc(Long bookingId);

    Optional<PaymentTransaction> findFirstByBookingIdAndTypeOrderByCreatedAtDesc(Long bookingId, PaymentTransactionType type);

    boolean existsByOriginalTxnRefAndType(String originalTxnRef, PaymentTransactionType type);
}
