package com.autobook.payment.service;

import java.util.List;
import java.util.Map;

import com.autobook.payment.dto.CheckoutDto;

public interface CheckoutService {
    CheckoutDto createCheckout(Long userId, String productId, String variantId, Map<String, Object> customData);
    CheckoutDto getCheckoutById(Long id);
    CheckoutDto getCheckoutByCheckoutId(String checkoutId);
    List<CheckoutDto> getCheckoutsByUserId(Long userId);
    List<CheckoutDto> getCheckoutsByUserIdAndStatus(Long userId, String status);
    CheckoutDto updateCheckoutStatus(String checkoutId, String status);
}