package com.autobook.orchestration.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.autobook.orchestration.dto.BookCreationDto;
import com.autobook.orchestration.dto.BookDto;
import com.autobook.orchestration.service.BookService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @PostMapping
    public ResponseEntity<BookDto> createBook(@Valid @RequestBody BookCreationDto bookCreationDto) {
        BookDto bookDto = bookService.createBook(bookCreationDto);
        return new ResponseEntity<>(bookDto, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDto> getBookById(@PathVariable Long id) {
        BookDto bookDto = bookService.getBookById(id);
        return ResponseEntity.ok(bookDto);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookDto>> getBooksByUserId(@PathVariable Long userId) {
        List<BookDto> books = bookService.getBooksByUserId(userId);
        return ResponseEntity.ok(books);
    }

    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<List<BookDto>> getBooksByUserIdAndStatus(
            @PathVariable Long userId,
            @PathVariable String status) {
        List<BookDto> books = bookService.getBooksByUserIdAndStatus(userId, status);
        return ResponseEntity.ok(books);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}