package com.autobook.imageservice.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class FreepikClient {

    private final RestTemplate restTemplate;

    @Value("${freepik.api.key}")
    private String apiKey;

    @Value("${freepik.api.url}")
    private String apiUrl;

    @Value("${image.upload.dir}")
    private String uploadDir;

    public String generateAndDownloadImage(String prompt, String style) {
        try {
            // Ensure directory exists
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // For now, let's create a local placeholder image since we can't properly handle webhooks
            // In a real implementation, you would:
            // 1. Call the Mystic API
            // 2. Poll for the result using the task ID
            // 3. Download the final image when ready

            log.info("Simulating image generation with prompt: {} and style: {}", prompt, style);

            // Create a placeholder image filename
            String filename = "cover_" + UUID.randomUUID() + ".jpg";
            Path filePath = uploadPath.resolve(filename);

            // For testing, download a placeholder image
            URL placeholderUrl = new URL("https://via.placeholder.com/800x600.jpg?text=" +
                    prompt.replace(" ", "+"));
            Files.copy(placeholderUrl.openStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Downloaded placeholder image to: {}", filePath);
            return filePath.toString();
        } catch (IOException e) {
            log.error("Error generating/downloading image from Freepik", e);
            return null;
        }
    }

    // This method would be used in a real implementation
    private String initiateImageGeneration(String prompt, String style) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-freepik-api-key", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestData = new HashMap<>();
            requestData.put("prompt", prompt);
            if (style != null && !style.isEmpty()) {
                requestData.put("prompt", prompt + " in " + style + " style");
            }
            requestData.put("resolution", "2k");
            requestData.put("aspect_ratio", "square_1_1");
            requestData.put("realism", true);
            requestData.put("filter_nsfw", true);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestData, headers);

            ResponseEntity<MysticResponse> response = restTemplate.exchange(
                    apiUrl + "/ai/mystic",
                    HttpMethod.POST,
                    requestEntity,
                    MysticResponse.class
            );

            if (response.getBody() != null) {
                return response.getBody().getData().getTaskId();
            }
            return null;
        } catch (Exception e) {
            log.error("Error initiating image generation with Freepik", e);
            return null;
        }
    }

    @Data
    static class MysticResponse {
        private MysticData data;
    }

    @Data
    static class MysticData {
        @JsonProperty("task_id")
        private String taskId;
        private String status;
    }
}