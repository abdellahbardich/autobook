package com.autobook.bookgenerationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "book_content")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long contentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    private Integer chapterNumber;

    private String chapterTitle;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String chapterContent;

    private String illustrationPath;
}