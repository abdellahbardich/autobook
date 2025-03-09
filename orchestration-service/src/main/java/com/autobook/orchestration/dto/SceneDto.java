package com.autobook.orchestration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SceneDto {
    private Integer number;
    private String narrative;
    private String sceneDescription;
    private String imageUrl;
    private String imageS3Key;
}