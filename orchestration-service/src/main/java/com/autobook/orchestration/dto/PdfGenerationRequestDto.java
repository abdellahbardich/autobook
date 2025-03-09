package com.autobook.orchestration.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfGenerationRequestDto {

    @NotBlank
    private String title;

    private String mainCharacterDesc;

    @NotEmpty
    private List<SceneDto> scenes;

    @NotBlank
    private String templateType;

    @NotNull
    private Long userId;

    @NotNull
    private Long bookId;
}