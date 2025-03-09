package com.autobook.orchestration.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.autobook.orchestration.dto.GenerationProgressDto;
import com.autobook.orchestration.service.WorkflowService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/generation")
@RequiredArgsConstructor
public class GenerationController {

    private final WorkflowService workflowService;

    @GetMapping("/progress/{bookId}")
    public ResponseEntity<GenerationProgressDto> getGenerationProgress(@PathVariable Long bookId) {
        GenerationProgressDto progress = workflowService.getBookGenerationProgress(bookId);
        return ResponseEntity.ok(progress);
    }

    @PostMapping("/restart/{bookId}")
    public ResponseEntity<Void> restartFailedGeneration(@PathVariable Long bookId) {
        workflowService.restartFailedWorkflow(bookId);
        return ResponseEntity.ok().build();
    }
}