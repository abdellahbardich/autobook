package com.autobook.collectionservice.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.time.LocalDateTime;
import java.util.Map;

@FeignClient(name = "book-generation-service")
public interface BookServiceClient {

    @GetMapping("/api/books/{id}")
    BookSummary getBookDetails(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("id") Long bookId);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class BookSummary {
        private Long bookId;
        private String title;
        private String previewImageUrl;
        private LocalDateTime createdAt;
    }
}