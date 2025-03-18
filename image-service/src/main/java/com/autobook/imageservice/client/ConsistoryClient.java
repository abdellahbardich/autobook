package com.autobook.imageservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConsistoryClient {

    private final RestTemplate restTemplate;

    @Value("${consistory.api.key}")
    private String apiKey;

    @Value("${consistory.api.url}")
    private String apiUrl;

    @Value("${image.upload.dir}")
    private String uploadDir;

    public List<String> generateConsistentImages(String subjectPrompt, List<String> subjectTokens,
                                                 String stylePrompt, String scenePrompt1, String scenePrompt2) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Accept", "application/json");
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> payload = new HashMap<>();
            payload.put("mode", "init");
            payload.put("subject_prompt", subjectPrompt);
            payload.put("subject_tokens", subjectTokens);
            payload.put("subject_seed", new Random().nextInt(1000));
            payload.put("style_prompt", stylePrompt);
            payload.put("scene_prompt1", scenePrompt1);
            payload.put("scene_prompt2", scenePrompt2 != null ? scenePrompt2 : "");
            payload.put("negative_prompt", "");
            payload.put("cfg_scale", 5);
            payload.put("same_initial_noise", false);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

            List<String> imagePaths = new ArrayList<>();
            if (response.getBody() != null) {
                List<Map<String, Object>> artifacts = (List<Map<String, Object>>) response.getBody().get("artifacts");

                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                for (Map<String, Object> artifact : artifacts) {
                    String base64Image = (String) artifact.get("base64");
                    byte[] imageBytes = Base64.getDecoder().decode(base64Image);

                    String filename = "illustration_" + UUID.randomUUID() + ".png";
                    Path filePath = uploadPath.resolve(filename);
                    Files.write(filePath, imageBytes);

                    log.info("Saved generated image: {}", filePath);
                    imagePaths.add(filePath.toString());
                }
            }

            return imagePaths;
        } catch (Exception e) {
            log.error("Error generating images with Consistory API", e);
            return Collections.emptyList();
        }
    }
}