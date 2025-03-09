package com.autobook.orchestration.service;

import com.autobook.orchestration.dto.BookCreationDto;
import com.autobook.orchestration.dto.GenerationProgressDto;

public interface WorkflowService {
    void startBookGenerationWorkflow(Long bookId, BookCreationDto bookCreationDto);
    GenerationProgressDto getBookGenerationProgress(Long bookId);
    void restartFailedWorkflow(Long bookId);
}