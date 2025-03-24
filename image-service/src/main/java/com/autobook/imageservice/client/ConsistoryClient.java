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


    public String generateCoverImage(String title, String style) {
        try {
            log.info("Generating cover image for title: {}, style: {}", title, style);

            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            List<String> titleWords = Arrays.asList(title.toLowerCase().split("\\s+"));
            List<String> subjectTokens = new ArrayList<>();
            for (String word : titleWords) {
                if (word.length() > 3 && !isCommonWord(word)) {
                    subjectTokens.add(word);
                }
                if (subjectTokens.size() >= 3) break;
            }

            if (subjectTokens.isEmpty()) {
                subjectTokens.add("book");
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("mode", "init");
            payload.put("subject_prompt", "Book cover for: " + title);
            payload.put("subject_tokens", subjectTokens);
            payload.put("subject_seed", new Random().nextInt(1000));
            payload.put("style_prompt", style != null && !style.isEmpty() ? style : "Professional book cover design");
            payload.put("scene_prompt1", "Book cover with title: " + title);
            payload.put("scene_prompt2", "Professional book cover design for: " + title);
            payload.put("negative_prompt", "ugly, blurry, text, words, low quality, distorted");
            payload.put("cfg_scale", 7);
            payload.put("same_initial_noise", false);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

            log.info("Consistory API response status: {}", response.getStatusCode());

            if (response.getBody() != null && response.getBody().containsKey("artifacts")) {
                List<Map<String, Object>> artifacts = (List<Map<String, Object>>) response.getBody().get("artifacts");
                if (!artifacts.isEmpty() && artifacts.get(0).containsKey("base64")) {
                    String base64Image = (String) artifacts.get(0).get("base64");
                    String filename = "cover_" + UUID.randomUUID() + ".png";
                    Path filePath = uploadPath.resolve(filename);

                    byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                    Files.write(filePath, imageBytes);

                    log.info("Cover image saved to: {}", filePath);
                    return filePath.toString();
                }
            }

            log.error("Failed to generate cover image - no artifacts in response");
            return null;

        } catch (Exception e) {
            log.error("Error generating cover image with Consistory API", e);
            return null;
        }
    }


    public List<Map<String, String>> generateIllustrations(String prompt, String style) {
        try {
            log.info("Generating illustration for prompt: {}", prompt);

            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String subject = extractSubject(prompt);
            List<String> subjectTokens = extractSubjectTokens(subject);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            Map<String, Object> payload = new HashMap<>();
            payload.put("mode", "init");
            payload.put("subject_prompt", subject);
            payload.put("subject_tokens", subjectTokens);
            payload.put("subject_seed", new Random().nextInt(1000));
            payload.put("style_prompt", style != null && !style.isEmpty() ? style : "Detailed illustration");
            payload.put("scene_prompt1", prompt);
            payload.put("scene_prompt2", prompt + (style != null ? " in " + style + " style" : ""));
            payload.put("negative_prompt", "ugly, blurry, low quality, distorted");
            payload.put("cfg_scale", 7);
            payload.put("same_initial_noise", false);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

            List<Map<String, String>> results = new ArrayList<>();

            if (response.getBody() != null && response.getBody().containsKey("artifacts")) {
                List<Map<String, Object>> artifacts = (List<Map<String, Object>>) response.getBody().get("artifacts");

                for (Map<String, Object> artifact : artifacts) {
                    if (artifact.containsKey("base64")) {
                        String base64Image = (String) artifact.get("base64");
                        String filename = "illustration_" + UUID.randomUUID() + ".png";
                        Path filePath = uploadPath.resolve(filename);

                        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                        Files.write(filePath, imageBytes);

                        log.info("Illustration saved to: {}", filePath);
                        results.add(Map.of("illustrationPath", filePath.toString()));
                    }
                }
            }

            return results;

        } catch (Exception e) {
            log.error("Error generating illustrations with Consistory API", e);
            return Collections.emptyList();
        }
    }


    private String extractSubject(String prompt) {
        String[] sentences = prompt.split("[,.]");
        return sentences.length > 0 ? sentences[0].trim() : prompt.trim();
    }


    private List<String> extractSubjectTokens(String subject) {
        List<String> tokens = new ArrayList<>();
        String[] words = subject.toLowerCase().split("\\s+");

        for (String word : words) {
            if (word.length() > 3 && !isCommonWord(word)) {
                tokens.add(word);
                if (tokens.size() >= 5) break;
            }
        }

        if (tokens.isEmpty()) {
            tokens.add("object");
        }

        return tokens;
    }


    private boolean isCommonWord(String word) {
        Set<String> commonWords = Set.of(
                "the", "and", "that", "with", "for", "this", "from", "into", "than",
                "your", "what", "have", "will", "they", "there"
        );
        return commonWords.contains(word.toLowerCase());
    }
}