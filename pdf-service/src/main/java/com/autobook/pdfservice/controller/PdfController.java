package com.autobook.pdfservice.controller;

import com.autobook.pdfservice.service.PdfGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
@Slf4j
public class PdfController {

    private final PdfGeneratorService pdfGeneratorService;

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generatePdf(@RequestBody Map<String, Object> request) {
        log.info("Generating PDF for book ID: {}", request.get("bookId"));

        Map<String, String> response = pdfGeneratorService.generatePdf(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/preview")
    public ResponseEntity<Map<String, String>> generatePreviewImage(@RequestParam("pdfPath") String pdfPath) {
        log.info("Generating preview image for PDF: {}", pdfPath);

        Map<String, String> response = pdfGeneratorService.generatePreviewImage(pdfPath);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<Resource> downloadPdf(@PathVariable String filename) {
        Resource resource = new FileSystemResource(Paths.get(filename));

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +
                        new File(filename).getName() + "\"")
                .body(resource);
    }

    @GetMapping("/preview/{filename:.+}")
    public ResponseEntity<Resource> getPreviewImage(@PathVariable String filename) {
        Resource resource = new FileSystemResource(Paths.get(filename));

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(resource);
    }
}