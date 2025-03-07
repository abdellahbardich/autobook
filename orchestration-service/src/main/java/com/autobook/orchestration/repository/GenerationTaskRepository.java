package com.autobook.orchestration.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.autobook.orchestration.model.GenerationTask;

@Repository
public interface GenerationTaskRepository extends JpaRepository<GenerationTask, Long> {
    List<GenerationTask> findByBookId(Long bookId);
    List<GenerationTask> findByUserIdAndStatus(Long userId, String status);
    Optional<GenerationTask> findByBookIdAndTaskType(Long bookId, String taskType);
}