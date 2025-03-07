package com.autobook.orchestration.service;

import java.util.List;

import com.autobook.orchestration.dto.BookCreationDto;
import com.autobook.orchestration.dto.BookDto;

public interface BookService {
    BookDto createBook(BookCreationDto bookCreationDto);
    BookDto getBookById(Long id);
    List<BookDto> getBooksByUserId(Long userId);
    List<BookDto> getBooksByUserIdAndStatus(Long userId, String status);
    BookDto updateBookStatus(Long id, String status);
    void deleteBook(Long id);
}