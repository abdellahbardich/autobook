package com.autobook.payment.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.autobook.payment.dto.WebhookEventDto;
import com.autobook.payment.model.Payment;
import com.autobook.payment.repository.PaymentRepository;
import com.autobook.payment.repository.SubscriptionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookServiceImpl implements WebhookService {

    private final SubscriptionServiceImpl subscriptionService;
    private final CheckoutService checkoutService;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Value("${lemonsqueezy.webhook-secret}")
    private String webhookSecret;

    @Override
    public void processWebhookEvent(WebhookEventDto webhookEvent) {
        String eventName = webhookEvent.getEventName();
        Map<String, Object> data = webhookEvent.getData();

        log.info("Processing webhook event: {}", eventName);

        switch (eventName) {
            case "subscription_created":
                handleSubscriptionCreated(data);
                break;
            case "subscription_updated":
                handleSubscriptionUpdated(data);
                break;
            case "subscription_cancelled":
                handleSubscriptionCancelled(data);
                break;
            case "subscription_resumed":
                handleSubscriptionResumed(data);
                break;
            case "subscription_expired":
                handleSubscriptionExpired(data);
                break;
            case "order_created":
                handleOrderCreated(data);
                break;
            case "order_refunded":
                handleOrderRefunded(data);
                break;
            default:
                log.warn("Unhandled webhook event: {}", eventName);
        }
    }

    @Override
    public boolean validateWebhookSignature(String requestBody, String signature) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secretKey);

            String calculatedSignature = bytesToHex(
                    sha256_HMAC.doFinal(requestBody.getBytes(StandardCharsets.UTF_8)));

            return calculatedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Error validating webhook signature: {}", e.getMessage());
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexStringBuilder = new StringBuilder();
        for (byte b : bytes) {
            hexStringBuilder.append(String.format("%02x", b));
        }
        return hexStringBuilder.toString();
    }

    private void handleSubscriptionCreated(Map<String, Object> data) {
        try {
            subscriptionService.createOrUpdateSubscription(data);

            createPaymentFromSubscription(data, "SUBSCRIPTION");

            updateCheckoutStatus(data, "COMPLETED");
        } catch (Exception e) {
            log.error("Error handling subscription_created: {}", e.getMessage());
        }
    }

    private void handleSubscriptionUpdated(Map<String, Object> data) {
        try {
            subscriptionService.createOrUpdateSubscription(data);
        } catch (Exception e) {
            log.error("Error handling subscription_updated: {}", e.getMessage());
        }
    }

    private void handleSubscriptionCancelled(Map<String, Object> data) {
        try {
            Map<String, Object> attributes = (Map<String, Object>) ((Map<String, Object>) data.get("data")).get("attributes");
            String subscriptionId = (String) ((Map<String, Object>) data.get("data")).get("id");

            subscriptionService.updateSubscriptionStatus(subscriptionId, "CANCELLED");
        } catch (Exception e) {
            log.error("Error handling subscription_cancelled: {}", e.getMessage());
        }
    }

    private void handleSubscriptionResumed(Map<String, Object> data) {
        try {
            Map<String, Object> attributes = (Map<String, Object>) ((Map<String, Object>) data.get("data")).get("attributes");
            String subscriptionId = (String) ((Map<String, Object>) data.get("data")).get("id");

            subscriptionService.updateSubscriptionStatus(subscriptionId, "ACTIVE");
        } catch (Exception e) {
            log.error("Error handling subscription_resumed: {}", e.getMessage());
        }
    }

    private void handleSubscriptionExpired(Map<String, Object> data) {
        try {
            Map<String, Object> attributes = (Map<String, Object>) ((Map<String, Object>) data.get("data")).get("attributes");
            String subscriptionId = (String) ((Map<String, Object>) data.get("data")).get("id");

            subscriptionService.updateSubscriptionStatus(subscriptionId, "EXPIRED");
        } catch (Exception e) {
            log.error("Error handling subscription_expired: {}", e.getMessage());
        }
    }

    private void handleOrderCreated(Map<String, Object> data) {
        try {
            createPaymentFromOrder(data);

            updateCheckoutStatus(data, "COMPLETED");
        } catch (Exception e) {
            log.error("Error handling order_created: {}", e.getMessage());
        }
    }

    private void handleOrderRefunded(Map<String, Object> data) {
        try {
            Map<String, Object> attributes = (Map<String, Object>) ((Map<String, Object>) data.get("data")).get("attributes");
            String orderId = (String) attributes.get("order_id");

            Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElse(null);

            if (payment != null) {
                payment.setStatus("REFUNDED");
                paymentRepository.save(payment);
            }
        } catch (Exception e) {
            log.error("Error handling order_refunded: {}", e.getMessage());
        }
    }

    private void createPaymentFromSubscription(Map<String, Object> data, String paymentType) {
        try {
            Map<String, Object> attributes = (Map<String, Object>) ((Map<String, Object>) data.get("data")).get("attributes");
            String subscriptionId = (String) ((Map<String, Object>) data.get("data")).get("id");

            Map<String, Object> customData = (Map<String, Object>) attributes.get("custom_data");
            Long userId = Long.valueOf(customData.get("userId").toString());

            BigDecimal amount = new BigDecimal(attributes.get("recurring_price").toString());
            String orderId = (String) attributes.get("order_id");

            Payment payment = Payment.builder()
                    .userId(userId)
                    .paymentId(subscriptionId + "-initial")
                    .paymentType(paymentType)
                    .amount(amount)
                    .currency("USD")
                    .status("COMPLETED")
                    .paymentMethod("LEMON_SQUEEZY")
                    .orderId(orderId)
                    .subscriptionId(subscriptionId)
                    .createdAt(LocalDateTime.now())
                    .build();

            paymentRepository.save(payment);
        } catch (Exception e) {
            log.error("Error creating payment from subscription: {}", e.getMessage());
        }
    }

    private void createPaymentFromOrder(Map<String, Object> data) {
        try {
            Map<String, Object> attributes = (Map<String, Object>) ((Map<String, Object>) data.get("data")).get("attributes");
            String orderId = (String) ((Map<String, Object>) data.get("data")).get("id");

            Map<String, Object> customData = (Map<String, Object>) attributes.get("custom_data");
            Long userId = Long.valueOf(customData.get("userId").toString());

            BigDecimal amount = new BigDecimal(attributes.get("total").toString());

            Payment payment = Payment.builder()
                    .userId(userId)
                    .paymentId(orderId)
                    .paymentType("ONE_TIME")
                    .amount(amount)
                    .currency("USD")
                    .status("COMPLETED")
                    .paymentMethod("LEMON_SQUEEZY")
                    .orderId(orderId)
                    .createdAt(LocalDateTime.now())
                    .build();

            paymentRepository.save(payment);
        } catch (Exception e) {
            log.error("Error creating payment from order: {}", e.getMessage());
        }
    }

    private void updateCheckoutStatus(Map<String, Object> data, String status) {
        try {
            Map<String, Object> attributes = (Map<String, Object>) ((Map<String, Object>) data.get("data")).get("attributes");
            String checkoutId = (String) attributes.get("checkout_id");

            if (checkoutId != null) {
                checkoutService.updateCheckoutStatus(checkoutId, status);
            }
        } catch (Exception e) {
            log.error("Error updating checkout status: {}", e.getMessage());
        }
    }
}