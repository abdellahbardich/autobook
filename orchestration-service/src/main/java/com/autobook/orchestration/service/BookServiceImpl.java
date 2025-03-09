package com.autobook.orchestration.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autobook.orchestration.dto.BookCreationDto;
import com.autobook.orchestration.dto.BookDto;
import com.autobook.orchestration.exception.ResourceNotFoundException;
import com.autobook.orchestration.model.Book;
import com.autobook.orchestration.repository.BookRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final WorkflowService workflowService;

    @Value("${service.storage.url}")
    private String storageServiceUrl;

    @Override
    @Transactional
    public BookDto createBook(BookCreationDto bookCreationDto) {
        Book book = Book.builder()
                .userId(bookCreationDto.getUserId())
                .title(bookCreationDto.getTitle())
                .description(bookCreationDto.getSummary())
                .status("DRAFT")
                .type(bookCreationDto.getBookType())
                .build();

        Book savedBook = bookRepository.save(book);

        workflowService.startBookGenerationWorkflow(savedBook.getId(), bookCreationDto);

        return mapToBookDto(savedBook);
    }

    @Override
    public BookDto getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
        return mapToBookDto(book);
    }

    @Override
    public List<BookDto> getBooksByUserId(Long userId) {
        return bookRepository.findByUserId(userId).stream()
                .map(this::mapToBookDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookDto> getBooksByUserIdAndStatus(Long userId, String status) {
        return bookRepository.findByUserIdAndStatus(userId, status).stream()
                .map(this::mapToBookDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookDto updateBookStatus(Long id, String status) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        book.setStatus(status);
        Book updatedBook = bookRepository.save(book);
        return mapToBookDto(updatedBook);
    }

    @Override
    @Transactional
    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Book not found with id: " + id);
        }
        bookRepository.deleteById(id);
    }

    private BookDto mapToBookDto(Book book) {
        String coverImageUrl = null;
        String pdfUrl = null;

        if (book.getCoverImageS3Key() != null) {
            coverImageUrl = storageServiceUrl + "/assets/url/" + book.getCoverImageS3Key();
        }

        if (book.getPdfS3Key() != null) {
            pdfUrl = storageServiceUrl + "/book-files/url/" + book.getPdfS3Key();
        }

        return BookDto.builder()
                .id(book.getId())
                .userId(book.getUserId())
                .title(book.getTitle())
                .description(book.getDescription())
                .status(book.getStatus())
                .type(book.getType())
                .coverImageUrl(coverImageUrl)
                .pdfUrl(pdfUrl)
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .build();
    }
}