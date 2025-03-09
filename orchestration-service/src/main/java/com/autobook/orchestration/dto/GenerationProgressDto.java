package com.autobook.orchestration.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationProgressDto {
    private Long bookId;
    private String overallStatus;
    private Integer overallProgress;
    private List<StepProgressDto> steps;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepProgressDto {
        private String stepName;
        private String status;
        private Integer progress;
        private String message;
    }
}