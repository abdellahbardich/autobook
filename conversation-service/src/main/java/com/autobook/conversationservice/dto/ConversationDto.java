package com.autobook.conversationservice.dto;

import com.autobook.conversationservice.entity.Message;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class ConversationDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateConversationRequest {
        private String title;

        @NotBlank(message = "Initial message content is required")
        private String initialMessage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationResponse {
        private Long conversationId;
        private String title;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationDetailResponse {
        private Long conversationId;
        private String title;
        private List<MessageResponse> messages;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageRequest {
        @NotBlank(message = "Message content is required")
        private String content;

        @NotNull(message = "Message role is required")
        private Message.MessageRole role;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageResponse {
        private Long messageId;
        private String content;
        private Message.MessageRole role;
        private LocalDateTime createdAt;
    }
}