package com.autobook.collectionservice.repository;

import com.autobook.collectionservice.entity.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollectionRepository extends JpaRepository<Collection, Long> {

    List<Collection> findByUserId(Long userId);

    Optional<Collection> findByCollectionIdAndUserId(Long collectionId, Long userId);
}

