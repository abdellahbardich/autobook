package com.autobook.bookgenerationservice.dto;

import com.autobook.bookgenerationservice.entity.Book;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class BookDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookCreationRequest {
        @NotNull(message = "Conversation ID is required")
        private Long conversationId;

        private Long messageId;

        @NotBlank(message = "Prompt is required")
        private String prompt;

        @NotBlank(message = "Title is required")
        private String title;

        @NotNull(message = "Book type is required")
        private Book.BookType bookType;

        private String stylePrompt;

        @Min(value = 1, message = "Number of chapters must be at least 1")
        private Integer numChapters;

        private boolean includeIllustrations;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookResponse {
        private Long bookId;
        private String title;
        private Book.BookStatus status;
        private String previewImageUrl;
        private String downloadUrl;
        private String coverImageUrl;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookDetailResponse {
        private Long bookId;
        private String title;
        private String summary;
        private String style;
        private Book.BookType bookType;
        private Book.BookStatus status;
        private String previewImageUrl;
        private String downloadUrl;
        private String coverImageUrl;
        private List<ChapterResponse> chapters;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterResponse {
        private Long contentId;
        private Integer chapterNumber;
        private String chapterTitle;
        private String chapterContent;
        private String illustrationUrl;
    }
}