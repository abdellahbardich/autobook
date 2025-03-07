package com.autobook.storage.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.autobook.storage.dto.BookFileDto;
import com.autobook.storage.service.BookFileService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/book-files")
@RequiredArgsConstructor
public class BookFileController {

    private final BookFileService bookFileService;

    @PostMapping
    public ResponseEntity<BookFileDto> uploadBookFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("userId") Long userId,
            @RequestParam("bookId") Long bookId,
            @RequestParam("fileType") String fileType) {

        BookFileDto bookFileDto = bookFileService.saveBookFile(file, title, description, userId, bookId, fileType);
        return new ResponseEntity<>(bookFileDto, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookFileDto> getBookFile(@PathVariable Long id) {
        BookFileDto bookFileDto = bookFileService.getBookFileById(id);
        return ResponseEntity.ok(bookFileDto);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookFileDto>> getBookFilesByUserId(@PathVariable Long userId) {
        List<BookFileDto> bookFiles = bookFileService.getBookFilesByUserId(userId);
        return ResponseEntity.ok(bookFiles);
    }

    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<List<BookFileDto>> getBookFilesByUserIdAndStatus(
            @PathVariable Long userId,
            @PathVariable String status) {
        List<BookFileDto> bookFiles = bookFileService.getBookFilesByUserIdAndStatus(userId, status);
        return ResponseEntity.ok(bookFiles);
    }

    @GetMapping("/book/{bookId}/type/{fileType}")
    public ResponseEntity<BookFileDto> getBookFileByBookIdAndFileType(
            @PathVariable Long bookId,
            @PathVariable String fileType) {
        BookFileDto bookFileDto = bookFileService.getBookFileByBookIdAndFileType(bookId, fileType);
        return ResponseEntity.ok(bookFileDto);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<BookFileDto> updateBookFileStatus(
            @PathVariable Long id,
            @RequestParam("status") String status) {

        BookFileDto updatedBookFileDto = bookFileService.updateBookFileStatus(id, status);
        return ResponseEntity.ok(updatedBookFileDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBookFile(@PathVariable Long id) {
        bookFileService.deleteBookFile(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadBookFile(@PathVariable Long id) {
        byte[] bookFileData = bookFileService.downloadBookFile(id);
        BookFileDto bookFileDto = bookFileService.getBookFileById(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", bookFileDto.getTitle() + ".pdf");
        headers.setContentLength(bookFileData.length);

        return new ResponseEntity<>(bookFileData, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}/url")
    public ResponseEntity<String> getBookFileUrl(@PathVariable Long id) {
        String url = bookFileService.getBookFileDownloadUrl(id);
        return ResponseEntity.ok(url);
    }
}