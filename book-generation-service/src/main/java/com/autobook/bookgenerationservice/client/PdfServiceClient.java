package com.autobook.bookgenerationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "pdf-service")
public interface PdfServiceClient {

    @PostMapping("/api/pdf/generate")
    Map<String, String> generatePdf(@RequestBody Map<String, Object> request);

    @PostMapping("/api/pdf/preview")
    Map<String, String> generatePreviewImage(@RequestParam("pdfPath") String pdfPath);
}
