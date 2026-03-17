package com.smartnotes.service;

import com.smartnotes.data.DefaultWordData;
import com.smartnotes.dto.ErrorCode;
import com.smartnotes.dto.WordBookRequest;
import com.smartnotes.dto.WordBookResponse;
import com.smartnotes.entity.WordBook;
import com.smartnotes.entity.WordBookType;
import com.smartnotes.exception.BusinessException;
import com.smartnotes.repository.WordBookRepository;
import com.smartnotes.repository.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WordBookService Unit Tests")
class WordBookServiceTest {

    @Mock
    private WordBookRepository wordBookRepository;

    @Mock
    private WordRepository wordRepository;

    @Mock
    private DefaultWordData defaultWordData;

    @InjectMocks
    private WordBookService wordBookService;

    private Long userId = 1L;
    private WordBook existingBook;

    @BeforeEach
    void setUp() {
        existingBook = new WordBook();
        existingBook.setId(10L);
        existingBook.setUserId(userId);
        existingBook.setName("My Word Book");
        existingBook.setDescription("A custom word book");
        existingBook.setType(WordBookType.CUSTOM);
        existingBook.setWordCount(50);
        existingBook.setIsDefault(false);
        existingBook.setVersion(1);
        existingBook.setDeleted(false);
        existingBook.setCreatedAt(LocalDateTime.now());
        existingBook.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("createWordBook - should create a new word book successfully")
    void createWordBook_success() {
        WordBookRequest request = WordBookRequest.builder()
                .name("New Book")
                .description("A new word book")
                .type("CUSTOM")
                .build();

        when(wordBookRepository.save(any(WordBook.class))).thenAnswer(invocation -> {
            WordBook book = invocation.getArgument(0);
            book.setId(1L);
            book.setCreatedAt(LocalDateTime.now());
            book.setUpdatedAt(LocalDateTime.now());
            return book;
        });

        WordBookResponse result = wordBookService.createWordBook(userId, request);

        assertNotNull(result);
        assertEquals("New Book", result.getName());
        assertEquals("A new word book", result.getDescription());
        assertEquals("CUSTOM", result.getType());
        assertEquals(0, result.getWordCount());
        assertFalse(result.getIsDefault());

        verify(wordBookRepository).save(argThat(book ->
                book.getUserId().equals(userId) &&
                "New Book".equals(book.getName()) &&
                book.getType() == WordBookType.CUSTOM &&
                book.getWordCount() == 0 &&
                Boolean.FALSE.equals(book.getIsDefault())
        ));
    }

    @Test
    @DisplayName("updateWordBook - should update an existing word book")
    void updateWordBook_success() {
        WordBookRequest request = WordBookRequest.builder()
                .name("Updated Book Name")
                .description("Updated Description")
                .build();

        when(wordBookRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                .thenReturn(Optional.of(existingBook));
        when(wordBookRepository.save(any(WordBook.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WordBookResponse result = wordBookService.updateWordBook(userId, 10L, request);

        assertNotNull(result);
        assertEquals("Updated Book Name", result.getName());
        assertEquals("Updated Description", result.getDescription());
        assertEquals(2, result.getVersion(), "Version should be incremented on update");

        verify(wordBookRepository).findByIdAndUserIdAndDeletedFalse(10L, userId);
        verify(wordBookRepository).save(any(WordBook.class));
    }

    @Test
    @DisplayName("updateWordBook - should throw exception when word book not found")
    void updateWordBook_notFound() {
        WordBookRequest request = WordBookRequest.builder()
                .name("Updated Name")
                .build();

        when(wordBookRepository.findByIdAndUserIdAndDeletedFalse(999L, userId))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> wordBookService.updateWordBook(userId, 999L, request));

        assertEquals(ErrorCode.WORD_BOOK_NOT_FOUND, exception.getCode());
        verify(wordBookRepository, never()).save(any(WordBook.class));
    }

    @Test
    @DisplayName("deleteWordBook - should soft delete a custom word book")
    void deleteWordBook_success() {
        when(wordBookRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                .thenReturn(Optional.of(existingBook));
        when(wordBookRepository.save(any(WordBook.class))).thenAnswer(invocation -> invocation.getArgument(0));

        wordBookService.deleteWordBook(userId, 10L);

        verify(wordBookRepository).save(argThat(book ->
                Boolean.TRUE.equals(book.getDeleted())
        ));
    }

    @Test
    @DisplayName("deleteWordBook - should throw exception when trying to delete default CET4 book")
    void deleteWordBook_defaultCet4() {
        existingBook.setType(WordBookType.CET4);
        existingBook.setIsDefault(true);

        when(wordBookRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                .thenReturn(Optional.of(existingBook));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> wordBookService.deleteWordBook(userId, 10L));

        assertEquals(ErrorCode.BAD_REQUEST, exception.getCode());
        assertNotNull(exception.getDetail());
        assertTrue(exception.getDetail().contains("CET4") || exception.getDetail().contains("CET6"),
                "Error detail should mention CET4/CET6");

        verify(wordBookRepository, never()).save(any(WordBook.class));
    }

    @Test
    @DisplayName("deleteWordBook - should throw exception when trying to delete default CET6 book")
    void deleteWordBook_defaultCet6() {
        WordBook cet6Book = new WordBook();
        cet6Book.setId(20L);
        cet6Book.setUserId(userId);
        cet6Book.setName("CET-6 Core Vocabulary");
        cet6Book.setType(WordBookType.CET6);
        cet6Book.setIsDefault(true);
        cet6Book.setDeleted(false);
        cet6Book.setVersion(1);

        when(wordBookRepository.findByIdAndUserIdAndDeletedFalse(20L, userId))
                .thenReturn(Optional.of(cet6Book));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> wordBookService.deleteWordBook(userId, 20L));

        assertEquals(ErrorCode.BAD_REQUEST, exception.getCode());
    }

    @Test
    @DisplayName("getWordBook - should return word book response for valid ID")
    void getWordBook_success() {
        when(wordBookRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                .thenReturn(Optional.of(existingBook));

        WordBookResponse result = wordBookService.getWordBook(userId, 10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(userId, result.getUserId());
        assertEquals("My Word Book", result.getName());
        assertEquals("A custom word book", result.getDescription());
        assertEquals("CUSTOM", result.getType());
        assertEquals(50, result.getWordCount());

        verify(wordBookRepository).findByIdAndUserIdAndDeletedFalse(10L, userId);
    }

    @Test
    @DisplayName("getWordBook - should throw exception when word book not found")
    void getWordBook_notFound() {
        when(wordBookRepository.findByIdAndUserIdAndDeletedFalse(999L, userId))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> wordBookService.getWordBook(userId, 999L));

        assertEquals(ErrorCode.WORD_BOOK_NOT_FOUND, exception.getCode());
    }

    @Test
    @DisplayName("listWordBooks - should return list of word books for user")
    void listWordBooks_success() {
        WordBook book2 = new WordBook();
        book2.setId(11L);
        book2.setUserId(userId);
        book2.setName("Second Book");
        book2.setType(WordBookType.CUSTOM);
        book2.setWordCount(30);
        book2.setIsDefault(false);
        book2.setDeleted(false);
        book2.setVersion(1);
        book2.setCreatedAt(LocalDateTime.now());
        book2.setUpdatedAt(LocalDateTime.now());

        when(wordBookRepository.findByUserIdAndDeletedFalse(userId))
                .thenReturn(List.of(existingBook, book2));

        List<WordBookResponse> result = wordBookService.listWordBooks(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("My Word Book", result.get(0).getName());
        assertEquals("Second Book", result.get(1).getName());

        verify(wordBookRepository).findByUserIdAndDeletedFalse(userId);
    }

    @Test
    @DisplayName("listWordBooks - should return empty list when no books exist")
    void listWordBooks_empty() {
        when(wordBookRepository.findByUserIdAndDeletedFalse(userId))
                .thenReturn(List.of());

        List<WordBookResponse> result = wordBookService.listWordBooks(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(wordBookRepository).findByUserIdAndDeletedFalse(userId);
    }
}
