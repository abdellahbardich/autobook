package com.autobook.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.autobook.payment.dto.WebhookEventDto;
import com.autobook.payment.service.WebhookService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/lemonsqueezy")
    public ResponseEntity<String> handleLemonSqueezyWebhook(
            @RequestBody String requestBody,
            @RequestBody WebhookEventDto webhookEvent,
            @RequestHeader("X-Signature") String signature) {

        log.info("Received webhook event: {}", webhookEvent.getEventName());

        if (!webhookService.validateWebhookSignature(requestBody, signature)) {
            log.error("Invalid webhook signature");
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        webhookService.processWebhookEvent(webhookEvent);
        return ResponseEntity.ok("Webhook processed successfully");
    }
}