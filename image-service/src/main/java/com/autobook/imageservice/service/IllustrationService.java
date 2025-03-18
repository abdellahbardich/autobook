package com.autobook.imageservice.service;

import com.autobook.imageservice.client.ConsistoryClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class IllustrationService {

    private final ConsistoryClient consistoryClient;

    public List<Map<String, String>> generateIllustrations(Map<String, Object> request) {
        try {
            String prompt = (String) request.get("prompt");
            String style = (String) request.get("style");

            if (prompt == null || prompt.isEmpty()) {
                throw new IllegalArgumentException("Prompt is required for illustration generation");
            }

            // Generate illustrations using Consistory API
            List<Map<String, String>> illustrations = consistoryClient.generateIllustrations(prompt, style);

            if (illustrations.isEmpty()) {
                log.warn("No illustrations were generated for prompt: {}", prompt);
            }

            return illustrations;
        } catch (Exception e) {
            log.error("Error generating illustrations", e);
            throw new RuntimeException("Failed to generate illustrations", e);
        }
    }
}