package com.autobook.storage.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.autobook.storage.dto.BookFileDto;

public interface BookFileService {
    BookFileDto saveBookFile(MultipartFile file, String title, String description, Long userId, Long bookId, String fileType);
    BookFileDto getBookFileById(Long id);
    List<BookFileDto> getBookFilesByUserId(Long userId);
    List<BookFileDto> getBookFilesByUserIdAndStatus(Long userId, String status);
    BookFileDto getBookFileByBookIdAndFileType(Long bookId, String fileType);
    BookFileDto updateBookFileStatus(Long id, String status);
    void deleteBookFile(Long id);
    byte[] downloadBookFile(Long id);
    String getBookFileDownloadUrl(Long id);
}