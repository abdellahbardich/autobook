package com.autobook.collectionservice.service;

import com.autobook.collectionservice.client.BookServiceClient;
import com.autobook.collectionservice.dto.CollectionDto;
import com.autobook.collectionservice.entity.Collection;
import com.autobook.collectionservice.entity.CollectionBook;
import com.autobook.collectionservice.repository.CollectionBookRepository;
import com.autobook.collectionservice.repository.CollectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final CollectionBookRepository collectionBookRepository;
    private final BookServiceClient bookServiceClient;

    @Transactional
    public CollectionDto.CollectionResponse createCollection(Long userId, CollectionDto.CreateCollectionRequest request) {
        Collection collection = new Collection();
        collection.setName(request.getName());
        collection.setDescription(request.getDescription());
        collection.setUserId(userId);

        Collection savedCollection = collectionRepository.save(collection);
        log.info("Created collection: {} for user: {}", savedCollection.getName(), userId);

        return mapToCollectionResponse(savedCollection);
    }

    public List<CollectionDto.CollectionResponse> getUserCollections(Long userId) {
        List<Collection> collections = collectionRepository.findByUserId(userId);

        return collections.stream()
                .map(this::mapToCollectionResponse)
                .collect(Collectors.toList());
    }

    public CollectionDto.CollectionDetailResponse getCollectionDetails(Long userId, Long collectionId) {
        Collection collection = collectionRepository.findByCollectionIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new RuntimeException("Collection not found"));

        List<CollectionBook> collectionBooks = collectionBookRepository.findByCollectionCollectionId(collectionId);

        List<CollectionDto.BookSummary> bookSummaries = new ArrayList<>();

        for (CollectionBook collectionBook : collectionBooks) {
            try {
                BookServiceClient.BookSummary bookSummary = bookServiceClient.getBookDetails(userId, collectionBook.getBookId());

                bookSummaries.add(new CollectionDto.BookSummary(
                        bookSummary.getBookId(),
                        bookSummary.getTitle(),
                        bookSummary.getPreviewImageUrl(),
                        collectionBook.getAddedAt()
                ));
            } catch (Exception e) {
                log.error("Error fetching book details for book ID: {}", collectionBook.getBookId(), e);
                bookSummaries.add(new CollectionDto.BookSummary(
                        collectionBook.getBookId(),
                        "Unknown Book",
                        null,
                        collectionBook.getAddedAt()
                ));
            }
        }

        return new CollectionDto.CollectionDetailResponse(
                collection.getCollectionId(),
                collection.getName(),
                collection.getDescription(),
                bookSummaries,
                collection.getCreatedAt(),
                collection.getUpdatedAt()
        );
    }

    @Transactional
    public CollectionDto.CollectionResponse updateCollection(Long userId, Long collectionId,
                                                             CollectionDto.UpdateCollectionRequest request) {
        Collection collection = collectionRepository.findByCollectionIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new RuntimeException("Collection not found"));

        if (request.getName() != null) {
            collection.setName(request.getName());
        }

        if (request.getDescription() != null) {
            collection.setDescription(request.getDescription());
        }

        Collection updatedCollection = collectionRepository.save(collection);
        log.info("Updated collection: {} for user: {}", updatedCollection.getName(), userId);

        return mapToCollectionResponse(updatedCollection);
    }

    @Transactional
    public void deleteCollection(Long userId, Long collectionId) {
        Collection collection = collectionRepository.findByCollectionIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new RuntimeException("Collection not found"));

        collectionRepository.delete(collection);
        log.info("Deleted collection: {} for user: {}", collection.getName(), userId);
    }

    @Transactional
    public void addBookToCollection(Long userId, Long collectionId, Long bookId) {
        Collection collection = collectionRepository.findByCollectionIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new RuntimeException("Collection not found"));

        boolean bookExists = collectionBookRepository.findByCollectionCollectionIdAndBookId(collectionId, bookId).isPresent();

        if (bookExists) {
            throw new RuntimeException("Book already exists in this collection");
        }

        CollectionBook collectionBook = new CollectionBook();
        collectionBook.setCollection(collection);
        collectionBook.setBookId(bookId);

        collectionBookRepository.save(collectionBook);
        log.info("Added book: {} to collection: {} for user: {}", bookId, collectionId, userId);
    }

    @Transactional
    public void removeBookFromCollection(Long userId, Long collectionId, Long bookId) {
        Collection collection = collectionRepository.findByCollectionIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new RuntimeException("Collection not found"));

        CollectionBook collectionBook = collectionBookRepository.findByCollectionCollectionIdAndBookId(collectionId, bookId)
                .orElseThrow(() -> new RuntimeException("Book not found in this collection"));

        collectionBookRepository.delete(collectionBook);
        log.info("Removed book: {} from collection: {} for user: {}", bookId, collectionId, userId);
    }

    public List<CollectionDto.BookSummary> getBooksInCollection(Long userId, Long collectionId) {
        Collection collection = collectionRepository.findByCollectionIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new RuntimeException("Collection not found"));

        List<CollectionBook> collectionBooks = collectionBookRepository.findByCollectionCollectionId(collectionId);

        List<CollectionDto.BookSummary> bookSummaries = new ArrayList<>();

        for (CollectionBook collectionBook : collectionBooks) {
            try {
                BookServiceClient.BookSummary bookSummary = bookServiceClient.getBookDetails(userId, collectionBook.getBookId());

                bookSummaries.add(new CollectionDto.BookSummary(
                        bookSummary.getBookId(),
                        bookSummary.getTitle(),
                        bookSummary.getPreviewImageUrl(),
                        collectionBook.getAddedAt()
                ));
            } catch (Exception e) {
                log.error("Error fetching book details for book ID: {}", collectionBook.getBookId(), e);
                bookSummaries.add(new CollectionDto.BookSummary(
                        collectionBook.getBookId(),
                        "Unknown Book",
                        null,
                        collectionBook.getAddedAt()
                ));
            }
        }

        return bookSummaries;
    }

    private CollectionDto.CollectionResponse mapToCollectionResponse(Collection collection) {
        return new CollectionDto.CollectionResponse(
                collection.getCollectionId(),
                collection.getName(),
                collection.getDescription(),
                collection.getCreatedAt(),
                collection.getUpdatedAt()
        );
    }
}