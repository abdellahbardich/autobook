package com.autobook.orchestration.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.autobook.orchestration.dto.ImageGenerationRequestDto;
import com.autobook.orchestration.exception.ServiceCommunicationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImageGenerationClient {

    private final RestTemplate restTemplate;

    @Value("${service.image-generation.url}")
    private String imageGenerationServiceUrl;

    public Object generateImages(ImageGenerationRequestDto request) {
        try {
            ResponseEntity<Object> response = restTemplate.postForEntity(
                    imageGenerationServiceUrl + "/generate-images",
                    request,
                    Object.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new ServiceCommunicationException("Image generation service returned: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error communicating with image generation service: {}", e.getMessage());
            throw new ServiceCommunicationException("Failed to communicate with image generation service", e);
        }
    }
}