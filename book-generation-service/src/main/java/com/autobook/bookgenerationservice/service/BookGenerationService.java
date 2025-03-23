package com.autobook.bookgenerationservice.service;

import com.autobook.bookgenerationservice.client.GeminiClient;
import com.autobook.bookgenerationservice.client.ImageServiceClient;
import com.autobook.bookgenerationservice.client.PdfServiceClient;
import com.autobook.bookgenerationservice.dto.BookDto;
import com.autobook.bookgenerationservice.entity.Book;
import com.autobook.bookgenerationservice.entity.BookContent;
import com.autobook.bookgenerationservice.repository.BookContentRepository;
import com.autobook.bookgenerationservice.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookGenerationService {

    private final BookRepository bookRepository;
    private final BookContentRepository bookContentRepository;
    private final GeminiClient geminiClient;
    private final ImageServiceClient imageServiceClient;
    private final PdfServiceClient pdfServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String UPLOADS_DIR = "uploads/images/";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 5000; // 5 seconds

    @Transactional
    public BookDto.BookResponse createBook(BookDto.BookCreationRequest request) {
        // Create book in database
        Book book = new Book();
        book.setConversationId(request.getConversationId());
        book.setMessageId(request.getMessageId());
        book.setTitle(request.getTitle());
        book.setBookType(request.getBookType());
        book.setStyle(request.getStylePrompt());
        book.setStatus(Book.BookStatus.PROCESSING);
        Book savedBook = bookRepository.save(book);

        // Send message to Kafka to trigger processing
        log.info("Sending book generation request to Kafka for book ID: {}", savedBook.getBookId());
        kafkaTemplate.send("book-generation-requests", savedBook.getBookId().toString(), request);

        // Return response
        return new BookDto.BookResponse(
                savedBook.getBookId(),
                savedBook.getTitle(),
                savedBook.getStatus(),
                savedBook.getPreviewImagePath() != null ? "/api/books/" + savedBook.getBookId() + "/preview" : null,
                savedBook.getPdfPath() != null ? "/api/books/" + savedBook.getBookId() + "/download" : null,
                savedBook.getCoverImagePath() != null ? "/api/books/" + savedBook.getBookId() + "/cover" : null,
                savedBook.getCreatedAt()
        );
    }

    @KafkaListener(topics = "book-generation-requests", groupId = "${spring.kafka.consumer.group-id}")
    public void processBookGenerationRequest(BookDto.BookCreationRequest request) {
        Book book = null;
        try {
            log.info("Received book generation request for title: {}", request.getTitle());

            // Find existing book with the same conversation ID and title
            List<Book> existingBooks = bookRepository.findByConversationId(request.getConversationId());
            Optional<Book> existingBook = existingBooks.stream()
                    .filter(b -> b.getTitle().equals(request.getTitle()))
                    .findFirst();

            if (existingBook.isPresent()) {
                // Update existing book
                book = existingBook.get();
                log.info("Found existing book with ID: {}, updating instead of creating new", book.getBookId());
            } else {
                // Create a new book if no existing book found (this should not happen with fixed code)
                book = new Book();
                book.setConversationId(request.getConversationId());
                book.setMessageId(request.getMessageId());
                book.setTitle(request.getTitle());
                book.setBookType(request.getBookType());
                book.setStyle(request.getStylePrompt());
                book.setStatus(Book.BookStatus.PROCESSING);
                book = bookRepository.save(book);
                log.info("Created new book with ID: {}", book.getBookId());
            }

            // Generate summary using Gemini with retry
            String summary = generateTextWithRetry(() ->
                            generateBookSummary(request.getPrompt(), request.getTitle()),
                    "book summary");

            book.setSummary(summary);
            bookRepository.save(book);

            // Generate chapters with retry
            List<BookContent> chapters = generateChaptersWithRetry(book, request.getPrompt(), request.getNumChapters());

            // Generate cover image
            Map<String, String> coverRequest = new HashMap<>();
            coverRequest.put("title", request.getTitle());
            coverRequest.put("style", request.getStylePrompt());

            Map<String, String> coverResponse = imageServiceClient.generateCoverImage(coverRequest);
            String coverPath = coverResponse.get("coverImagePath");

            // Ensure path is stored in a normalized format
            coverPath = normalizePath(coverPath);
            book.setCoverImagePath(coverPath);

            // Generate illustrations if needed
            if (request.isIncludeIllustrations() && request.getBookType() == Book.BookType.TEXT_IMAGE) {
                generateIllustrations(book, chapters, request.getStylePrompt());
            }

            // Generate PDF
            Map<String, Object> pdfRequest = new HashMap<>();
            pdfRequest.put("bookId", book.getBookId());
            pdfRequest.put("title", book.getTitle());
            pdfRequest.put("summary", book.getSummary());
            pdfRequest.put("coverImagePath", book.getCoverImagePath());
            pdfRequest.put("chapters", mapChaptersForPdf(chapters));
            pdfRequest.put("bookType", book.getBookType().toString());

            Map<String, String> pdfResponse = pdfServiceClient.generatePdf(pdfRequest);
            String pdfPath = pdfResponse.get("pdfPath");
            pdfPath = normalizePath(pdfPath);
            book.setPdfPath(pdfPath);

            // Generate preview image
            Map<String, String> previewResponse = pdfServiceClient.generatePreviewImage(book.getPdfPath());
            String previewPath = previewResponse.get("previewImagePath");
            previewPath = normalizePath(previewPath);
            book.setPreviewImagePath(previewPath);

            // Update book status to complete
            book.setStatus(Book.BookStatus.COMPLETE);
            bookRepository.save(book);

            log.info("Book generation completed successfully for book ID: {}", book.getBookId());
        } catch (Exception e) {
            log.error("Error processing book generation request", e);

            // Update book status to error if book exists
            if (book != null) {
                try {
                    book.setStatus(Book.BookStatus.FAILED);
                    bookRepository.save(book);
                    log.info("Updated book status to FAILED for book ID: {}", book.getBookId());
                } catch (Exception updateEx) {
                    log.error("Failed to update book status to FAILED", updateEx);
                }
            }

            log.error("Book generation failed for title: {}", request.getTitle());
        }
    }

    // Helper method to normalize file paths (remove Windows-specific elements)
    private String normalizePath(String path) {
        if (path == null) return null;

        // Extract just the filename if it's a full Windows path
        if (path.contains(":\\")) {
            Path p = Paths.get(path);
            String filename = p.getFileName().toString();

            // For illustrations, store them in the uploads directory
            if (path.contains("illustration")) {
                return UPLOADS_DIR + filename;
            }
            return filename;
        }
        return path;
    }

    // Generic retry method for handling rate limiting
    private <T> T generateTextWithRetry(TextGenerator<T> generator, String operationName) throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                return generator.generate();
            } catch (Exception e) {
                lastException = e;

                // Check if this is a rate limit error (429)
                if (e.getMessage() != null && e.getMessage().contains("429")) {
                    log.warn("Rate limit hit for {} (attempt {}/{}), retrying after {} ms",
                            operationName, attempt, MAX_RETRY_ATTEMPTS, RETRY_DELAY_MS);
                    Thread.sleep(RETRY_DELAY_MS * attempt); // Exponential backoff
                } else {
                    // For other errors, don't retry
                    throw e;
                }
            }
        }

        throw lastException != null ? lastException :
                new RuntimeException("Failed to generate " + operationName + " after " + MAX_RETRY_ATTEMPTS + " attempts");
    }

    // Functional interface for retry operations
    @FunctionalInterface
    private interface TextGenerator<T> {
        T generate() throws Exception;
    }

    private String generateBookSummary(String prompt, String title) {
        String summaryPrompt = String.format(
                "Create a brief summary for a book titled '%s' based on this prompt: %s. " +
                        "The summary should be around 200 words and capture the essence of the story.",
                title, prompt
        );

        return geminiClient.generateText(summaryPrompt);
    }

    private List<BookContent> generateChaptersWithRetry(Book book, String prompt, int numChapters) throws Exception {
        // First generate chapter titles with retry
        String chapterTitlesResponse = generateTextWithRetry(() -> {
            String titlesPrompt = String.format(
                    "Create %d chapter titles for a book titled '%s' based on this prompt: %s. " +
                            "Return only the chapter numbers and titles in the format: 1. Title",
                    numChapters, book.getTitle(), prompt
            );
            return geminiClient.generateText(titlesPrompt);
        }, "chapter titles");

        String[] chapterTitles = chapterTitlesResponse.split("\n");
        List<BookContent> chapters = new ArrayList<>();

        // Generate content for each chapter with retry
        for (int i = 0; i < numChapters && i < chapterTitles.length; i++) {
            final int chapterIndex = i;
            String chapterTitle = chapterTitles[i].replaceAll("^\\d+\\.\\s*", "").trim();

            String chapterContent = generateTextWithRetry(() -> {
                String contentPrompt = String.format(
                        "Write chapter %d titled '%s' for a book called '%s' about: %s. " +
                                "The chapter should be engaging and between 1000-1500 words.",
                        chapterIndex + 1, chapterTitle, book.getTitle(), prompt
                );
                return geminiClient.generateText(contentPrompt);
            }, "chapter " + (i+1) + " content");

            BookContent chapter = new BookContent();
            chapter.setBook(book);
            chapter.setChapterNumber(i + 1);
            chapter.setChapterTitle(chapterTitle);
            chapter.setChapterContent(chapterContent);

            chapters.add(bookContentRepository.save(chapter));
        }

        return chapters;
    }

    private void generateIllustrations(Book book, List<BookContent> chapters, String stylePrompt) {
        // For each chapter, generate an illustration based on the content
        for (BookContent chapter : chapters) {
            try {
                Map<String, Object> illustrationRequest = new HashMap<>();
                illustrationRequest.put("prompt", String.format(
                        "Create an illustration for chapter titled '%s' from the book '%s'. " +
                                "The chapter is about: %s",
                        chapter.getChapterTitle(), book.getTitle(),
                        chapter.getChapterContent().substring(0, Math.min(200, chapter.getChapterContent().length()))
                ));
                illustrationRequest.put("style", stylePrompt);

                List<Map<String, String>> illustrations = imageServiceClient.generateIllustrations(illustrationRequest);

                if (!illustrations.isEmpty()) {
                    String illustrationPath = illustrations.get(0).get("illustrationPath");
                    illustrationPath = normalizePath(illustrationPath);
                    chapter.setIllustrationPath(illustrationPath);
                    bookContentRepository.save(chapter);
                }
            } catch (Exception e) {
                log.error("Failed to generate illustration for chapter {}", chapter.getChapterNumber(), e);
                // Continue with other chapters even if one fails
            }
        }
    }

    private List<Map<String, Object>> mapChaptersForPdf(List<BookContent> chapters) {
        return chapters.stream()
                .map(chapter -> {
                    Map<String, Object> chapterMap = new HashMap<>();
                    chapterMap.put("chapterNumber", chapter.getChapterNumber());
                    chapterMap.put("chapterTitle", chapter.getChapterTitle());
                    chapterMap.put("chapterContent", chapter.getChapterContent());

                    // Normalize illustration path
                    String illustrationPath = chapter.getIllustrationPath();
                    if (illustrationPath != null) {
                        illustrationPath = normalizePath(illustrationPath);
                    }
                    chapterMap.put("illustrationPath", illustrationPath);

                    return chapterMap;
                })
                .collect(Collectors.toList());
    }

    public List<BookDto.BookResponse> getBooksByConversationId(Long conversationId) {
        List<Book> books = bookRepository.findByConversationId(conversationId);

        return books.stream()
                .map(this::mapBookToResponse)
                .collect(Collectors.toList());
    }

    public BookDto.BookDetailResponse getBookDetails(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        List<BookContent> chapters = bookContentRepository.findByBookBookIdOrderByChapterNumberAsc(bookId);

        List<BookDto.ChapterResponse> chapterResponses = chapters.stream()
                .map(chapter -> {
                    String illustrationUrl = null;
                    if (chapter.getIllustrationPath() != null) {
                        // Extract just the filename for the URL
                        String filename = Paths.get(chapter.getIllustrationPath()).getFileName().toString();
                        illustrationUrl = "/api/books/illustrations/" + filename;
                    }

                    return new BookDto.ChapterResponse(
                            chapter.getContentId(),
                            chapter.getChapterNumber(),
                            chapter.getChapterTitle(),
                            chapter.getChapterContent(),
                            illustrationUrl
                    );
                })
                .collect(Collectors.toList());

        return new BookDto.BookDetailResponse(
                book.getBookId(),
                book.getTitle(),
                book.getSummary(),
                book.getStyle(),
                book.getBookType(),
                book.getStatus(),
                book.getPreviewImagePath(),
                book.getPdfPath(),
                book.getCoverImagePath() != null ? "/api/books/" + book.getBookId() + "/cover" : null,
                chapterResponses,
                book.getCreatedAt(),
                book.getUpdatedAt()
        );
    }

    public void deleteBook(Long bookId) {
        bookRepository.deleteById(bookId);
    }

    public BookDto.BookResponse getBookStatus(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        return mapBookToResponse(book);
    }

    private BookDto.BookResponse mapBookToResponse(Book book) {
        return new BookDto.BookResponse(
                book.getBookId(),
                book.getTitle(),
                book.getStatus(),
                book.getPreviewImagePath() != null ? "/api/books/" + book.getBookId() + "/preview" : null,
                book.getPdfPath() != null ? "/api/books/" + book.getBookId() + "/download" : null,
                book.getCoverImagePath() != null ? "/api/books/" + book.getBookId() + "/cover" : null,
                book.getCreatedAt()
        );
    }

    public Book getBookEntity(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found with ID: " + bookId));
    }
}