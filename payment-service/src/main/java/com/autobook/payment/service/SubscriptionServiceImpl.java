package com.autobook.payment.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autobook.payment.client.LemonSqueezyClient;
import com.autobook.payment.dto.SubscriptionDto;
import com.autobook.payment.exception.ResourceNotFoundException;
import com.autobook.payment.model.Subscription;
import com.autobook.payment.repository.SubscriptionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final LemonSqueezyClient lemonSqueezyClient;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    @Override
    public SubscriptionDto getSubscriptionById(Long id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + id));
        return mapToSubscriptionDto(subscription);
    }

    @Override
    public SubscriptionDto getSubscriptionBySubscriptionId(String subscriptionId) {
        Subscription subscription = subscriptionRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + subscriptionId));
        return mapToSubscriptionDto(subscription);
    }

    @Override
    public List<SubscriptionDto> getSubscriptionsByUserId(Long userId) {
        return subscriptionRepository.findByUserId(userId).stream()
                .map(this::mapToSubscriptionDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<SubscriptionDto> getActiveSubscriptionsByUserId(Long userId) {
        return subscriptionRepository.findByUserIdAndStatus(userId, "ACTIVE").stream()
                .map(this::mapToSubscriptionDto)
                .collect(Collectors.toList());
    }

    @Override
    public SubscriptionDto getLatestActiveSubscriptionByUserId(Long userId) {
        Subscription subscription = subscriptionRepository
                .findTopByUserIdAndStatusOrderByCreatedAtDesc(userId, "ACTIVE")
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription found for userId: " + userId));
        return mapToSubscriptionDto(subscription);
    }

    @Override
    @Transactional
    public SubscriptionDto cancelSubscription(String subscriptionId) {
        Subscription subscription = subscriptionRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + subscriptionId));

        lemonSqueezyClient.cancelSubscription(subscriptionId);

        subscription.setStatus("CANCELLED");
        subscription.setCanceledAt(LocalDateTime.now());

        Subscription updatedSubscription = subscriptionRepository.save(subscription);
        return mapToSubscriptionDto(updatedSubscription);
    }

    @Override
    @Transactional
    public SubscriptionDto updateSubscriptionStatus(String subscriptionId, String status) {
        Subscription subscription = subscriptionRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + subscriptionId));

        subscription.setStatus(status);
        if ("CANCELLED".equals(status)) {
            subscription.setCanceledAt(LocalDateTime.now());
        }

        Subscription updatedSubscription = subscriptionRepository.save(subscription);
        return mapToSubscriptionDto(updatedSubscription);
    }

    @Override
    public boolean hasActiveSubscription(Long userId) {
        return !subscriptionRepository.findByUserIdAndStatus(userId, "ACTIVE").isEmpty();
    }

    public Subscription createOrUpdateSubscription(Map<String, Object> webhookData) {
        Map<String, Object> subscriptionData = extractSubscriptionData(webhookData);

        String subscriptionId = (String) subscriptionData.get("id");
        String status = (String) subscriptionData.get("status");
        String planId = (String) subscriptionData.get("plan_id");
        String planName = (String) subscriptionData.get("plan_name");
        BigDecimal recurringPrice = new BigDecimal(subscriptionData.get("recurring_price").toString());
        String billingInterval = (String) subscriptionData.get("billing_interval");

        String startDateStr = (String) subscriptionData.get("current_period_start");
        String endDateStr = (String) subscriptionData.get("current_period_end");
        String canceledDateStr = (String) subscriptionData.get("canceled_at");

        LocalDateTime startDate = startDateStr != null ? LocalDateTime.parse(startDateStr, DATE_FORMATTER) : null;
        LocalDateTime endDate = endDateStr != null ? LocalDateTime.parse(endDateStr, DATE_FORMATTER) : null;
        LocalDateTime canceledDate = canceledDateStr != null ? LocalDateTime.parse(canceledDateStr, DATE_FORMATTER) : null;

        Map<String, Object> customData = (Map<String, Object>) subscriptionData.get("custom_data");
        Long userId = Long.valueOf(customData.get("userId").toString());

        Subscription subscription = subscriptionRepository.findBySubscriptionId(subscriptionId)
                .orElse(new Subscription());

        subscription.setSubscriptionId(subscriptionId);
        subscription.setUserId(userId);
        subscription.setStatus(status);
        subscription.setPlanId(planId);
        subscription.setPlanName(planName);
        subscription.setRecurringPrice(recurringPrice);
        subscription.setBillingInterval(billingInterval);
        subscription.setCurrentPeriodStart(startDate);
        subscription.setCurrentPeriodEnd(endDate);
        subscription.setCanceledAt(canceledDate);

        return subscriptionRepository.save(subscription);
    }

    private Map<String, Object> extractSubscriptionData(Map<String, Object> webhookData) {
        Map<String, Object> data = (Map<String, Object>) webhookData.get("data");
        return (Map<String, Object>) data.get("attributes");
    }

    private SubscriptionDto mapToSubscriptionDto(Subscription subscription) {
        return SubscriptionDto.builder()
                .id(subscription.getId())
                .userId(subscription.getUserId())
                .subscriptionId(subscription.getSubscriptionId())
                .status(subscription.getStatus())
                .planId(subscription.getPlanId())
                .planName(subscription.getPlanName())
                .recurringPrice(subscription.getRecurringPrice())
                .billingInterval(subscription.getBillingInterval())
                .currentPeriodStart(subscription.getCurrentPeriodStart())
                .currentPeriodEnd(subscription.getCurrentPeriodEnd())
                .canceledAt(subscription.getCanceledAt())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }
}