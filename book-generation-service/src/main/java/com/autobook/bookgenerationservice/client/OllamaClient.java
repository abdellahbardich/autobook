
package com.autobook.bookgenerationservice.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class OllamaClient {

    private final RestTemplate restTemplate;

    @Value("${ollama.api.url}")
    private String ollamaApiUrl;

    public String generateText(String prompt) {
        OllamaRequest request = new OllamaRequest();
        request.setModel("llama3.2");
        request.setPrompt(prompt);

        OllamaResponse response = restTemplate.postForObject(
                ollamaApiUrl + "/api/generate",
                request,
                OllamaResponse.class
        );

        return response != null ? response.getResponse() : "";
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