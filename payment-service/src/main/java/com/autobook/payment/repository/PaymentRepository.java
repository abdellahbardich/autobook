package com.autobook.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.autobook.payment.model.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentId(String paymentId);
    List<Payment> findByUserId(Long userId);
    List<Payment> findByUserIdAndPaymentType(Long userId, String paymentType);
    Optional<Payment> findByOrderId(String orderId);
    Optional<Payment> findBySubscriptionId(String subscriptionId);
}