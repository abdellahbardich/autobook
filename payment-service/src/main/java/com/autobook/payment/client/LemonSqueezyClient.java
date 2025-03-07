package com.autobook.payment.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.autobook.payment.exception.PaymentException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class LemonSqueezyClient {

    private final WebClient webClient;
    private final String storeId;

    public LemonSqueezyClient(
            WebClient.Builder webClientBuilder,
            @Value("${lemonsqueezy.api-key}") String apiKey,
            @Value("${lemonsqueezy.base-url}") String baseUrl,
            @Value("${lemonsqueezy.store-id}") String storeId) {

        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        this.storeId = storeId;
    }

    public Map<String, Object> createCheckout(String productId, String variantId, Map<String, Object> customData) {
        Map<String, Object> requestBody = Map.of(
                "data", Map.of(
                        "type", "checkouts",
                        "attributes", Map.of(
                                "store_id", storeId,
                                "product_id", productId,
                                "variant_id", variantId,
                                "custom_data", customData
                        )
                )
        );

        return webClient.post()
                .uri("/checkouts")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(e -> {
                    log.error("Error creating checkout with Lemon Squeezy: {}", e.getMessage());
                    return Mono.error(new PaymentException("Failed to create checkout: " + e.getMessage()));
                })
                .block();
    }

    public Map<String, Object> getSubscription(String subscriptionId) {
        return webClient.get()
                .uri("/subscriptions/{subscriptionId}", subscriptionId)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(e -> {
                    log.error("Error fetching subscription from Lemon Squeezy: {}", e.getMessage());
                    return Mono.error(new PaymentException("Failed to get subscription: " + e.getMessage()));
                })
                .block();
    }

    public Map<String, Object> cancelSubscription(String subscriptionId) {
        return webClient.delete()
                .uri("/subscriptions/{subscriptionId}", subscriptionId)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(e -> {
                    log.error("Error cancelling subscription with Lemon Squeezy: {}", e.getMessage());
                    return Mono.error(new PaymentException("Failed to cancel subscription: " + e.getMessage()));
                })
                .block();
    }

    public Map<String, Object> getCheckout(String checkoutId) {
        return webClient.get()
                .uri("/checkouts/{checkoutId}", checkoutId)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(e -> {
                    log.error("Error fetching checkout from Lemon Squeezy: {}", e.getMessage());
                    return Mono.error(new PaymentException("Failed to get checkout: " + e.getMessage()));
                })
                .block();
    }
}