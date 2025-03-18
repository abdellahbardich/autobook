package com.autobook.collectionservice.repository;

import com.autobook.collectionservice.entity.CollectionBook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollectionBookRepository extends JpaRepository<CollectionBook, Long> {

    List<CollectionBook> findByCollectionCollectionId(Long collectionId);

    Optional<CollectionBook> findByCollectionCollectionIdAndBookId(Long collectionId, Long bookId);

    void deleteByCollectionCollectionIdAndBookId(Long collectionId, Long bookId);
}