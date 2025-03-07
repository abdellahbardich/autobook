package com.autobook.payment.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.autobook.payment.dto.CheckoutDto;
import com.autobook.payment.service.CheckoutService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/checkouts")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping
    public ResponseEntity<CheckoutDto> createCheckout(
            @RequestParam("userId") Long userId,
            @RequestParam("productId") String productId,
            @RequestParam(value = "variantId", required = false) String variantId,
            @RequestBody(required = false) Map<String, Object> customData) {

        CheckoutDto checkoutDto = checkoutService.createCheckout(userId, productId, variantId, customData);
        return new ResponseEntity<>(checkoutDto, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CheckoutDto> getCheckoutById(@PathVariable Long id) {
        CheckoutDto checkoutDto = checkoutService.getCheckoutById(id);
        return ResponseEntity.ok(checkoutDto);
    }

    @GetMapping("/external/{checkoutId}")
    public ResponseEntity<CheckoutDto> getCheckoutByCheckoutId(@PathVariable String checkoutId) {
        CheckoutDto checkoutDto = checkoutService.getCheckoutByCheckoutId(checkoutId);
        return ResponseEntity.ok(checkoutDto);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CheckoutDto>> getCheckoutsByUserId(@PathVariable Long userId) {
        List<CheckoutDto> checkouts = checkoutService.getCheckoutsByUserId(userId);
        return ResponseEntity.ok(checkouts);
    }

    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<List<CheckoutDto>> getCheckoutsByUserIdAndStatus(
            @PathVariable Long userId,
            @PathVariable String status) {
        List<CheckoutDto> checkouts = checkoutService.getCheckoutsByUserIdAndStatus(userId, status);
        return ResponseEntity.ok(checkouts);
    }
}