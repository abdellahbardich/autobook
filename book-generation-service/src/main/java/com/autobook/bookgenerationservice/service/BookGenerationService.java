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
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        try {
            log.info("Received book generation request for title: {}", request.getTitle());

            // Create a new book from the request
            Book book = new Book();
            book.setConversationId(request.getConversationId());
            book.setMessageId(request.getMessageId());
            book.setTitle(request.getTitle());
            book.setBookType(request.getBookType());
            book.setStyle(request.getStylePrompt());
            book.setStatus(Book.BookStatus.PROCESSING);
            book = bookRepository.save(book);

            // Generate summary using Gemini
            String summary = generateBookSummary(request.getPrompt(), request.getTitle());
            book.setSummary(summary);
            bookRepository.save(book);

            // Generate chapters
            List<BookContent> chapters = generateChapters(book, request.getPrompt(), request.getNumChapters());

            // Generate cover image
            Map<String, String> coverRequest = new HashMap<>();
            coverRequest.put("title", request.getTitle());
            coverRequest.put("style", request.getStylePrompt());

            Map<String, String> coverResponse = imageServiceClient.generateCoverImage(coverRequest);
            book.setCoverImagePath(coverResponse.get("coverImagePath"));

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
            book.setPdfPath(pdfResponse.get("pdfPath"));

            // Generate preview image
            Map<String, String> previewResponse = pdfServiceClient.generatePreviewImage(book.getPdfPath());
            book.setPreviewImagePath(previewResponse.get("previewImagePath"));

            // Update book status to complete
            book.setStatus(Book.BookStatus.COMPLETE);
            bookRepository.save(book);

            log.info("Book generation completed successfully for book ID: {}", book.getBookId());
        } catch (Exception e) {
            log.error("Error processing book generation request", e);
            log.error("Book generation failed for title: {}", request.getTitle());
        }
    }

    private String generateBookSummary(String prompt, String title) {
        String summaryPrompt = String.format(
                "Create a brief summary for a book titled '%s' based on this prompt: %s. " +
                        "The summary should be around 200 words and capture the essence of the story.",
                title, prompt
        );

        return geminiClient.generateText(summaryPrompt);
    }

    private List<BookContent> generateChapters(Book book, String prompt, int numChapters) {
        List<BookContent> chapters = new ArrayList<>();

        // First, generate chapter titles
        String chapterTitlesPrompt = String.format(
                "Create %d chapter titles for a book titled '%s' based on this prompt: %s. " +
                        "Return only the chapter numbers and titles in the format: 1. Title",
                numChapters, book.getTitle(), prompt
        );

        String chapterTitlesResponse = geminiClient.generateText(chapterTitlesPrompt);
        String[] chapterTitles = chapterTitlesResponse.split("\n");

        // Then generate content for each chapter
        for (int i = 0; i < numChapters && i < chapterTitles.length; i++) {
            String chapterTitle = chapterTitles[i].replaceAll("^\\d+\\.\\s*", "").trim();

            String chapterContentPrompt = String.format(
                    "Write chapter %d titled '%s' for a book called '%s' about: %s. " +
                            "The chapter should be engaging and between 1000-1500 words.",
                    i + 1, chapterTitle, book.getTitle(), prompt
            );

            String chapterContent = geminiClient.generateText(chapterContentPrompt);

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
                chapter.setIllustrationPath(illustrations.get(0).get("illustrationPath"));
                bookContentRepository.save(chapter);
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
                    chapterMap.put("illustrationPath", chapter.getIllustrationPath());
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
                .map(chapter -> new BookDto.ChapterResponse(
                        chapter.getContentId(),
                        chapter.getChapterNumber(),
                        chapter.getChapterTitle(),
                        chapter.getChapterContent(),
                        chapter.getIllustrationPath() != null ?
                                "/api/books/illustrations/" + chapter.getIllustrationPath() : null
                ))
                .collect(Collectors.toList());

        return new BookDto.BookDetailResponse(
                book.getBookId(),
                book.getTitle(),
                book.getSummary(),
                book.getStyle(),
                book.getBookType(),
                book.getStatus(),
                book.getPreviewImagePath() != null ? "/api/books/" + book.getBookId() + "/preview" : null,
                book.getPdfPath() != null ? "/api/books/" + book.getBookId() + "/download" : null,
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