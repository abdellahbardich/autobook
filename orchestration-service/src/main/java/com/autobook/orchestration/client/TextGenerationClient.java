package com.autobook.orchestration.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.autobook.orchestration.dto.StoryGenerationRequestDto;
import com.autobook.orchestration.exception.ServiceCommunicationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TextGenerationClient {

    private final RestTemplate restTemplate;

    @Value("${service.text-generation.url}")
    private String textGenerationServiceUrl;

    public Object generateStory(StoryGenerationRequestDto request) {
        try {
            ResponseEntity<Object> response = restTemplate.postForEntity(
                    textGenerationServiceUrl + "/generate-story",
                    request,
                    Object.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new ServiceCommunicationException("Text generation service returned: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error communicating with text generation service: {}", e.getMessage());
            throw new ServiceCommunicationException("Failed to communicate with text generation service", e);
        }
    }
}