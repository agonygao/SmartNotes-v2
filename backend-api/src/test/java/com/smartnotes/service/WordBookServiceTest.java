package com.smartnotes.service;

import com.smartnotes.data.DefaultWordData;
import com.smartnotes.dto.ErrorCode;
import com.smartnotes.dto.WordBookRequest;
import com.smartnotes.dto.WordBookResponse;
import com.smartnotes.entity.Word;
import com.smartnotes.entity.WordBook;
import com.smartnotes.entity.WordBookType;
import com.smartnotes.exception.BusinessException;
import com.smartnotes.repository.WordBookRepository;
import com.smartnotes.repository.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    // ==================== createWordBook Tests ====================

    @Nested
    @DisplayName("createWordBook()")
    class CreateWordBookTests {

        @Test
        @DisplayName("should create a new CUSTOM word book")
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

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("New Book");
            assertThat(result.getDescription()).isEqualTo("A new word book");
            assertThat(result.getType()).isEqualTo("CUSTOM");
            assertThat(result.getWordCount()).isEqualTo(0);
            assertThat(result.getIsDefault()).isFalse();

            verify(wordBookRepository).save(argThat(book ->
                    book.getUserId().equals(userId) &&
                    "New Book".equals(book.getName()) &&
                    book.getType() == WordBookType.CUSTOM &&
                    book.getWordCount() == 0 &&
                    Boolean.FALSE.equals(book.getIsDefault())
            ));
        }

        @Test
        @DisplayName("should default to CUSTOM type when type is null")
        void createWordBook_nullType() {
            WordBookRequest request = WordBookRequest.builder()
                    .name("Book Without Type")
                    .build();

            when(wordBookRepository.save(any(WordBook.class))).thenAnswer(invocation -> {
                WordBook book = invocation.getArgument(0);
                book.setId(2L);
                book.setCreatedAt(LocalDateTime.now());
                book.setUpdatedAt(LocalDateTime.now());
                return book;
            });

            WordBookResponse result = wordBookService.createWordBook(userId, request);

            assertThat(result.getType()).isEqualTo("CUSTOM");
        }

        @Test
        @DisplayName("should default to CUSTOM type when type is blank")
        void createWordBook_blankType() {
            WordBookRequest request = WordBookRequest.builder()
                    .name("Book Blank Type")
                    .type("  ")
                    .build();

            when(wordBookRepository.save(any(WordBook.class))).thenAnswer(invocation -> {
                WordBook book = invocation.getArgument(0);
                book.setId(3L);
                book.setCreatedAt(LocalDateTime.now());
                book.setUpdatedAt(LocalDateTime.now());
                return book;
            });

            WordBookResponse result = wordBookService.createWordBook(userId, request);

            assertThat(result.getType()).isEqualTo("CUSTOM");
        }

        @Test
        @DisplayName("should default to CUSTOM type when type is invalid")
        void createWordBook_invalidType() {
            WordBookRequest request = WordBookRequest.builder()
                    .name("Book Invalid Type")
                    .type("INVALID_TYPE")
                    .build();

            when(wordBookRepository.save(any(WordBook.class))).thenAnswer(invocation -> {
                WordBook book = invocation.getArgument(0);
                book.setId(4L);
                book.setCreatedAt(LocalDateTime.now());
                book.setUpdatedAt(LocalDateTime.now());
                return book;
            });

            WordBookResponse result = wordBookService.createWordBook(userId, request);

            assertThat(result.getType()).isEqualTo("CUSTOM");
        }
    }

    // ==================== updateWordBook Tests ====================

    @Nested
    @DisplayName("updateWordBook()")
    class UpdateWordBookTests {

        @Test
        @DisplayName("should update an existing word book")
        void updateWordBook_success() {
            WordBookRequest request = WordBookRequest.builder()
                    .name("Updated Book Name")
                    .description("Updated Description")
                    .build();

            when(wordBookRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                    .thenReturn(Optional.of(existingBook));
            when(wordBookRepository.save(any(WordBook.class))).thenAnswer(invocation -> invocation.getArgument(0));

            WordBookResponse result = wordBookService.updateWordBook(userId, 10L, request);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Updated Book Name");
            assertThat(result.getDescription()).isEqualTo("Updated Description");
            assertThat(result.getVersion()).isEqualTo(2);

            verify(wordBookRepository).findByIdAndUserIdAndDeletedFalse(10L, userId);
            verify(wordBookRepository).save(any(WordBook.class));
        }

        @Test
        @DisplayName("should enforce ownership - throw when book belongs to another user")
        void updateWordBook_ownershipCheck() {
            Long otherUserId = 2L;
            WordBookRequest request = WordBookRequest.builder()
                    .name("Hacked Name")
                    .build();

            when(wordBookRepository.findByIdAndUserIdAndDeletedFalse(10L, otherUserId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> wordBookService.updateWordBook(otherUserId, 10L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.WORD_BOOK_NOT_FOUND);

            verify(wordBookRepository, never()).save(any(WordBook.class));
        }

        @Test
        @DisplayName("should throw WORD_BOOK_NOT_FOUND for non-existent book")
        void updateWordBook_notFound() {
            WordBookRequest request = WordBookRequest.builder()
                    .name("Updated Name")
                    .build();

            when(wordBookRepository.findByIdAndUserIdAndDeletedFalse(999L, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> wordBookService.updateWordBook(userId, 999L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.WORD_BOOK_NOT_FOUND);

            verify(wordBookRepository, never()).save(any(WordBook.class));
        }

        @Test
        @DisplayName("should only update name when only name is provided")
        void updateWordBook_partialUpdate_nameOnly() {
            WordBookRequest request = WordBookRequest.builder()
                    .name("New Name Only")
                    .build();

            when(wordBookRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                    .thenReturn(Optional.of(existingBook));
            when(wordBookRepository.save(any(WordBook.class))).thenAnswer(invocation -> invocation.getArgument(0));

            WordBookResponse result = wordBookService.updateWordBook(userId, 10L, request);

            assertThat(result.getName()).isEqualTo("New Name Only");
            assertThat(result.getDescription()).isEqualTo("A custom word book"); // unchanged
        }

        @Test
        @DisplayName("should update type when type is valid")
        void updateWordBook_validTypeChange() {
            WordBookRequest request = WordBookRequest.builder()
                    .type("CET4")
                    .build();

            when(wordBookRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                    .thenReturn(Optional.of(existingBook));
            when(wordBookRepository.save(any(WordBook.class))).thenAnswer(invocation -> invocation.getArgument(0));

            WordBookResponse result = wordBookService.updateWordBook(userId, 10L, request);

            assertThat(result.getType()).isEqualTo("CET4");
        }

        @Test
        @DisplayName("should keep existing type when type is invalid in update")
        void updateWordBook_invalidTypeInUpdate() {
            WordBookRequest request = WordBookRequest.builder()
                    .type("INVALID")
                    .build();

            when(wordBookRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                    .thenReturn(Optional.of(existingBook));
            when(wordBookRepository.save(any(WordBook.class))).thenAnswer(invocation -> invocation.getArgument(0));

            WordBookResponse result = wordBookService.updateWordBook(userId, 10L, request);

            assertThat(result.getType()).isEqualTo("CUSTOM"); // unchanged, default fallback
        }
    }

    // ==================== deleteWordBook Tests ====================

    @Nested
    @DisplayName("deleteWordBook()")
    class DeleteWordBookTests {

        @Test
        @DisplayName("should soft delete a custom word book")
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
        @DisplayName("should throw BAD_REQUEST when trying to delete CET4 default book")
        void deleteWordBook_defaultCet4() {
            existingBook.setType(WordBookType.CET4);
            existingBook.setIsDefault(true);

            when(wordBookRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                    .thenReturn(Optional.of(existingBook));

            assertThatThrownBy(() -> wordBookService.deleteWordBook(userId, 10L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.BAD_REQUEST);

            assertThatThrownBy(() -> wordBookService.deleteWordBook(userId, 10L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getDetail()).contains("CET4").contains("CET6");
                    });

            verify(wordBookRepository, never()).save(any(WordBook.class));
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when trying to delete CET6 default book")
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

            assertThatThrownBy(() -> wordBookService.deleteWordBook(userId, 20L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.BAD_REQUEST);
        }

        @Test
        @DisplayName("should throw WORD_BOOK_NOT_FOUND for non-existent book")
        void deleteWordBook_notFound() {
            when(wordBookRepository.findByIdAndUserIdAndDeletedFalse(999L, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> wordBookService.deleteWordBook(userId, 999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.WORD_BOOK_NOT_FOUND);
        }
    }

    // ==================== getWordBook Tests ====================

    @Nested
    @DisplayName("getWordBook()")
    class GetWordBookTests {

        @Test
        @DisplayName("should return word book for valid ID")
        void getWordBook_success() {
            when(wordBookRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                    .thenReturn(Optional.of(existingBook));

            WordBookResponse result = wordBookService.getWordBook(userId, 10L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getName()).isEqualTo("My Word Book");
            assertThat(result.getDescription()).isEqualTo("A custom word book");
            assertThat(result.getType()).isEqualTo("CUSTOM");
            assertThat(result.getWordCount()).isEqualTo(50);
        }

        @Test
        @DisplayName("should throw WORD_BOOK_NOT_FOUND for non-existent book")
        void getWordBook_notFound() {
            when(wordBookRepository.findByIdAndUserIdAndDeletedFalse(999L, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> wordBookService.getWordBook(userId, 999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.WORD_BOOK_NOT_FOUND);
        }
    }

    // ==================== listWordBooks Tests ====================

    @Nested
    @DisplayName("listWordBooks()")
    class ListWordBooksTests {

        @Test
        @DisplayName("should return list of word books for user")
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

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("My Word Book");
            assertThat(result.get(1).getName()).isEqualTo("Second Book");
        }

        @Test
        @DisplayName("should return empty list when no books exist")
        void listWordBooks_empty() {
            when(wordBookRepository.findByUserIdAndDeletedFalse(userId))
                    .thenReturn(List.of());

            List<WordBookResponse> result = wordBookService.listWordBooks(userId);

            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }
    }

    // ==================== initializeDefaultWordBooks Tests ====================

    @Nested
    @DisplayName("initializeDefaultWordBooks()")
    class InitializeDefaultWordBooksTests {

        @Test
        @DisplayName("should create both CET4 and CET6 default books when they don't exist")
        void initializeDefaultWordBooks_bothNew() {
            Word cet4Word = new Word();
            cet4Word.setId(1L);
            cet4Word.setWord("abandon");

            Word cet6Word = new Word();
            cet6Word.setId(2L);
            cet6Word.setWord("abnormal");

            when(wordBookRepository.existsByTypeAndIsDefaultTrue(WordBookType.CET4)).thenReturn(false);
            when(wordBookRepository.existsByTypeAndIsDefaultTrue(WordBookType.CET6)).thenReturn(false);
            when(wordBookRepository.save(any(WordBook.class))).thenAnswer(invocation -> {
                WordBook book = invocation.getArgument(0);
                if (book.getId() == null) book.setId(1L);
                book.setCreatedAt(LocalDateTime.now());
                book.setUpdatedAt(LocalDateTime.now());
                return book;
            });
            when(defaultWordData.getCet4Words()).thenReturn(List.of(cet4Word));
            when(defaultWordData.getCet6Words()).thenReturn(List.of(cet6Word));
            when(wordRepository.saveAll(anyList())).thenReturn(List.of(cet4Word, cet6Word));

            List<WordBookResponse> result = wordBookService.initializeDefaultWordBooks(userId);

            assertThat(result).hasSize(2);

            verify(wordBookRepository).existsByTypeAndIsDefaultTrue(WordBookType.CET4);
            verify(wordBookRepository).existsByTypeAndIsDefaultTrue(WordBookType.CET6);
            verify(wordRepository, times(2)).saveAll(anyList());
        }

        @Test
        @DisplayName("should return existing books when they already exist")
        void initializeDefaultWordBooks_alreadyExist() {
            WordBook existingCet4 = new WordBook();
            existingCet4.setId(100L);
            existingCet4.setName("CET-4 Core Vocabulary");
            existingCet4.setType(WordBookType.CET4);
            existingCet4.setIsDefault(true);

            WordBook existingCet6 = new WordBook();
            existingCet6.setId(101L);
            existingCet6.setName("CET-6 Core Vocabulary");
            existingCet6.setType(WordBookType.CET6);
            existingCet6.setIsDefault(true);

            when(wordBookRepository.existsByTypeAndIsDefaultTrue(WordBookType.CET4)).thenReturn(true);
            when(wordBookRepository.existsByTypeAndIsDefaultTrue(WordBookType.CET6)).thenReturn(true);
            when(wordBookRepository.findByTypeAndIsDefaultTrue(WordBookType.CET4))
                    .thenReturn(Optional.of(existingCet4));
            when(wordBookRepository.findByTypeAndIsDefaultTrue(WordBookType.CET6))
                    .thenReturn(Optional.of(existingCet6));

            List<WordBookResponse> result = wordBookService.initializeDefaultWordBooks(userId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("CET-4 Core Vocabulary");
            assertThat(result.get(1).getName()).isEqualTo("CET-6 Core Vocabulary");

            verify(wordRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("should create only CET6 when CET4 already exists")
        void initializeDefaultWordBooks_cet4Exists_cet6New() {
            WordBook existingCet4 = new WordBook();
            existingCet4.setId(100L);
            existingCet4.setName("CET-4 Core Vocabulary");
            existingCet4.setType(WordBookType.CET4);
            existingCet4.setIsDefault(true);

            Word cet6Word = new Word();
            cet6Word.setId(3L);
            cet6Word.setWord("abnormal");

            when(wordBookRepository.existsByTypeAndIsDefaultTrue(WordBookType.CET4)).thenReturn(true);
            when(wordBookRepository.existsByTypeAndIsDefaultTrue(WordBookType.CET6)).thenReturn(false);
            when(wordBookRepository.findByTypeAndIsDefaultTrue(WordBookType.CET4))
                    .thenReturn(Optional.of(existingCet4));
            when(wordBookRepository.save(any(WordBook.class))).thenAnswer(invocation -> {
                WordBook book = invocation.getArgument(0);
                if (book.getId() == null) book.setId(101L);
                book.setCreatedAt(LocalDateTime.now());
                book.setUpdatedAt(LocalDateTime.now());
                return book;
            });
            when(defaultWordData.getCet6Words()).thenReturn(List.of(cet6Word));
            when(wordRepository.saveAll(anyList())).thenReturn(List.of(cet6Word));

            List<WordBookResponse> result = wordBookService.initializeDefaultWordBooks(userId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getType()).isEqualTo("CET4");
            assertThat(result.get(1).getType()).isEqualTo("CET6");

            verify(wordRepository, times(1)).saveAll(anyList());
            verify(defaultWordData, never()).getCet4Words();
        }

        @Test
        @DisplayName("should create only CET4 when CET6 already exists")
        void initializeDefaultWordBooks_cet4New_cet6Exists() {
            WordBook existingCet6 = new WordBook();
            existingCet6.setId(101L);
            existingCet6.setName("CET-6 Core Vocabulary");
            existingCet6.setType(WordBookType.CET6);
            existingCet6.setIsDefault(true);

            Word cet4Word = new Word();
            cet4Word.setId(4L);
            cet4Word.setWord("abandon");

            when(wordBookRepository.existsByTypeAndIsDefaultTrue(WordBookType.CET4)).thenReturn(false);
            when(wordBookRepository.existsByTypeAndIsDefaultTrue(WordBookType.CET6)).thenReturn(true);
            when(wordBookRepository.findByTypeAndIsDefaultTrue(WordBookType.CET6))
                    .thenReturn(Optional.of(existingCet6));
            when(wordBookRepository.save(any(WordBook.class))).thenAnswer(invocation -> {
                WordBook book = invocation.getArgument(0);
                if (book.getId() == null) book.setId(100L);
                book.setCreatedAt(LocalDateTime.now());
                book.setUpdatedAt(LocalDateTime.now());
                return book;
            });
            when(defaultWordData.getCet4Words()).thenReturn(List.of(cet4Word));
            when(wordRepository.saveAll(anyList())).thenReturn(List.of(cet4Word));

            List<WordBookResponse> result = wordBookService.initializeDefaultWordBooks(userId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getType()).isEqualTo("CET4");
            assertThat(result.get(1).getType()).isEqualTo("CET6");

            verify(wordRepository, times(1)).saveAll(anyList());
            verify(defaultWordData, never()).getCet6Words();
        }
    }

    // ==================== getDefaultWordBooks Tests ====================

    @Nested
    @DisplayName("getDefaultWordBooks()")
    class GetDefaultWordBooksTests {

        @Test
        @DisplayName("should return default CET4 and CET6 books")
        void getDefaultWordBooks_success() {
            WordBook cet4 = new WordBook();
            cet4.setId(100L);
            cet4.setName("CET-4 Core Vocabulary");
            cet4.setType(WordBookType.CET4);
            cet4.setIsDefault(true);

            WordBook cet6 = new WordBook();
            cet6.setId(101L);
            cet6.setName("CET-6 Core Vocabulary");
            cet6.setType(WordBookType.CET6);
            cet6.setIsDefault(true);

            when(wordBookRepository.findByTypeAndIsDefaultTrue(WordBookType.CET4))
                    .thenReturn(Optional.of(cet4));
            when(wordBookRepository.findByTypeAndIsDefaultTrue(WordBookType.CET6))
                    .thenReturn(Optional.of(cet6));

            List<WordBookResponse> result = wordBookService.getDefaultWordBooks();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getType()).isEqualTo("CET4");
            assertThat(result.get(1).getType()).isEqualTo("CET6");
        }

        @Test
        @DisplayName("should return empty list when no default books exist")
        void getDefaultWordBooks_empty() {
            when(wordBookRepository.findByTypeAndIsDefaultTrue(WordBookType.CET4))
                    .thenReturn(Optional.empty());
            when(wordBookRepository.findByTypeAndIsDefaultTrue(WordBookType.CET6))
                    .thenReturn(Optional.empty());

            List<WordBookResponse> result = wordBookService.getDefaultWordBooks();

            assertThat(result).isEmpty();
        }
    }
}
