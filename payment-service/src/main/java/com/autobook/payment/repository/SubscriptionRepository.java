package com.autobook.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.autobook.payment.model.Subscription;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findBySubscriptionId(String subscriptionId);
    List<Subscription> findByUserId(Long userId);
    List<Subscription> findByUserIdAndStatus(Long userId, String status);
    Optional<Subscription> findTopByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
}