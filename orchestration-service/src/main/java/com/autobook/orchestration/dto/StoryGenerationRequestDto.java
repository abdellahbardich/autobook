package com.autobook.orchestration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryGenerationRequestDto {

    @NotBlank
    private String summary;

    @NotNull
    private Integer numScenes;

    @NotNull
    private Long userId;

    @NotNull
    private Long bookId;
}