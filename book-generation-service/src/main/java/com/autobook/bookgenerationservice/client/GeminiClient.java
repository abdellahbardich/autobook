package com.autobook.bookgenerationservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    public String generateText(String prompt) {
        try {
            log.info("Generating text with Gemini for prompt: {}",
                    prompt.length() > 100 ? prompt.substring(0, 100) + "..." : prompt);

            // Build the URL with API key
            String url = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("key", apiKey)
                    .toUriString();

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Prepare request body
            Map<String, Object> contents = new HashMap<>();
            contents.put("role", "user");
            contents.put("parts", List.of(Map.of("text", prompt)));

            Map<String, Object> payload = new HashMap<>();
            payload.put("contents", List.of(contents));
            payload.put("generationConfig", Map.of(
                    "temperature", 0.7,
                    "topK", 40,
                    "topP", 0.95,
                    "maxOutputTokens", 2048
            ));

            // Create request entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

            // Make API call to Gemini
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            // Process response
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                JsonNode candidates = jsonResponse.path("candidates");
                if (!candidates.isEmpty()) {
                    JsonNode content = candidates.get(0).path("content");
                    if (!content.path("parts").isEmpty()) {
                        String generatedText = content.path("parts").get(0).path("text").asText();
                        return generatedText;
                    }
                }
                log.warn("Unexpected response structure: {}", response.getBody());
            }

            log.warn("Failed to generate text with Gemini: {}", response.getStatusCode());
            return "Failed to generate text with Gemini. Please try again later.";

        } catch (Exception e) {
            log.error("Error generating text with Gemini API", e);
            return "Error generating text: " + e.getMessage();
        }
    }
}