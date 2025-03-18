package com.autobook.collectionservice.entity;

import com.autobook.collectionservice.entity.Collection;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "collection_books")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long collectionBookId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false)
    private Collection collection;

    @Column(nullable = false)
    private Long bookId;

    @CreationTimestamp
    private LocalDateTime addedAt;
}