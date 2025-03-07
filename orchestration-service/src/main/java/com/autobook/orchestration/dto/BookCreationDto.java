package com.autobook.orchestration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookCreationDto {

    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 100, message = "Title must be between 2 and 100 characters")
    private String title;

    @NotBlank(message = "Summary is required")
    @Size(min = 10, max = 1000, message = "Summary must be between 10 and 1000 characters")
    private String summary;

    @NotNull(message = "Number of scenes is required")
    private Integer numScenes;

    @NotBlank(message = "Book type is required")
    private String bookType;

    private String stylePrompt;

    @NotNull(message = "User ID is required")
    private Long userId;
}