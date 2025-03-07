package com.autobook.storage.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.autobook.storage.model.BookFile;

@Repository
public interface BookFileRepository extends JpaRepository<BookFile, Long> {
    List<BookFile> findByUserId(Long userId);
    List<BookFile> findByUserIdAndStatus(Long userId, String status);
    Optional<BookFile> findByBookIdAndFileType(Long bookId, String fileType);
}