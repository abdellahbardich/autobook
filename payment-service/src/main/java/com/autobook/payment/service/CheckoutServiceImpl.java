package com.autobook.payment.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autobook.payment.client.LemonSqueezyClient;
import com.autobook.payment.dto.CheckoutDto;
import com.autobook.payment.exception.PaymentException;
import com.autobook.payment.exception.ResourceNotFoundException;
import com.autobook.payment.model.Checkout;
import com.autobook.payment.repository.CheckoutRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutServiceImpl implements CheckoutService {

    private final CheckoutRepository checkoutRepository;
    private final LemonSqueezyClient lemonSqueezyClient;

    @Override
    @Transactional
    public CheckoutDto createCheckout(Long userId, String productId, String variantId, Map<String, Object> customData) {
        Map<String, Object> response = lemonSqueezyClient.createCheckout(productId, variantId, customData);

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        Map<String, Object> attributes = (Map<String, Object>) data.get("attributes");

        String checkoutId = data.get("id").toString();
        String checkoutUrl = attributes.get("url").toString();
        String expiresAt = attributes.get("expires_at") != null ? attributes.get("expires_at").toString() : null;

        Checkout checkout = Checkout.builder()
                .userId(userId)
                .checkoutId(checkoutId)
                .productId(productId)
                .variantId(variantId)
                .status("PENDING")
                .checkoutUrl(checkoutUrl)
                .expiresAt(expiresAt)
                .build();

        Checkout savedCheckout = checkoutRepository.save(checkout);
        return mapToCheckoutDto(savedCheckout);
    }

    @Override
    public CheckoutDto getCheckoutById(Long id) {
        Checkout checkout = checkoutRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checkout not found with id: " + id));
        return mapToCheckoutDto(checkout);
    }

    @Override
    public CheckoutDto getCheckoutByCheckoutId(String checkoutId) {
        Checkout checkout = checkoutRepository.findByCheckoutId(checkoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkout not found with checkoutId: " + checkoutId));
        return mapToCheckoutDto(checkout);
    }

    @Override
    public List<CheckoutDto> getCheckoutsByUserId(Long userId) {
        return checkoutRepository.findByUserId(userId).stream()
                .map(this::mapToCheckoutDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CheckoutDto> getCheckoutsByUserIdAndStatus(Long userId, String status) {
        return checkoutRepository.findByUserIdAndStatus(userId, status).stream()
                .map(this::mapToCheckoutDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CheckoutDto updateCheckoutStatus(String checkoutId, String status) {
        Checkout checkout = checkoutRepository.findByCheckoutId(checkoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkout not found with checkoutId: " + checkoutId));

        checkout.setStatus(status);
        Checkout updatedCheckout = checkoutRepository.save(checkout);
        return mapToCheckoutDto(updatedCheckout);
    }

    private CheckoutDto mapToCheckoutDto(Checkout checkout) {
        return CheckoutDto.builder()
                .id(checkout.getId())
                .userId(checkout.getUserId())
                .checkoutId(checkout.getCheckoutId())
                .productId(checkout.getProductId())
                .variantId(checkout.getVariantId())
                .status(checkout.getStatus())
                .checkoutUrl(checkout.getCheckoutUrl())
                .expiresAt(checkout.getExpiresAt())
                .createdAt(checkout.getCreatedAt())
                .updatedAt(checkout.getUpdatedAt())
                .build();
    }
}