package com.autobook.collectionservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class CollectionDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCollectionRequest {
        @NotBlank(message = "Collection name is required")
        private String name;

        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateCollectionRequest {
        private String name;

        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectionResponse {
        private Long collectionId;
        private String name;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectionDetailResponse {
        private Long collectionId;
        private String name;
        private String description;
        private List<BookSummary> books;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookSummary {
        private Long bookId;
        private String title;
        private String previewImageUrl;
        private LocalDateTime addedAt;
    }
}