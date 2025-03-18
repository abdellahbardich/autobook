package com.autobook.bookgenerationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "books")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookId;

    @Column(nullable = false)
    private Long conversationId;

    private Long messageId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private String style;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookType bookType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookStatus status;

    private String pdfPath;

    private String previewImagePath;

    private String coverImagePath;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookContent> contents = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum BookType {
        TEXT_ONLY, TEXT_IMAGE
    }

    public enum BookStatus {
        DRAFT, PROCESSING, COMPLETE, FAILED
    }
}
