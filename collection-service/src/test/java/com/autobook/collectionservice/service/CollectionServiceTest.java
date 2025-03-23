package com.autobook.collectionservice.service;

import com.autobook.collectionservice.client.BookServiceClient;
import com.autobook.collectionservice.dto.CollectionDto;
import com.autobook.collectionservice.entity.Collection;
import com.autobook.collectionservice.entity.CollectionBook;
import com.autobook.collectionservice.repository.CollectionBookRepository;
import com.autobook.collectionservice.repository.CollectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

    @Mock
    private CollectionRepository collectionRepository;

    @Mock
    private CollectionBookRepository collectionBookRepository;

    @Mock
    private BookServiceClient bookServiceClient;

    @InjectMocks
    private CollectionService collectionService;

    private Collection collection;
    private CollectionBook collectionBook;
    private Long userId;
    private Long bookId;
    private CollectionDto.CreateCollectionRequest createRequest;
    private CollectionDto.UpdateCollectionRequest updateRequest;
    private BookServiceClient.BookSummary bookSummary;

    @BeforeEach
    void setUp() {
        userId = 1L;
        bookId = 1L;

        collection = new Collection();
        collection.setCollectionId(1L);
        collection.setUserId(userId);
        collection.setName("Test Collection");
        collection.setDescription("Test Description");
        collection.setCreatedAt(LocalDateTime.now());
        collection.setUpdatedAt(LocalDateTime.now());

        collectionBook = new CollectionBook();
        collectionBook.setCollectionBookId(1L);
        collectionBook.setCollection(collection);
        collectionBook.setBookId(bookId);
        collectionBook.setAddedAt(LocalDateTime.now());

        createRequest = new CollectionDto.CreateCollectionRequest();
        createRequest.setName("Test Collection");
        createRequest.setDescription("Test Description");

        updateRequest = new CollectionDto.UpdateCollectionRequest();
        updateRequest.setName("Updated Collection");
        updateRequest.setDescription("Updated Description");

        bookSummary = new BookServiceClient.BookSummary();
        bookSummary.setBookId(bookId);
        bookSummary.setTitle("Test Book");
        bookSummary.setPreviewImageUrl("http://example.com/image.jpg");
    }

    @Test
    void createCollection_Success() {
        when(collectionRepository.save(any(Collection.class))).thenReturn(collection);

        CollectionDto.CollectionResponse result = collectionService.createCollection(userId, createRequest);

        assertThat(result).isNotNull();
        assertEquals(collection.getCollectionId(), result.getCollectionId());
        assertEquals(collection.getName(), result.getName());
        assertEquals(collection.getDescription(), result.getDescription());
        assertEquals(collection.getCreatedAt(), result.getCreatedAt());
        assertEquals(collection.getUpdatedAt(), result.getUpdatedAt());

        verify(collectionRepository).save(any(Collection.class));
    }

    @Test
    void getUserCollections_Success() {
        List<Collection> collections = Arrays.asList(collection);
        when(collectionRepository.findByUserId(anyLong())).thenReturn(collections);

        List<CollectionDto.CollectionResponse> result = collectionService.getUserCollections(userId);

        assertThat(result).isNotNull();
        assertEquals(1, result.size());
        assertEquals(collection.getCollectionId(), result.get(0).getCollectionId());
        assertEquals(collection.getName(), result.get(0).getName());
        assertEquals(collection.getDescription(), result.get(0).getDescription());
        assertEquals(collection.getCreatedAt(), result.get(0).getCreatedAt());
        assertEquals(collection.getUpdatedAt(), result.get(0).getUpdatedAt());

        verify(collectionRepository).findByUserId(userId);
    }

    @Test
    void getUserCollections_EmptyList() {
        when(collectionRepository.findByUserId(anyLong())).thenReturn(List.of());

        List<CollectionDto.CollectionResponse> result = collectionService.getUserCollections(userId);

        assertThat(result).isNotNull();
        assertEquals(0, result.size());

        verify(collectionRepository).findByUserId(userId);
    }

    @Test
    void getCollectionDetails_Success() {
        List<CollectionBook> collectionBooks = Arrays.asList(collectionBook);
        when(collectionRepository.findByCollectionIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(collection));
        when(collectionBookRepository.findByCollectionCollectionId(anyLong())).thenReturn(collectionBooks);
        when(bookServiceClient.getBookDetails(anyLong(), anyLong())).thenReturn(bookSummary);

        CollectionDto.CollectionDetailResponse result = collectionService.getCollectionDetails(userId, collection.getCollectionId());

        assertThat(result).isNotNull();
        assertEquals(collection.getCollectionId(), result.getCollectionId());
        assertEquals(collection.getName(), result.getName());
        assertEquals(collection.getDescription(), result.getDescription());
        assertEquals(collection.getCreatedAt(), result.getCreatedAt());
        assertEquals(collection.getUpdatedAt(), result.getUpdatedAt());
        assertEquals(1, result.getBooks().size());
        assertEquals(bookId, result.getBooks().get(0).getBookId());
        assertEquals(bookSummary.getTitle(), result.getBooks().get(0).getTitle());
        assertEquals(bookSummary.getPreviewImageUrl(), result.getBooks().get(0).getPreviewImageUrl());
        assertEquals(collectionBook.getAddedAt(), result.getBooks().get(0).getAddedAt());

        verify(collectionRepository).findByCollectionIdAndUserId(collection.getCollectionId(), userId);
        verify(collectionBookRepository).findByCollectionCollectionId(collection.getCollectionId());
        verify(bookServiceClient).getBookDetails(userId, bookId);
    }

    @Test
    void getCollectionDetails_CollectionNotFound() {
        when(collectionRepository.findByCollectionIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                collectionService.getCollectionDetails(userId, 999L)
        );
        assertEquals("Collection not found", exception.getMessage());

        verify(collectionRepository).findByCollectionIdAndUserId(999L, userId);
        verify(collectionBookRepository, never()).findByCollectionCollectionId(anyLong());
    }

    @Test
    void getCollectionDetails_ErrorFetchingBookDetails() {
        List<CollectionBook> collectionBooks = Arrays.asList(collectionBook);
        when(collectionRepository.findByCollectionIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(collection));
        when(collectionBookRepository.findByCollectionCollectionId(anyLong())).thenReturn(collectionBooks);
        when(bookServiceClient.getBookDetails(anyLong(), anyLong())).thenThrow(new RuntimeException("Service unavailable"));

        CollectionDto.CollectionDetailResponse result = collectionService.getCollectionDetails(userId, collection.getCollectionId());

        assertThat(result).isNotNull();
        assertEquals(collection.getCollectionId(), result.getCollectionId());
        assertEquals(collection.getName(), result.getName());
        assertEquals(collection.getDescription(), result.getDescription());
        assertEquals(1, result.getBooks().size());
        assertEquals(bookId, result.getBooks().get(0).getBookId());
        assertEquals("Unknown Book", result.getBooks().get(0).getTitle());
        assertEquals(null, result.getBooks().get(0).getPreviewImageUrl());
        assertEquals(collectionBook.getAddedAt(), result.getBooks().get(0).getAddedAt());

        verify(collectionRepository).findByCollectionIdAndUserId(collection.getCollectionId(), userId);
        verify(collectionBookRepository).findByCollectionCollectionId(collection.getCollectionId());
        verify(bookServiceClient).getBookDetails(userId, bookId);
    }

    @Test
    void updateCollection_Success() {
        when(collectionRepository.findByCollectionIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(collection));
        when(collectionRepository.save(any(Collection.class))).thenReturn(collection);

        CollectionDto.CollectionResponse result = collectionService.updateCollection(userId, collection.getCollectionId(), updateRequest);

        assertThat(result).isNotNull();
        assertEquals(collection.getCollectionId(), result.getCollectionId());
        assertEquals(collection.getName(), result.getName());
        assertEquals(collection.getDescription(), result.getDescription());
        assertEquals(collection.getCreatedAt(), result.getCreatedAt());
        assertEquals(collection.getUpdatedAt(), result.getUpdatedAt());

        verify(collectionRepository).findByCollectionIdAndUserId(collection.getCollectionId(), userId);
        verify(collectionRepository).save(collection);
    }

    @Test
    void updateCollection_PartialUpdate() {
        updateRequest.setName(null);
        when(collectionRepository.findByCollectionIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(collection));
        when(collectionRepository.save(any(Collection.class))).thenReturn(collection);

        collectionService.updateCollection(userId, collection.getCollectionId(), updateRequest);

        verify(collectionRepository).findByCollectionIdAndUserId(collection.getCollectionId(), userId);
        verify(collectionRepository).save(collection);
    }

    @Test
    void updateCollection_CollectionNotFound() {
        when(collectionRepository.findByCollectionIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                collectionService.updateCollection(userId, 999L, updateRequest)
        );
        assertEquals("Collection not found", exception.getMessage());

        verify(collectionRepository).findByCollectionIdAndUserId(999L, userId);
        verify(collectionRepository, never()).save(any(Collection.class));
    }

    @Test
    void deleteCollection_Success() {
        when(collectionRepository.findByCollectionIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(collection));
        doNothing().when(collectionRepository).delete(any(Collection.class));

        collectionService.deleteCollection(userId, collection.getCollectionId());

        verify(collectionRepository).findByCollectionIdAndUserId(collection.getCollectionId(), userId);
        verify(collectionRepository).delete(collection);
    }

    @Test
    void deleteCollection_CollectionNotFound() {
        when(collectionRepository.findByCollectionIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                collectionService.deleteCollection(userId, 999L)
        );
        assertEquals("Collection not found", exception.getMessage());

        verify(collectionRepository).findByCollectionIdAndUserId(999L, userId);
        verify(collectionRepository, never()).delete(any(Collection.class));
    }

    @Test
    void addBookToCollection_Success() {
        when(collectionRepository.findByCollectionIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(collection));
        when(collectionBookRepository.findByCollectionCollectionIdAndBookId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(collectionBookRepository.save(any(CollectionBook.class))).thenReturn(collectionBook);

        collectionService.addBookToCollection(userId, collection.getCollectionId(), bookId);

        verify(collectionRepository).findByCollectionIdAndUserId(collection.getCollectionId(), userId);
        verify(collectionBookRepository).findByCollectionCollectionIdAndBookId(collection.getCollectionId(), bookId);
        verify(collectionBookRepository).save(any(CollectionBook.class));
    }

    @Test
    void addBookToCollection_CollectionNotFound() {
        when(collectionRepository.findByCollectionIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                collectionService.addBookToCollection(userId, 999L, bookId)
        );
        assertEquals("Collection not found", exception.getMessage());

        verify(collectionRepository).findByCollectionIdAndUserId(999L, userId);
        verify(collectionBookRepository, never()).findByCollectionCollectionIdAndBookId(anyLong(), anyLong());
        verify(collectionBookRepository, never()).save(any(CollectionBook.class));
    }

    @Test
    void addBookToCollection_BookAlreadyExists() {
        when(collectionRepository.findByCollectionIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(collection));
        when(collectionBookRepository.findByCollectionCollectionIdAndBookId(anyLong(), anyLong())).thenReturn(Optional.of(collectionBook));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                collectionService.addBookToCollection(userId, collection.getCollectionId(), bookId)
        );
        assertEquals("Book already exists in this collection", exception.getMessage());

        verify(collectionRepository).findByCollectionIdAndUserId(collection.getCollectionId(), userId);
        verify(collectionBookRepository).findByCollectionCollectionIdAndBookId(collection.getCollectionId(), bookId);
        verify(collectionBookRepository, never()).save(any(CollectionBook.class));
    }

    @Test
    void removeBookFromCollection_Success() {
        when(collectionRepository.findByCollectionIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(collection));
        when(collectionBookRepository.findByCollectionCollectionIdAndBookId(anyLong(), anyLong())).thenReturn(Optional.of(collectionBook));
        doNothing().when(collectionBookRepository).delete(any(CollectionBook.class));

        collectionService.removeBookFromCollection(userId, collection.getCollectionId(), bookId);

        verify(collectionRepository).findByCollectionIdAndUserId(collection.getCollectionId(), userId);
        verify(collectionBookRepository).findByCollectionCollectionIdAndBookId(collection.getCollectionId(), bookId);
        verify(collectionBookRepository).delete(collectionBook);
    }

    @Test
    void removeBookFromCollection_CollectionNotFound() {
        when(collectionRepository.findByCollectionIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                collectionService.removeBookFromCollection(userId, 999L, bookId)
        );
        assertEquals("Collection not found", exception.getMessage());

        verify(collectionRepository).findByCollectionIdAndUserId(999L, userId);
        verify(collectionBookRepository, never()).findByCollectionCollectionIdAndBookId(anyLong(), anyLong());
        verify(collectionBookRepository, never()).delete(any(CollectionBook.class));
    }

    @Test
    void removeBookFromCollection_BookNotFound() {
        when(collectionRepository.findByCollectionIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(collection));
        when(collectionBookRepository.findByCollectionCollectionIdAndBookId(anyLong(), anyLong())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                collectionService.removeBookFromCollection(userId, collection.getCollectionId(), 999L)
        );
        assertEquals("Book not found in this collection", exception.getMessage());

        verify(collectionRepository).findByCollectionIdAndUserId(collection.getCollectionId(), userId);
        verify(collectionBookRepository).findByCollectionCollectionIdAndBookId(collection.getCollectionId(), 999L);
        verify(collectionBookRepository, never()).delete(any(CollectionBook.class));
    }

    @Test
    void getBooksInCollection_Success() {
        List<CollectionBook> collectionBooks = Arrays.asList(collectionBook);
        when(collectionRepository.findByCollectionIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(collection));
        when(collectionBookRepository.findByCollectionCollectionId(anyLong())).thenReturn(collectionBooks);
        when(bookServiceClient.getBookDetails(anyLong(), anyLong())).thenReturn(bookSummary);

        List<CollectionDto.BookSummary> result = collectionService.getBooksInCollection(userId, collection.getCollectionId());

        assertThat(result).isNotNull();
        assertEquals(1, result.size());
        assertEquals(bookId, result.get(0).getBookId());
        assertEquals(bookSummary.getTitle(), result.get(0).getTitle());
        assertEquals(bookSummary.getPreviewImageUrl(), result.get(0).getPreviewImageUrl());
        assertEquals(collectionBook.getAddedAt(), result.get(0).getAddedAt());

        verify(collectionRepository).findByCollectionIdAndUserId(collection.getCollectionId(), userId);
        verify(collectionBookRepository).findByCollectionCollectionId(collection.getCollectionId());
        verify(bookServiceClient).getBookDetails(userId, bookId);
    }

    @Test
    void getBooksInCollection_CollectionNotFound() {
        when(collectionRepository.findByCollectionIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                collectionService.getBooksInCollection(userId, 999L)
        );
        assertEquals("Collection not found", exception.getMessage());

        verify(collectionRepository).findByCollectionIdAndUserId(999L, userId);
        verify(collectionBookRepository, never()).findByCollectionCollectionId(anyLong());
    }

    @Test
    void getBooksInCollection_ErrorFetchingBookDetails() {
        List<CollectionBook> collectionBooks = Arrays.asList(collectionBook);
        when(collectionRepository.findByCollectionIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(collection));
        when(collectionBookRepository.findByCollectionCollectionId(anyLong())).thenReturn(collectionBooks);
        when(bookServiceClient.getBookDetails(anyLong(), anyLong())).thenThrow(new RuntimeException("Service unavailable"));

        List<CollectionDto.BookSummary> result = collectionService.getBooksInCollection(userId, collection.getCollectionId());

        assertThat(result).isNotNull();
        assertEquals(1, result.size());
        assertEquals(bookId, result.get(0).getBookId());
        assertEquals("Unknown Book", result.get(0).getTitle());
        assertEquals(null, result.get(0).getPreviewImageUrl());
        assertEquals(collectionBook.getAddedAt(), result.get(0).getAddedAt());

        verify(collectionRepository).findByCollectionIdAndUserId(collection.getCollectionId(), userId);
        verify(collectionBookRepository).findByCollectionCollectionId(collection.getCollectionId());
        verify(bookServiceClient).getBookDetails(userId, bookId);
    }
}