package com.autobook.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.autobook.payment.model.Checkout;

@Repository
public interface CheckoutRepository extends JpaRepository<Checkout, Long> {
    Optional<Checkout> findByCheckoutId(String checkoutId);
    List<Checkout> findByUserId(Long userId);
    List<Checkout> findByUserIdAndStatus(Long userId, String status);
}