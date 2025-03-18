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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
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

    public String searchAndDownloadImage(String query, String style) {
        try {
            String searchUrl = apiUrl + "/search?q=" + query;
            if (style != null && !style.isEmpty()) {
                searchUrl += "&style=" + style;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<SearchResponse> response = restTemplate.exchange(
                    searchUrl, HttpMethod.GET, entity, SearchResponse.class);

            if (response.getBody() != null && !response.getBody().getData().isEmpty()) {
                Image image = response.getBody().getData().get(0);

                HttpEntity<Resource> downloadEntity = new HttpEntity<>(headers);
                ResponseEntity<Resource> imageResponse = restTemplate.exchange(
                        image.getDownloadUrl(), HttpMethod.GET, downloadEntity, Resource.class);

                if (imageResponse.getBody() != null) {
                    // Ensure directory exists
                    Path uploadPath = Paths.get(uploadDir);
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }

                    String filename = "cover_" + UUID.randomUUID() + ".jpg";
                    Path filePath = uploadPath.resolve(filename);
                    Files.copy(imageResponse.getBody().getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                    log.info("Downloaded image: {}", filePath);
                    return filePath.toString();
                }
            }

            log.warn("No images found for query: {}", query);
            return null;
        } catch (IOException e) {
            log.error("Error downloading image from Freepik", e);
            return null;
        }
    }

    @Data
    public static class SearchResponse {
        @JsonProperty("data")
        private List<Image> data;
    }

    @Data
    public static class Image {
        @JsonProperty("id")
        private String id;

        @JsonProperty("title")
        private String title;

        @JsonProperty("preview_url")
        private String previewUrl;

        @JsonProperty("download_url")
        private String downloadUrl;
    }
}