package com.autobook.storage.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookFileDto {
    private Long id;
    private String title;
    private String s3Key;
    private String fileType;
    private Long size;
    private String description;
    private String status;
    private Long userId;
    private Long bookId;
    private String downloadUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}