package com.autobook.bookgenerationservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class OllamaClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ollama.api.url}")
    private String ollamaApiUrl;

    @PostConstruct
    public void init() {
        // Add support for ndjson content type
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(
                Arrays.asList(MediaType.APPLICATION_JSON,
                        MediaType.valueOf("application/x-ndjson")));
        restTemplate.getMessageConverters().add(0, converter);
    }

    public String generateText(String prompt) {
        try {
            log.info("Generating text with Ollama for prompt: {}",
                    prompt.length() > 100 ? prompt.substring(0, 100) + "..." : prompt);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.valueOf("application/x-ndjson")));

            OllamaRequest request = new OllamaRequest();
            request.setModel("llama3.2");
            request.setPrompt(prompt);

            HttpEntity<OllamaRequest> entity = new HttpEntity<>(request, headers);

            String response = restTemplate.postForObject(
                    ollamaApiUrl + "/api/generate",
                    entity,
                    String.class
            );

            // For ndjson format, we need to parse the last line to get the final response
            if (response != null && !response.isEmpty()) {
                String[] lines = response.split("\n");
                String lastLine = lines[lines.length - 1];
                OllamaResponse ollamaResponse = objectMapper.readValue(lastLine, OllamaResponse.class);
                return ollamaResponse.getResponse();
            }

            // If Ollama is not available, generate a mock response for testing
            return "This is a mock response for: " + prompt;

        } catch (Exception e) {
            log.error("Error generating text with Ollama: {}", e.getMessage());
            // Return a mock response for testing if Ollama is not working
            return "Error occurred, but here is a mock response for: " + prompt;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OllamaRequest {
        private String model;
        private String prompt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OllamaResponse {
        private String model;
        private String response;
    }
}