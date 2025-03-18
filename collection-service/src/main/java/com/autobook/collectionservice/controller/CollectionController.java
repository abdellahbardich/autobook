package com.autobook.collectionservice.controller;

import com.autobook.collectionservice.dto.CollectionDto;
import com.autobook.collectionservice.service.CollectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
@Slf4j
public class CollectionController {

    private final CollectionService collectionService;

    @GetMapping
    public ResponseEntity<List<CollectionDto.CollectionResponse>> getUserCollections(
            @RequestHeader("X-Auth-User-Id") Long userId) {
        return ResponseEntity.ok(collectionService.getUserCollections(userId));
    }

    @PostMapping
    public ResponseEntity<CollectionDto.CollectionResponse> createCollection(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @Valid @RequestBody CollectionDto.CreateCollectionRequest request) {
        return new ResponseEntity<>(collectionService.createCollection(userId, request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CollectionDto.CollectionDetailResponse> getCollectionDetails(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("id") Long collectionId) {
        return ResponseEntity.ok(collectionService.getCollectionDetails(userId, collectionId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CollectionDto.CollectionResponse> updateCollection(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("id") Long collectionId,
            @RequestBody CollectionDto.UpdateCollectionRequest request) {
        return ResponseEntity.ok(collectionService.updateCollection(userId, collectionId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCollection(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("id") Long collectionId) {
        collectionService.deleteCollection(userId, collectionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/books/{bookId}")
    public ResponseEntity<Void> addBookToCollection(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("id") Long collectionId,
            @PathVariable("bookId") Long bookId) {
        collectionService.addBookToCollection(userId, collectionId, bookId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/books/{bookId}")
    public ResponseEntity<Void> removeBookFromCollection(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("id") Long collectionId,
            @PathVariable("bookId") Long bookId) {
        collectionService.removeBookFromCollection(userId, collectionId, bookId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/books")
    public ResponseEntity<List<CollectionDto.BookSummary>> getBooksInCollection(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("id") Long collectionId) {
        return ResponseEntity.ok(collectionService.getBooksInCollection(userId, collectionId));
    }
}