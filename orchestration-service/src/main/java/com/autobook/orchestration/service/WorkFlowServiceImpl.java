package com.autobook.orchestration.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autobook.orchestration.client.ImageGenerationClient;
import com.autobook.orchestration.client.PdfAssemblyClient;
import com.autobook.orchestration.client.StorageClient;
import com.autobook.orchestration.client.TextGenerationClient;
import com.autobook.orchestration.dto.BookCreationDto;
import com.autobook.orchestration.dto.GenerationProgressDto;
import com.autobook.orchestration.dto.GenerationProgressDto.StepProgressDto;
import com.autobook.orchestration.dto.ImageGenerationRequestDto;
import com.autobook.orchestration.dto.PdfGenerationRequestDto;
import com.autobook.orchestration.dto.SceneDto;
import com.autobook.orchestration.dto.StoryGenerationRequestDto;
import com.autobook.orchestration.exception.ResourceNotFoundException;
import com.autobook.orchestration.exception.WorkflowException;
import com.autobook.orchestration.model.Book;
import com.autobook.orchestration.model.GenerationTask;
import com.autobook.orchestration.model.WorkflowStep;
import com.autobook.orchestration.repository.BookRepository;
import com.autobook.orchestration.repository.GenerationTaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowServiceImpl implements WorkflowService {

    private final BookRepository bookRepository;
    private final GenerationTaskRepository generationTaskRepository;
    private final BookService bookService;
    private final TextGenerationClient textGenerationClient;
    private final ImageGenerationClient imageGenerationClient;
    private final PdfAssemblyClient pdfAssemblyClient;
    private final StorageClient storageClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Async("taskExecutor")
    public void startBookGenerationWorkflow(Long bookId, BookCreationDto bookCreationDto) {
        try {
            bookService.updateBookStatus(bookId, "GENERATING");

            createGenerationTasks(bookId, bookCreationDto.getUserId());

            sendProgressUpdate(bookId);

            Map<String, Object> storyResult = generateStoryContent(bookId, bookCreationDto);

            List<Map<String, Object>> scenes = (List<Map<String, Object>>) storyResult.get("scenes");
            Map<String, Object> character = (Map<String, Object>) storyResult.get("main_character");

            Map<String, Object> imageResult = null;
            if (!bookCreationDto.getBookType().equals("TEXT_ONLY")) {
                imageResult = generateImages(
                        bookId,
                        bookCreationDto.getUserId(),
                        (String) character.get("description"),
                        (List<String>) character.get("tokens"),
                        scenes,
                        bookCreationDto.getStylePrompt());
            }

            generatePdf(
                    bookId,
                    bookCreationDto.getUserId(),
                    bookCreationDto.getTitle(),
                    (String) character.get("description"),
                    scenes,
                    imageResult,
                    bookCreationDto.getBookType());

            bookService.updateBookStatus(bookId, "COMPLETE");

            sendProgressUpdate(bookId);

        } catch (Exception e) {
            log.error("Error in book generation workflow for bookId {}: {}", bookId, e.getMessage());

            bookService.updateBookStatus(bookId, "FAILED");

            updateFailedTask(bookId, e.getMessage());

            sendProgressUpdate(bookId);
        }
    }

    private void createGenerationTasks(Long bookId, Long userId) {
        GenerationTask storyTask = GenerationTask.builder()
                .bookId(bookId)
                .userId(userId)
                .taskType("STORY")
                .status("PENDING")
                .progress(0)
                .build();
        generationTaskRepository.save(storyTask);

        GenerationTask imageTask = GenerationTask.builder()
                .bookId(bookId)
                .userId(userId)
                .taskType("IMAGE")
                .status("PENDING")
                .progress(0)
                .build();
        generationTaskRepository.save(imageTask);

        GenerationTask pdfTask = GenerationTask.builder()
                .bookId(bookId)
                .userId(userId)
                .taskType("PDF")
                .status("PENDING")
                .progress(0)
                .build();
        generationTaskRepository.save(pdfTask);
    }

    private Map<String, Object> generateStoryContent(Long bookId, BookCreationDto bookCreation) {
        try {
            updateTaskStatus(bookId, "STORY", "PROCESSING", 10);

            // Create story generation request
            StoryGenerationRequestDto request = StoryGenerationRequestDto.builder()
                    .summary(bookCreation.getSummary())
                    .numScenes(bookCreation.getNumScenes())
                    .userId(bookCreation.getUserId())
                    .bookId(bookId)
                    .build();

            Object response = textGenerationClient.generateStory(request);
            updateTaskStatus(bookId, "STORY", "PROCESSING", 50);

            Map<String, Object> result = objectMapper.convertValue(response, Map.class);

            updateTaskStatus(bookId, "STORY", "COMPLETED", 100, objectMapper.writeValueAsString(result));

            return result;
        } catch (Exception e) {
            log.error("Error generating story content for bookId {}: {}", bookId, e.getMessage());
            updateTaskStatus(bookId, "STORY", "FAILED", 0, null, e.getMessage());
            throw new WorkflowException("Story generation failed", e);
        }
    }

    private Map<String, Object> generateImages(
            Long bookId,
            Long userId,
            String characterDescription,
            List<String> characterTokens,
            List<Map<String, Object>> scenes,
            String stylePrompt) {

        try {
            updateTaskStatus(bookId, "IMAGE", "PROCESSING", 10);

            List<SceneDto> sceneDtos = scenes.stream()
                    .map(scene -> {
                        return SceneDto.builder()
                                .number((Integer) scene.get("number"))
                                .narrative((String) scene.get("narrative"))
                                .sceneDescription((String) scene.get("scene_description"))
                                .build();
                    })
                    .collect(Collectors.toList());

            // Create image generation request
            ImageGenerationRequestDto request = ImageGenerationRequestDto.builder()
                    .subjectPrompt(characterDescription)
                    .subjectTokens(characterTokens)
                    .scenes(sceneDtos)
                    .stylePrompt(stylePrompt != null ? stylePrompt : "A detailed digital artwork")
                    .userId(userId)
                    .bookId(bookId)
                    .build();

            Object response = imageGenerationClient.generateImages(request);
            updateTaskStatus(bookId, "IMAGE", "PROCESSING", 50);

            Map<String, Object> result = objectMapper.convertValue(response, Map.class);

            updateTaskStatus(bookId, "IMAGE", "COMPLETED", 100, objectMapper.writeValueAsString(result));

            return result;
        } catch (Exception e) {
            log.error("Error generating images for bookId {}: {}", bookId, e.getMessage());
            updateTaskStatus(bookId, "IMAGE", "FAILED", 0, null, e.getMessage());
            throw new WorkflowException("Image generation failed", e);
        }
    }

    private void generatePdf(
            Long bookId,
            Long userId,
            String title,
            String characterDescription,
            List<Map<String, Object>> scenes,
            Map<String, Object> imageResult,
            String bookType) {

        try {
            updateTaskStatus(bookId, "PDF", "PROCESSING", 10);

            List<SceneDto> mergedScenes = new ArrayList<>();

            for (Map<String, Object> scene : scenes) {
                SceneDto sceneDto = SceneDto.builder()
                        .number((Integer) scene.get("number"))
                        .narrative((String) scene.get("narrative"))
                        .sceneDescription((String) scene.get("scene_description"))
                        .build();

                // Add image URL if available
                if (imageResult != null) {
                    List<Map<String, Object>> images = (List<Map<String, Object>>) imageResult.get("images");
                    for (Map<String, Object> image : images) {
                        if ((Integer) image.get("scene_number") == sceneDto.getNumber()) {
                            sceneDto.setImageUrl((String) image.get("image_url"));
                            sceneDto.setImageS3Key((String) image.get("s3_key"));
                            break;
                        }
                    }
                }

                mergedScenes.add(sceneDto);
            }

            PdfGenerationRequestDto request = PdfGenerationRequestDto.builder()
                    .title(title)
                    .mainCharacterDesc(characterDescription)
                    .scenes(mergedScenes)
                    .templateType(bookType)
                    .userId(userId)
                    .bookId(bookId)
                    .build();

            Object response = pdfAssemblyClient.generatePdf(request);
            updateTaskStatus(bookId, "PDF", "PROCESSING", 50);

            Map<String, Object> result = objectMapper.convertValue(response, Map.class);

            String pdfS3Key = (String) result.get("s3_key");
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));
            book.setPdfS3Key(pdfS3Key);

            if (imageResult != null) {
                List<Map<String, Object>> images = (List<Map<String, Object>>) imageResult.get("images");
                if (!images.isEmpty()) {
                    book.setCoverImageS3Key((String) images.get(0).get("s3_key"));
                }
            }

            bookRepository.save(book);

            // Update task status to completed
            updateTaskStatus(bookId, "PDF", "COMPLETED", 100, objectMapper.writeValueAsString(result));

        } catch (Exception e) {
            log.error("Error generating PDF for bookId {}: {}", bookId, e.getMessage());
            updateTaskStatus(bookId, "PDF", "FAILED", 0, null, e.getMessage());
            throw new WorkflowException("PDF generation failed", e);
        }
    }

    @Transactional
    private void updateTaskStatus(Long bookId, String taskType, String status, Integer progress) {
        updateTaskStatus(bookId, taskType, status, progress, null, null);
    }

    @Transactional
    private void updateTaskStatus(Long bookId, String taskType, String status, Integer progress, String result) {
        updateTaskStatus(bookId, taskType, status, progress, result, null);
    }

    @Transactional
    private void updateTaskStatus(Long bookId, String taskType, String status, Integer progress, String result, String errorMessage) {
        GenerationTask task = generationTaskRepository.findByBookIdAndTaskType(bookId, taskType)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found for bookId: " + bookId + " and type: " + taskType));

        task.setStatus(status);
        task.setProgress(progress);

        if (result != null) {
            task.setResult(result);
        }

        if (errorMessage != null) {
            task.setErrorMessage(errorMessage);
        }

        if (status.equals("PROCESSING") && task.getStartedAt() == null) {
            task.setStartedAt(LocalDateTime.now());
        }

        if (status.equals("COMPLETED") || status.equals("FAILED")) {
            task.setCompletedAt(LocalDateTime.now());
        }

        generationTaskRepository.save(task);

        sendProgressUpdate(bookId);
    }

    private void updateFailedTask(Long bookId, String errorMessage) {
        List<GenerationTask> tasks = generationTaskRepository.findByBookId(bookId);

        for (GenerationTask task : tasks) {
            if (task.getStatus().equals("PROCESSING") || task.getStatus().equals("PENDING")) {
                task.setStatus("FAILED");
                task.setErrorMessage(errorMessage);
                task.setCompletedAt(LocalDateTime.now());
                generationTaskRepository.save(task);
            }
        }
    }

    private void sendProgressUpdate(Long bookId) {
        GenerationProgressDto progress = getBookGenerationProgress(bookId);
        messagingTemplate.convertAndSend("/topic/progress/" + bookId, progress);
    }

    @Override
    public GenerationProgressDto getBookGenerationProgress(Long bookId) {
        List<GenerationTask> tasks = generationTaskRepository.findByBookId(bookId);

        int totalProgress = tasks.stream().mapToInt(task -> task.getProgress()).sum();
        int overallProgress = totalProgress / tasks.size();

        String overallStatus = "PENDING";
        if (tasks.stream().allMatch(task -> task.getStatus().equals("COMPLETED"))) {
            overallStatus = "COMPLETED";
        } else if (tasks.stream().anyMatch(task -> task.getStatus().equals("FAILED"))) {
            overallStatus = "FAILED";
        } else if (tasks.stream().anyMatch(task -> task.getStatus().equals("PROCESSING"))) {
            overallStatus = "PROCESSING";
        }

        List<StepProgressDto> steps = tasks.stream()
                .map(task -> {
                    return StepProgressDto.builder()
                            .stepName(task.getTaskType())
                            .status(task.getStatus())
                            .progress(task.getProgress())
                            .message(task.getErrorMessage())
                            .build();
                })
                .collect(Collectors.toList());

        return GenerationProgressDto.builder()
                .bookId(bookId)
                .overallStatus(overallStatus)
                .overallProgress(overallProgress)
                .steps(steps)
                .build();
    }

    @Override
    @Async("taskExecutor")
    public void restartFailedWorkflow(Long bookId) {
        try {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

            List<GenerationTask> tasks = generationTaskRepository.findByBookId(bookId);
            for (GenerationTask task : tasks) {
                task.setStatus("PENDING");
                task.setProgress(0);
                task.setErrorMessage(null);
                task.setStartedAt(null);
                task.setCompletedAt(null);
                generationTaskRepository.save(task);
            }

            bookService.updateBookStatus(bookId, "GENERATING");

            sendProgressUpdate(bookId);

            BookCreationDto bookCreationDto = BookCreationDto.builder()
                    .title(book.getTitle())
                    .summary(book.getDescription())
                    .numScenes(5) // Default to 5 scenes if restarting
                    .bookType(book.getType())
                    .userId(book.getUserId())
                    .build();

            startBookGenerationWorkflow(bookId, bookCreationDto);

        } catch (Exception e) {
            log.error("Error restarting workflow for bookId {}: {}", bookId, e.getMessage());
            throw new WorkflowException("Failed to restart workflow", e);
        }
    }
}