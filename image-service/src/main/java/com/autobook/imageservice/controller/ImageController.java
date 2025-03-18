package com.autobook.imageservice.controller;

import com.autobook.imageservice.service.CoverImageService;
import com.autobook.imageservice.service.IllustrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
public class ImageController {

    private final CoverImageService coverImageService;
    private final IllustrationService illustrationService;

    @PostMapping("/cover")
    public ResponseEntity<Map<String, String>> generateCoverImage(@RequestBody Map<String, String> request) {
        String title = request.get("title");
        String style = request.get("style");

        log.info("Generating cover image for title: {}, style: {}", title, style);

        Map<String, String> response = coverImageService.generateCoverImage(title, style);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/illustrations")
    public ResponseEntity<List<Map<String, String>>> generateIllustrations(@RequestBody Map<String, Object> request) {
        log.info("Generating illustrations for prompt: {}", request.get("prompt"));

        List<Map<String, String>> response = illustrationService.generateIllustrations(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/file/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        Resource resource = new FileSystemResource(Paths.get(filename));

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String contentType;
        if (filename.endsWith(".png")) {
            contentType = MediaType.IMAGE_PNG_VALUE;
        } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            contentType = MediaType.IMAGE_JPEG_VALUE;
        } else {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}