package com.autobook.payment.service;

import com.autobook.payment.dto.WebhookEventDto;

public interface WebhookService {
    void processWebhookEvent(WebhookEventDto webhookEvent);
    boolean validateWebhookSignature(String requestBody, String signature);
}