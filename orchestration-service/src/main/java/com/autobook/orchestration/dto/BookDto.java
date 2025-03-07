package com.autobook.orchestration.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDto {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String status;
    private String type;
    private String coverImageUrl;
    private String pdfUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}