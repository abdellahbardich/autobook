package com.autobook.bookgenerationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "image-service")
public interface ImageServiceClient {

    @PostMapping("/api/images/cover")
    Map<String, String> generateCoverImage(@RequestBody Map<String, String> request);

    @PostMapping("/api/images/illustrations")
    List<Map<String, String>> generateIllustrations(@RequestBody Map<String, Object> request);
}
