package com.autobook.payment.service;

import java.util.List;

import com.autobook.payment.dto.SubscriptionDto;

public interface SubscriptionService {
    SubscriptionDto getSubscriptionById(Long id);
    SubscriptionDto getSubscriptionBySubscriptionId(String subscriptionId);
    List<SubscriptionDto> getSubscriptionsByUserId(Long userId);
    List<SubscriptionDto> getActiveSubscriptionsByUserId(Long userId);
    SubscriptionDto getLatestActiveSubscriptionByUserId(Long userId);
    SubscriptionDto cancelSubscription(String subscriptionId);
    SubscriptionDto updateSubscriptionStatus(String subscriptionId, String status);
    boolean hasActiveSubscription(Long userId);
}