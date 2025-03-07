package com.autobook.payment.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutDto {
    private Long id;
    private Long userId;
    private String checkoutId;
    private String productId;
    private String variantId;
    private String status;
    private String checkoutUrl;
    private String expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}