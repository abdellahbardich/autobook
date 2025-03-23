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
import java.util.Optional;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Slf4j
public class BookController {

    private final BookGenerationService bookGenerationService;
    private static final String UPLOAD_DIR = "uploads/images/";

    @GetMapping("/illustrations/{path:.+}")
    public ResponseEntity<Resource> getIllustrationImage(
            @PathVariable("path") String path) {
        try {
            // Sanitize and validate the path
            if (path.contains("..") || path.contains(":\\")) {
                log.error("Potentially unsafe path requested: {}", path);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            // Always use the uploads directory for illustrations
            String fullPath = UPLOAD_DIR + path;
            log.info("Attempting to serve illustration from path: {}", fullPath);

            File file = new File(fullPath);
            if (!file.exists()) {
                log.error("Illustration file does not exist at path: {}", fullPath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .contentType(determineMediaType(fullPath))
                    .body(resource);
        } catch (Exception e) {
            log.error("Error serving illustration: {}", path, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/file")
    public ResponseEntity<Resource> getFileByPath(@RequestParam("path") String filePath) {
        try {
            // Validate the file path for security
            if (filePath.contains("..") || !isAuthorizedPath(filePath)) {
                log.error("Unauthorized file path requested: {}", filePath);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }

            log.info("Attempting to serve file from path: {}", filePath);

            File file = new File(filePath);
            if (!file.exists()) {
                log.error("File does not exist at path: {}", filePath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .contentType(determineMediaType(filePath))
                    .body(resource);
        } catch (Exception e) {
            log.error("Error serving file: {}", filePath, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Helper method to determine media type
    private MediaType determineMediaType(String filename) {
        filename = filename.toLowerCase();
        if (filename.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        } else if (filename.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        } else {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    // Helper method to validate path is in authorized directories
    private boolean isAuthorizedPath(String path) {
        // Allow only paths in certain directories
        return path.startsWith(UPLOAD_DIR) ||
                path.startsWith("books/") ||
                path.startsWith("pdfs/");
    }

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
            String receivedPath = book.getDownloadUrl();

            if (receivedPath == null) {
                log.error("Download URL is null for book ID: {}", bookId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            // Determine the filename: if the received path is "download", fallback to using the book entity's PDF path.
            String fileName;
            if ("download".equalsIgnoreCase(receivedPath)) {
                // Fallback: retrieve the full PDF path from the book entity.
                Book bookEntity = bookGenerationService.getBookEntity(bookId);
                fileName = Paths.get(bookEntity.getPdfPath()).getFileName().toString();
            } else {
                fileName = Paths.get(receivedPath).getFileName().toString();
            }

            // Define the fixed base directory.
            String baseDir = "C:\\Users\\Youcode\\Desktop\\autobook\\s3\\uploads\\pdfs";
            // Build the full path.
            String pdfPath = Paths.get(baseDir, fileName).toString();

            log.info("Attempting to download PDF from path: {}", pdfPath);

            Path path = Paths.get(pdfPath);
            if (!Files.exists(path)) {
                log.error("PDF file does not exist at path: {}", path);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            Resource resource = new FileSystemResource(path.toFile());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +
                            path.getFileName().toString() + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Error downloading PDF for book ID: {}", bookId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }



    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> getPreviewImage(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("id") Long bookId) {

        try {
            BookDto.BookDetailResponse book = bookGenerationService.getBookDetails(bookId);
            String receivedUrl = book.getPreviewImageUrl();

            if (receivedUrl == null) {
                log.error("Preview image URL is null for book ID: {}", bookId);
                return ResponseEntity.notFound().build();
            }

            // Determine the filename: if the URL is in API format, fall back to the book entity's preview image path.
            String fileName;
            if (receivedUrl.startsWith("/api/")) {
                Book bookEntity = bookGenerationService.getBookEntity(bookId);
                fileName = Paths.get(bookEntity.getPreviewImagePath()).getFileName().toString();
            } else {
                fileName = Paths.get(receivedUrl).getFileName().toString();
            }

            // Define the fixed base directory for preview images.
            String baseDir = "C:\\Users\\Youcode\\Desktop\\autobook\\s3\\uploads\\previews";
            // Construct the full image path.
            String imagePath = Paths.get(baseDir, fileName).toString();
            log.info("Attempting to serve preview image from path: {}", imagePath);

            // Validate that the file exists.
            Path path = Paths.get(imagePath);
            if (!Files.exists(path)) {
                log.error("Preview image does not exist at path: {}", imagePath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(path.toFile());

            return ResponseEntity.ok()
                    .contentType(determineMediaType(imagePath))
                    .body(resource);
        } catch (Exception e) {
            log.error("Error serving preview for book ID: {}", bookId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


    @GetMapping("/{id}/cover")
    public ResponseEntity<Resource> getCoverImage(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("id") Long bookId) {

        try {
            BookDto.BookDetailResponse book = bookGenerationService.getBookDetails(bookId);
            String receivedUrl = book.getPreviewImageUrl();

            if (receivedUrl == null) {
                log.error("Preview image URL is null for book ID: {}", bookId);
                return ResponseEntity.notFound().build();
            }

            // Determine the filename: if the URL is in API format, fall back to the book entity's preview image path.
            String fileName;
            if (receivedUrl.startsWith("/api/")) {
                Book bookEntity = bookGenerationService.getBookEntity(bookId);
                fileName = Paths.get(bookEntity.getPreviewImagePath()).getFileName().toString();
            } else {
                fileName = Paths.get(receivedUrl).getFileName().toString();
            }

            // Define the fixed base directory for preview images.
            String baseDir = "C:\\Users\\Youcode\\Desktop\\autobook\\s3\\uploads\\previews";
            // Construct the full image path.
            String imagePath = Paths.get(baseDir, fileName).toString();
            log.info("Attempting to serve preview image from path: {}", imagePath);

            // Validate that the file exists.
            Path path = Paths.get(imagePath);
            if (!Files.exists(path)) {
                log.error("Preview image does not exist at path: {}", imagePath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(path.toFile());

            return ResponseEntity.ok()
                    .contentType(determineMediaType(imagePath))
                    .body(resource);
        } catch (Exception e) {
            log.error("Error serving preview for book ID: {}", bookId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


//    @GetMapping("/{id}/cover")
//    public ResponseEntity<Resource> getCoverImage(
//            @RequestHeader("X-Auth-User-Id") Long userId,
//            @PathVariable("id") Long bookId) {
//        try {
//            BookDto.BookDetailResponse book = bookGenerationService.getBookDetails(bookId);
//            String receivedUrl = book.getCoverImageUrl();
//
//            if (receivedUrl == null) {
//                log.error("Cover image URL is null for book ID: {}", bookId);
//                return ResponseEntity.notFound().build();
//            }
//
//            // Determine the filename: if the URL is in API format, use the actual cover image path from the book entity.
//            String fileName;
//            if (receivedUrl.startsWith("/api/")) {
//                Book bookEntity = bookGenerationService.getBookEntity(bookId);
//                fileName = Paths.get(bookEntity.getCoverImagePath()).getFileName().toString();
//            } else {
//                fileName = Paths.get(receivedUrl).getFileName().toString();
//            }
//
//            // Define the fixed base directory for cover images.
//            String baseDir = "C:\\Users\\Youcode\\Desktop\\autobook\\pdf-service\\uploads\\previews";
//            // Construct the full image path.
//            String imagePath = Paths.get(baseDir, fileName).toString();
//            log.info("Attempting to serve cover image from path: {}", imagePath);
//
//            // Validate that the file exists.
//            Path path = Paths.get(imagePath);
//            if (!Files.exists(path)) {
//                log.error("Cover image does not exist at path: {}", imagePath);
//                return ResponseEntity.notFound().build();
//            }
//
//            Resource resource = new FileSystemResource(path.toFile());
//
//            return ResponseEntity.ok()
//                    .contentType(determineMediaType(imagePath))
//                    .body(resource);
//        } catch (Exception e) {
//            log.error("Error serving cover for book ID: {}", bookId, e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//    }

}