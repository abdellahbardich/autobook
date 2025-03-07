package com.autobook.storage.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.autobook.storage.dto.BookFileDto;
import com.autobook.storage.exception.ResourceNotFoundException;
import com.autobook.storage.model.BookFile;
import com.autobook.storage.repository.BookFileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookFileServiceImpl implements BookFileService {

    private final BookFileRepository bookFileRepository;
    private final S3Service s3Service;

    @Override
    @Transactional
    public BookFileDto saveBookFile(MultipartFile file, String title, String description, Long userId, Long bookId, String fileType) {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String s3Key = "books/" + userId + "/" + bookId + "/" + fileType + "/" + UUID.randomUUID() + extension;

        s3Service.uploadFile(s3Key, file);

        BookFile bookFile = BookFile.builder()
                .title(title)
                .s3Key(s3Key)
                .fileType(fileType)
                .size(file.getSize())
                .description(description)
                .status("COMPLETE")
                .userId(userId)
                .bookId(bookId)
                .build();

        BookFile savedBookFile = bookFileRepository.save(bookFile);
        return mapToBookFileDto(savedBookFile);
    }

    @Override
    public BookFileDto getBookFileById(Long id) {
        BookFile bookFile = bookFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book file not found with id: " + id));
        return mapToBookFileDto(bookFile);
    }

    @Override
    public List<BookFileDto> getBookFilesByUserId(Long userId) {
        return bookFileRepository.findByUserId(userId).stream()
                .map(this::mapToBookFileDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookFileDto> getBookFilesByUserIdAndStatus(Long userId, String status) {
        return bookFileRepository.findByUserIdAndStatus(userId, status).stream()
                .map(this::mapToBookFileDto)
                .collect(Collectors.toList());
    }

    @Override
    public BookFileDto getBookFileByBookIdAndFileType(Long bookId, String fileType) {
        BookFile bookFile = bookFileRepository.findByBookIdAndFileType(bookId, fileType)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Book file not found with bookId: " + bookId + " and fileType: " + fileType));
        return mapToBookFileDto(bookFile);
    }

    @Override
    @Transactional
    public BookFileDto updateBookFileStatus(Long id, String status) {
        BookFile bookFile = bookFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book file not found with id: " + id));

        bookFile.setStatus(status);

        BookFile updatedBookFile = bookFileRepository.save(bookFile);
        return mapToBookFileDto(updatedBookFile);
    }

    @Override
    @Transactional
    public void deleteBookFile(Long id) {
        BookFile bookFile = bookFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book file not found with id: " + id));

        s3Service.deleteFile(bookFile.getS3Key());
        bookFileRepository.deleteById(id);
    }

    @Override
    public byte[] downloadBookFile(Long id) {
        BookFile bookFile = bookFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book file not found with id: " + id));

        return s3Service.downloadFile(bookFile.getS3Key());
    }

    @Override
    public String getBookFileDownloadUrl(Long id) {
        BookFile bookFile = bookFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book file not found with id: " + id));

        return s3Service.generatePresignedUrl(bookFile.getS3Key(), 60).toString();
    }

    private BookFileDto mapToBookFileDto(BookFile bookFile) {
        return BookFileDto.builder()
                .id(bookFile.getId())
                .title(bookFile.getTitle())
                .s3Key(bookFile.getS3Key())
                .fileType(bookFile.getFileType())
                .size(bookFile.getSize())
                .description(bookFile.getDescription())
                .status(bookFile.getStatus())
                .userId(bookFile.getUserId())
                .bookId(bookFile.getBookId())
                .downloadUrl(s3Service.generatePresignedUrl(bookFile.getS3Key(), 60).toString())
                .createdAt(bookFile.getCreatedAt())
                .updatedAt(bookFile.getUpdatedAt())
                .build();
    }
}