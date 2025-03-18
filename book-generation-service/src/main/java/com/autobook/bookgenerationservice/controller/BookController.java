package com.autobook.bookgenerationservice.controller;

import com.autobook.bookgenerationservice.dto.BookDto;
import com.autobook.bookgenerationservice.entity.Book;
import com.autobook.bookgenerationservice.service.BookGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Slf4j
public class BookController {

    private final BookGenerationService bookGenerationService;

    @PostMapping
    public ResponseEntity<BookDto.BookResponse> createBook(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @Valid @RequestBody BookDto.BookCreationRequest request) {
        log.info("Received request to create book: {}", request.getTitle());
        return new ResponseEntity<>(bookGenerationService.createBook(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDto.BookDetailResponse> getBookDetails(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("id") Long bookId) {
        return ResponseEntity.ok(bookGenerationService.getBookDetails(bookId));
    }

    @GetMapping("/conversation/{id}")
    public ResponseEntity<List<BookDto.BookResponse>> getBooksByConversation(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("id") Long conversationId) {
        return ResponseEntity.ok(bookGenerationService.getBooksByConversationId(conversationId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("id") Long bookId) {
        bookGenerationService.deleteBook(bookId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<Book.BookStatus> getBookStatus(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("id") Long bookId) {
        BookDto.BookResponse bookResponse = bookGenerationService.getBookStatus(bookId);
        return ResponseEntity.ok(bookResponse.getStatus());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadPdf(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("id") Long bookId) {
        try {
            BookDto.BookDetailResponse book = bookGenerationService.getBookDetails(bookId);

            String pdfPath = null;
            if (book.getDownloadUrl() != null) {
                if (book.getDownloadUrl().startsWith("/api/")) {
                    log.info("Download URL is in API format: {}", book.getDownloadUrl());
                    Book bookEntity = bookGenerationService.getBookEntity(bookId);
                    pdfPath = bookEntity.getPdfPath();
                } else {
                    pdfPath = book.getDownloadUrl();
                }
            }

            if (pdfPath == null) {
                log.error("PDF path is null for book ID: {}", bookId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null);
            }

            log.info("Attempting to download PDF from path: {}", pdfPath);

            Path path = Paths.get(pdfPath);
            if (!Files.exists(path)) {
                log.error("PDF file does not exist at path: {}", path);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null);
            }

            Resource resource = new FileSystemResource(path.toFile());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +
                            path.getFileName().toString() + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Error downloading PDF for book ID: {}", bookId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> getPreviewImage(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("id") Long bookId) {
        BookDto.BookDetailResponse book = bookGenerationService.getBookDetails(bookId);

        if (book.getPreviewImageUrl() == null) {
            return ResponseEntity.notFound().build();
        }

        String imagePath;
        if (book.getPreviewImageUrl().startsWith("/api/")) {
            Book bookEntity = bookGenerationService.getBookEntity(bookId);
            imagePath = bookEntity.getPreviewImagePath();
        } else {
            imagePath = book.getPreviewImageUrl();
        }

        if (imagePath == null) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(imagePath);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(resource);
    }

    @GetMapping("/{id}/cover")
    public ResponseEntity<Resource> getCoverImage(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("id") Long bookId) {
        BookDto.BookDetailResponse book = bookGenerationService.getBookDetails(bookId);

        if (book.getCoverImageUrl() == null) {
            return ResponseEntity.notFound().build();
        }

        String imagePath;
        if (book.getCoverImageUrl().startsWith("/api/")) {
            Book bookEntity = bookGenerationService.getBookEntity(bookId);
            imagePath = bookEntity.getCoverImagePath();
        } else {
            imagePath = book.getCoverImageUrl();
        }

        if (imagePath == null) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(imagePath);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String contentType;
        if (imagePath.endsWith(".png")) {
            contentType = MediaType.IMAGE_PNG_VALUE;
        } else if (imagePath.endsWith(".jpg") || imagePath.endsWith(".jpeg")) {
            contentType = MediaType.IMAGE_JPEG_VALUE;
        } else {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}