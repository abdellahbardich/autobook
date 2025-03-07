package com.autobook.payment.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.autobook.payment.dto.SubscriptionDto;
import com.autobook.payment.service.SubscriptionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionDto> getSubscriptionById(@PathVariable Long id) {
        SubscriptionDto subscriptionDto = subscriptionService.getSubscriptionById(id);
        return ResponseEntity.ok(subscriptionDto);
    }

    @GetMapping("/external/{subscriptionId}")
    public ResponseEntity<SubscriptionDto> getSubscriptionBySubscriptionId(@PathVariable String subscriptionId) {
        SubscriptionDto subscriptionDto = subscriptionService.getSubscriptionBySubscriptionId(subscriptionId);
        return ResponseEntity.ok(subscriptionDto);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SubscriptionDto>> getSubscriptionsByUserId(@PathVariable Long userId) {
        List<SubscriptionDto> subscriptions = subscriptionService.getSubscriptionsByUserId(userId);
        return ResponseEntity.ok(subscriptions);
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<SubscriptionDto>> getActiveSubscriptionsByUserId(@PathVariable Long userId) {
        List<SubscriptionDto> subscriptions = subscriptionService.getActiveSubscriptionsByUserId(userId);
        return ResponseEntity.ok(subscriptions);
    }

    @GetMapping("/user/{userId}/active/latest")
    public ResponseEntity<SubscriptionDto> getLatestActiveSubscriptionByUserId(@PathVariable Long userId) {
        SubscriptionDto subscriptionDto = subscriptionService.getLatestActiveSubscriptionByUserId(userId);
        return ResponseEntity.ok(subscriptionDto);
    }

    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<SubscriptionDto> cancelSubscription(@PathVariable String subscriptionId) {
        SubscriptionDto subscriptionDto = subscriptionService.cancelSubscription(subscriptionId);
        return ResponseEntity.ok(subscriptionDto);
    }

    @GetMapping("/user/{userId}/has-active")
    public ResponseEntity<Boolean> hasActiveSubscription(@PathVariable Long userId) {
        boolean hasActive = subscriptionService.hasActiveSubscription(userId);
        return ResponseEntity.ok(hasActive);
    }
}