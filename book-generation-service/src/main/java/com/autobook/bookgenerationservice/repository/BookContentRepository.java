
package com.autobook.bookgenerationservice.repository;

import com.autobook.bookgenerationservice.entity.BookContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookContentRepository extends JpaRepository<BookContent, Long> {
    List<BookContent> findByBookBookIdOrderByChapterNumberAsc(Long bookId);
}