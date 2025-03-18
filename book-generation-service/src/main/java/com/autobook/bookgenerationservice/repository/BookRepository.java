package com.autobook.bookgenerationservice.repository;

import com.autobook.bookgenerationservice.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByConversationId(Long conversationId);
    List<Book> findByStatus(Book.BookStatus status);
}
