package com.smartnotes.service;

import com.smartnotes.dto.ErrorCode;
import com.smartnotes.dto.WordReviewRequest;
import com.smartnotes.dto.WordReviewResponse;
import com.smartnotes.dto.WordReviewResultRequest;
import com.smartnotes.entity.DictationRecord;
import com.smartnotes.entity.Word;
import com.smartnotes.entity.WordProgress;
import com.smartnotes.entity.WrongWord;
import com.smartnotes.exception.BusinessException;
import com.smartnotes.repository.DictationRecordRepository;
import com.smartnotes.repository.WordProgressRepository;
import com.smartnotes.repository.WordRepository;
import com.smartnotes.repository.WrongWordRepository;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WordReviewService Unit Tests")
class WordReviewServiceTest {

    @Mock
    private WordRepository wordRepository;

    @Mock
    private WordProgressRepository wordProgressRepository;

    @Mock
    private DictationRecordRepository dictationRecordRepository;

    @Mock
    private WrongWordRepository wrongWordRepository;

    @InjectMocks
    private WordReviewService wordReviewService;

    private Long userId = 1L;
    private Word testWord;

    @BeforeEach
    void setUp() {
        testWord = new Word();
        testWord.setId(100L);
        testWord.setBookId(10L);
        testWord.setWord("abandon");
        testWord.setPhonetic("/əˈbændən/");
        testWord.setMeaning("vt. 放弃，抛弃");
        testWord.setExampleSentence("He abandoned his wife and children.");
        testWord.setSortOrder(1);
        testWord.setDeleted(false);
    }

    private WordProgress createProgress(Long wordId, int masteryLevel, LocalDateTime nextReview) {
        WordProgress progress = new WordProgress();
        progress.setId(1L);
        progress.setUserId(userId);
        progress.setWordId(wordId);
        progress.setMasteryLevel(masteryLevel);
        progress.setReviewCount(3);
        progress.setCorrectCount(2);
        progress.setWrongCount(1);
        progress.setLastReviewedAt(LocalDateTime.now().minusDays(1));
        progress.setNextReviewAt(nextReview);
        return progress;
    }

    // ==================== getReviewWords Tests ====================

    @Nested
    @DisplayName("getReviewWords()")
    class GetReviewWordsTests {

        @Test
        @DisplayName("should return words by specific word IDs")
        void getReviewWords_byWordIds() {
            WordReviewRequest req = WordReviewRequest.builder()
                    .wordIds(List.of(100L, 101L))
                    .build();

            Word word2 = new Word();
            word2.setId(101L);
            word2.setWord("ability");
            word2.setDeleted(false);

            when(wordRepository.findAllById(List.of(100L, 101L)))
                    .thenReturn(List.of(testWord, word2));
            when(wordProgressRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.empty());
            when(wordProgressRepository.findByUserIdAndWordId(userId, 101L))
                    .thenReturn(Optional.empty());

            WordReviewResponse result = wordReviewService.getReviewWords(userId, req);

            assertThat(result.getWords()).hasSize(2);
            assertThat(result.getWords().get(0).getWord()).isEqualTo("abandon");
            assertThat(result.getWords().get(1).getWord()).isEqualTo("ability");
        }

        @Test
        @DisplayName("should filter out deleted words when fetching by IDs")
        void getReviewWords_byWordIds_filtersDeleted() {
            Word deletedWord = new Word();
            deletedWord.setId(101L);
            deletedWord.setWord("deleted");
            deletedWord.setDeleted(true);

            when(wordRepository.findAllById(List.of(100L, 101L)))
                    .thenReturn(List.of(testWord, deletedWord));
            when(wordProgressRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.empty());

            WordReviewRequest req = WordReviewRequest.builder()
                    .wordIds(List.of(100L, 101L))
                    .build();

            WordReviewResponse result = wordReviewService.getReviewWords(userId, req);

            assertThat(result.getWords()).hasSize(1);
            assertThat(result.getWords().get(0).getWord()).isEqualTo("abandon");
        }

        @Test
        @DisplayName("should return due words based on progress")
        void getReviewWords_dueWords() {
            WordReviewRequest req = WordReviewRequest.builder().build();

            WordProgress progress = createProgress(100L, 1, LocalDateTime.now().minusHours(1));

            when(wordProgressRepository.findByUserIdAndNextReviewAtBeforeAndNextReviewAtIsNotNull(
                    eq(userId), any(LocalDateTime.class)))
                    .thenReturn(List.of(progress));
            when(wordRepository.findAllById(List.of(100L)))
                    .thenReturn(List.of(testWord));
            when(wordProgressRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.of(progress));

            WordReviewResponse result = wordReviewService.getReviewWords(userId, req);

            assertThat(result.getWords()).hasSize(1);
            assertThat(result.getWords().get(0).getMasteryLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("should set hasMore when due words exceed page size")
        void getReviewWords_dueWords_hasMore() {
            WordReviewRequest req = WordReviewRequest.builder().pageSize(5).build();

            List<WordProgress> progresses = new java.util.ArrayList<>();
            List<Word> words = new java.util.ArrayList<>();
            List<Long> wordIds = new java.util.ArrayList<>();

            for (int i = 0; i < 10; i++) {
                WordProgress p = createProgress(100L + i, 0, LocalDateTime.now().minusHours(1));
                progresses.add(p);
                wordIds.add(100L + i);

                Word w = new Word();
                w.setId(100L + i);
                w.setWord("word" + i);
                w.setDeleted(false);
                words.add(w);
            }

            when(wordProgressRepository.findByUserIdAndNextReviewAtBeforeAndNextReviewAtIsNotNull(
                    eq(userId), any(LocalDateTime.class)))
                    .thenReturn(progresses);
            when(wordRepository.findAllById(wordIds)).thenReturn(words);
            when(wordProgressRepository.findByUserIdAndWordId(eq(userId), anyLong()))
                    .thenReturn(Optional.of(progresses.get(0)));

            WordReviewResponse result = wordReviewService.getReviewWords(userId, req);

            assertThat(result.getWords()).hasSize(5);
            assertThat(result.getHasMore()).isTrue();
        }

        @Test
        @DisplayName("should fallback to unreviewed words when no due words exist and bookId is provided")
        void getReviewWords_fallbackToUnreviewed() {
            WordReviewRequest req = WordReviewRequest.builder()
                    .bookId(10L)
                    .pageSize(10)
                    .build();

            Word word1 = new Word();
            word1.setId(101L);
            word1.setBookId(10L);
            word1.setWord("newword1");
            word1.setDeleted(false);
            word1.setSortOrder(0);

            Word word2 = new Word();
            word2.setId(102L);
            word2.setBookId(10L);
            word2.setWord("newword2");
            word2.setDeleted(false);
            word2.setSortOrder(1);

            // No due words
            when(wordProgressRepository.findByUserIdAndNextReviewAtBeforeAndNextReviewAtIsNotNull(
                    eq(userId), any(LocalDateTime.class)))
                    .thenReturn(List.of());
            // All book words
            when(wordRepository.findByBookIdAndDeletedFalseOrderBySortOrder(10L))
                    .thenReturn(List.of(word1, word2));
            // No existing progress for these words
            when(wordProgressRepository.findByUserIdAndWordId(userId, 101L))
                    .thenReturn(Optional.empty());
            when(wordProgressRepository.findByUserIdAndWordId(userId, 102L))
                    .thenReturn(Optional.empty());

            WordReviewResponse result = wordReviewService.getReviewWords(userId, req);

            assertThat(result.getWords()).hasSize(2);
            assertThat(result.getWords().get(0).getWord()).isEqualTo("newword1");
            assertThat(result.getWords().get(1).getWord()).isEqualTo("newword2");
        }

        @Test
        @DisplayName("should filter out already-reviewed words when falling back to unreviewed")
        void getReviewWords_fallbackFiltersReviewed() {
            WordReviewRequest req = WordReviewRequest.builder()
                    .bookId(10L)
                    .pageSize(10)
                    .build();

            Word reviewedWord = new Word();
            reviewedWord.setId(101L);
            reviewedWord.setBookId(10L);
            reviewedWord.setWord("reviewed");
            reviewedWord.setDeleted(false);
            reviewedWord.setSortOrder(0);

            Word unreviewedWord = new Word();
            unreviewedWord.setId(102L);
            unreviewedWord.setBookId(10L);
            unreviewedWord.setWord("unreviewed");
            unreviewedWord.setDeleted(false);
            unreviewedWord.setSortOrder(1);

            when(wordProgressRepository.findByUserIdAndNextReviewAtBeforeAndNextReviewAtIsNotNull(
                    eq(userId), any(LocalDateTime.class)))
                    .thenReturn(List.of());
            when(wordRepository.findByBookIdAndDeletedFalseOrderBySortOrder(10L))
                    .thenReturn(List.of(reviewedWord, unreviewedWord));
            // reviewedWord has progress, unreviewedWord does not
            when(wordProgressRepository.findByUserIdAndWordId(userId, 101L))
                    .thenReturn(Optional.of(createProgress(101L, 3, LocalDateTime.now().plusDays(1))));
            when(wordProgressRepository.findByUserIdAndWordId(userId, 102L))
                    .thenReturn(Optional.empty());

            WordReviewResponse result = wordReviewService.getReviewWords(userId, req);

            assertThat(result.getWords()).hasSize(1);
            assertThat(result.getWords().get(0).getWord()).isEqualTo("unreviewed");
        }

        @Test
        @DisplayName("should return empty when no words match any criteria")
        void getReviewWords_empty() {
            WordReviewRequest req = WordReviewRequest.builder().build();

            when(wordProgressRepository.findByUserIdAndNextReviewAtBeforeAndNextReviewAtIsNotNull(
                    eq(userId), any(LocalDateTime.class)))
                    .thenReturn(List.of());

            WordReviewResponse result = wordReviewService.getReviewWords(userId, req);

            assertThat(result.getWords()).isEmpty();
            assertThat(result.getHasMore()).isFalse();
        }

        @Test
        @DisplayName("should include mastery level from progress")
        void getReviewWords_includesMasteryLevel() {
            WordReviewRequest req = WordReviewRequest.builder()
                    .wordIds(List.of(100L))
                    .build();

            WordProgress progress = createProgress(100L, 3, LocalDateTime.now().plusDays(7));

            when(wordRepository.findAllById(List.of(100L)))
                    .thenReturn(List.of(testWord));
            when(wordProgressRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.of(progress));

            WordReviewResponse result = wordReviewService.getReviewWords(userId, req);

            assertThat(result.getWords().get(0).getMasteryLevel()).isEqualTo(3);
        }

        @Test
        @DisplayName("should default mastery level to 0 when no progress exists")
        void getReviewWords_defaultMasteryLevel() {
            WordReviewRequest req = WordReviewRequest.builder()
                    .wordIds(List.of(100L))
                    .build();

            when(wordRepository.findAllById(List.of(100L)))
                    .thenReturn(List.of(testWord));
            when(wordProgressRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.empty());

            WordReviewResponse result = wordReviewService.getReviewWords(userId, req);

            assertThat(result.getWords().get(0).getMasteryLevel()).isEqualTo(0);
        }
    }

    // ==================== submitReviewResult Tests ====================

    @Nested
    @DisplayName("submitReviewResult()")
    class SubmitReviewResultTests {

        @Test
        @DisplayName("should create new progress for correct answer on new word")
        void submitReviewResult_correct_newProgress() {
            WordReviewResultRequest req = WordReviewResultRequest.builder()
                    .wordId(100L)
                    .isCorrect(true)
                    .mode("REVIEW")
                    .build();

            when(wordRepository.findById(100L)).thenReturn(Optional.of(testWord));
            when(wordProgressRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.empty());
            when(wrongWordRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.empty());
            when(wordProgressRepository.save(any(WordProgress.class))).thenAnswer(inv -> inv.getArgument(0));

            wordReviewService.submitReviewResult(userId, req);

            verify(wordProgressRepository).save(argThat(progress ->
                    progress.getMasteryLevel() == 1 &&
                    progress.getReviewCount() == 1 &&
                    progress.getCorrectCount() == 1 &&
                    progress.getWrongCount() == 0 &&
                    progress.getNextReviewAt() != null
            ));
        }

        @Test
        @DisplayName("should increment mastery level for correct answer on existing progress")
        void submitReviewResult_correct_existingProgress() {
            WordReviewResultRequest req = WordReviewResultRequest.builder()
                    .wordId(100L)
                    .isCorrect(true)
                    .mode("REVIEW")
                    .build();

            WordProgress existingProgress = createProgress(100L, 2, LocalDateTime.now().minusDays(1));

            when(wordRepository.findById(100L)).thenReturn(Optional.of(testWord));
            when(wordProgressRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.of(existingProgress));
            when(wrongWordRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.empty());
            when(wordProgressRepository.save(any(WordProgress.class))).thenAnswer(inv -> inv.getArgument(0));

            wordReviewService.submitReviewResult(userId, req);

            verify(wordProgressRepository).save(argThat(progress ->
                    progress.getMasteryLevel() == 3 &&
                    progress.getReviewCount() == 4 &&
                    progress.getCorrectCount() == 3
            ));
        }

        @Test
        @DisplayName("should cap mastery level at 5")
        void submitReviewResult_correct_cappedAtMax() {
            WordReviewResultRequest req = WordReviewResultRequest.builder()
                    .wordId(100L)
                    .isCorrect(true)
                    .mode("REVIEW")
                    .build();

            WordProgress maxProgress = createProgress(100L, 5, LocalDateTime.now().minusDays(1));

            when(wordRepository.findById(100L)).thenReturn(Optional.of(testWord));
            when(wordProgressRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.of(maxProgress));
            when(wrongWordRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.empty());
            when(wordProgressRepository.save(any(WordProgress.class))).thenAnswer(inv -> inv.getArgument(0));

            wordReviewService.submitReviewResult(userId, req);

            verify(wordProgressRepository).save(argThat(progress ->
                    progress.getMasteryLevel() == 5
            ));
        }

        @Test
        @DisplayName("should decrease mastery level for wrong answer on new word")
        void submitReviewResult_wrong_newProgress() {
            WordReviewResultRequest req = WordReviewResultRequest.builder()
                    .wordId(100L)
                    .isCorrect(false)
                    .mode("REVIEW")
                    .build();

            when(wordRepository.findById(100L)).thenReturn(Optional.of(testWord));
            when(wordProgressRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.empty());
            when(wrongWordRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.empty());
            when(wrongWordRepository.save(any(WrongWord.class))).thenAnswer(inv -> inv.getArgument(0));
            when(wordProgressRepository.save(any(WordProgress.class))).thenAnswer(inv -> inv.getArgument(0));

            wordReviewService.submitReviewResult(userId, req);

            verify(wordProgressRepository).save(argThat(progress ->
                    progress.getMasteryLevel() == 0 &&
                    progress.getReviewCount() == 1 &&
                    progress.getCorrectCount() == 0 &&
                    progress.getWrongCount() == 1
            ));
            verify(wrongWordRepository).save(any(WrongWord.class));
        }

        @Test
        @DisplayName("should decrease mastery level for wrong answer on existing progress")
        void submitReviewResult_wrong_existingProgress() {
            WordReviewResultRequest req = WordReviewResultRequest.builder()
                    .wordId(100L)
                    .isCorrect(false)
                    .mode("REVIEW")
                    .build();

            WordProgress existingProgress = createProgress(100L, 3, LocalDateTime.now().minusDays(1));

            when(wordRepository.findById(100L)).thenReturn(Optional.of(testWord));
            when(wordProgressRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.of(existingProgress));
            when(wrongWordRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.empty());
            when(wrongWordRepository.save(any(WrongWord.class))).thenAnswer(inv -> inv.getArgument(0));
            when(wordProgressRepository.save(any(WordProgress.class))).thenAnswer(inv -> inv.getArgument(0));

            wordReviewService.submitReviewResult(userId, req);

            verify(wordProgressRepository).save(argThat(progress ->
                    progress.getMasteryLevel() == 2 &&
                    progress.getReviewCount() == 4 &&
                    progress.getWrongCount() == 2
            ));
        }

        @Test
        @DisplayName("should not decrease mastery level below 0")
        void submitReviewResult_wrong_floorAtZero() {
            WordReviewResultRequest req = WordReviewResultRequest.builder()
                    .wordId(100L)
                    .isCorrect(false)
                    .mode("REVIEW")
                    .build();

            WordProgress zeroProgress = createProgress(100L, 0, LocalDateTime.now().minusDays(1));

            when(wordRepository.findById(100L)).thenReturn(Optional.of(testWord));
            when(wordProgressRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.of(zeroProgress));
            when(wrongWordRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.empty());
            when(wrongWordRepository.save(any(WrongWord.class))).thenAnswer(inv -> inv.getArgument(0));
            when(wordProgressRepository.save(any(WordProgress.class))).thenAnswer(inv -> inv.getArgument(0));

            wordReviewService.submitReviewResult(userId, req);

            verify(wordProgressRepository).save(argThat(progress ->
                    progress.getMasteryLevel() == 0
            ));
        }

        @Test
        @DisplayName("should record dictation when mode is DICTATION")
        void submitReviewResult_dictationMode() {
            WordReviewResultRequest req = WordReviewResultRequest.builder()
                    .wordId(100L)
                    .isCorrect(true)
                    .mode("DICTATION")
                    .build();

            when(wordRepository.findById(100L)).thenReturn(Optional.of(testWord));
            when(wordProgressRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.empty());
            when(wrongWordRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.empty());
            when(wordProgressRepository.save(any(WordProgress.class))).thenAnswer(inv -> inv.getArgument(0));
            when(dictationRecordRepository.save(any(DictationRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            wordReviewService.submitReviewResult(userId, req);

            verify(dictationRecordRepository).save(argThat(record ->
                    record.getUserId().equals(userId) &&
                    record.getWordId().equals(100L) &&
                    Boolean.TRUE.equals(record.getIsCorrect())
            ));
        }

        @Test
        @DisplayName("should not record dictation when mode is REVIEW")
        void submitReviewResult_reviewMode() {
            WordReviewResultRequest req = WordReviewResultRequest.builder()
                    .wordId(100L)
                    .isCorrect(true)
                    .mode("REVIEW")
                    .build();

            when(wordRepository.findById(100L)).thenReturn(Optional.of(testWord));
            when(wordProgressRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.empty());
            when(wrongWordRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.empty());
            when(wordProgressRepository.save(any(WordProgress.class))).thenAnswer(inv -> inv.getArgument(0));

            wordReviewService.submitReviewResult(userId, req);

            verify(dictationRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("should handle DICTATION mode case-insensitively")
        void submitReviewResult_dictationModeCaseInsensitive() {
            WordReviewResultRequest req = WordReviewResultRequest.builder()
                    .wordId(100L)
                    .isCorrect(false)
                    .mode("dictation")
                    .build();

            when(wordRepository.findById(100L)).thenReturn(Optional.of(testWord));
            when(wordProgressRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.empty());
            when(wrongWordRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.empty());
            when(wrongWordRepository.save(any(WrongWord.class))).thenAnswer(inv -> inv.getArgument(0));
            when(wordProgressRepository.save(any(WordProgress.class))).thenAnswer(inv -> inv.getArgument(0));
            when(dictationRecordRepository.save(any(DictationRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            wordReviewService.submitReviewResult(userId, req);

            verify(dictationRecordRepository).save(any(DictationRecord.class));
        }

        @Test
        @DisplayName("should mark WrongWord as mastered when correct after being wrong")
        void submitReviewResult_correct_marksWrongWordMastered() {
            WordReviewResultRequest req = WordReviewResultRequest.builder()
                    .wordId(100L)
                    .isCorrect(true)
                    .mode("REVIEW")
                    .build();

            WordProgress existingProgress = createProgress(100L, 1, LocalDateTime.now().minusDays(1));

            WrongWord existingWrong = new WrongWord();
            existingWrong.setId(1L);
            existingWrong.setUserId(userId);
            existingWrong.setWordId(100L);
            existingWrong.setWrongCount(3);
            existingWrong.setMastered(false);

            when(wordRepository.findById(100L)).thenReturn(Optional.of(testWord));
            when(wordProgressRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.of(existingProgress));
            when(wrongWordRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.of(existingWrong));
            when(wrongWordRepository.save(any(WrongWord.class))).thenAnswer(inv -> inv.getArgument(0));
            when(wordProgressRepository.save(any(WordProgress.class))).thenAnswer(inv -> inv.getArgument(0));

            wordReviewService.submitReviewResult(userId, req);

            verify(wrongWordRepository).save(argThat(ww ->
                    Boolean.TRUE.equals(ww.getMastered())
            ));
        }

        @Test
        @DisplayName("should update existing WrongWord when answer is wrong again")
        void submitReviewResult_wrong_updatesExistingWrongWord() {
            WordReviewResultRequest req = WordReviewResultRequest.builder()
                    .wordId(100L)
                    .isCorrect(false)
                    .mode("REVIEW")
                    .build();

            WordProgress existingProgress = createProgress(100L, 2, LocalDateTime.now().minusDays(1));

            WrongWord existingWrong = new WrongWord();
            existingWrong.setId(1L);
            existingWrong.setUserId(userId);
            existingWrong.setWordId(100L);
            existingWrong.setWrongCount(2);
            existingWrong.setMastered(false);

            when(wordRepository.findById(100L)).thenReturn(Optional.of(testWord));
            when(wordProgressRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.of(existingProgress));
            when(wrongWordRepository.findByUserIdAndWordId(userId, 100L))
                    .thenReturn(Optional.of(existingWrong));
            when(wrongWordRepository.save(any(WrongWord.class))).thenAnswer(inv -> inv.getArgument(0));
            when(wordProgressRepository.save(any(WordProgress.class))).thenAnswer(inv -> inv.getArgument(0));

            wordReviewService.submitReviewResult(userId, req);

            verify(wrongWordRepository).save(argThat(ww ->
                    ww.getWrongCount() == 3 &&
                    Boolean.FALSE.equals(ww.getMastered())
            ));
        }

        @Test
        @DisplayName("should throw WORD_NOT_FOUND when word does not exist")
        void submitReviewResult_wordNotFound() {
            WordReviewResultRequest req = WordReviewResultRequest.builder()
                    .wordId(999L)
                    .isCorrect(true)
                    .mode("REVIEW")
                    .build();

            when(wordRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> wordReviewService.submitReviewResult(userId, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.WORD_NOT_FOUND);
        }

        @Test
        @DisplayName("should throw WORD_NOT_FOUND when word is deleted")
        void submitReviewResult_wordDeleted() {
            WordReviewResultRequest req = WordReviewResultRequest.builder()
                    .wordId(100L)
                    .isCorrect(true)
                    .mode("REVIEW")
                    .build();

            testWord.setDeleted(true);
            when(wordRepository.findById(100L)).thenReturn(Optional.of(testWord));

            assertThatThrownBy(() -> wordReviewService.submitReviewResult(userId, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.WORD_NOT_FOUND);
        }
    }

    // ==================== getWrongWords Tests ====================

    @Nested
    @DisplayName("getWrongWords()")
    class GetWrongWordsTests {

        @Test
        @DisplayName("should return unmastered wrong words")
        void getWrongWords_success() {
            WrongWord wrongWord = new WrongWord();
            wrongWord.setId(1L);
            wrongWord.setUserId(userId);
            wrongWord.setWordId(100L);
            wrongWord.setWrongCount(3);
            wrongWord.setMastered(false);

            when(wrongWordRepository.findByUserIdAndMasteredFalseOrderByLastWrongAtDesc(userId))
                    .thenReturn(List.of(wrongWord));
            when(wordRepository.findById(100L)).thenReturn(Optional.of(testWord));

            List<WordReviewResponse.WordReviewItem> result = wordReviewService.getWrongWords(userId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getWord()).isEqualTo("abandon");
        }

        @Test
        @DisplayName("should skip deleted words")
        void getWrongWords_skipsDeleted() {
            WrongWord wrongWord = new WrongWord();
            wrongWord.setId(1L);
            wrongWord.setUserId(userId);
            wrongWord.setWordId(100L);
            wrongWord.setMastered(false);

            testWord.setDeleted(true);

            when(wrongWordRepository.findByUserIdAndMasteredFalseOrderByLastWrongAtDesc(userId))
                    .thenReturn(List.of(wrongWord));
            when(wordRepository.findById(100L)).thenReturn(Optional.of(testWord));

            List<WordReviewResponse.WordReviewItem> result = wordReviewService.getWrongWords(userId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should skip mastered wrong words")
        void getWrongWords_skipsMastered() {
            when(wrongWordRepository.findByUserIdAndMasteredFalseOrderByLastWrongAtDesc(userId))
                    .thenReturn(List.of());

            List<WordReviewResponse.WordReviewItem> result = wordReviewService.getWrongWords(userId);

            assertThat(result).isEmpty();
        }
    }

    // ==================== getDictationStats Tests ====================

    @Nested
    @DisplayName("getDictationStats()")
    class GetDictationStatsTests {

        @Test
        @DisplayName("should return correct and wrong counts")
        void getDictationStats_success() {
            when(dictationRecordRepository.countByUserIdAndIsCorrectTrue(userId)).thenReturn(15L);
            when(dictationRecordRepository.countByUserIdAndIsCorrectFalse(userId)).thenReturn(5L);

            Map<String, Object> stats = wordReviewService.getDictationStats(userId);

            assertThat(stats.get("correctCount")).isEqualTo(15L);
            assertThat(stats.get("wrongCount")).isEqualTo(5L);
            assertThat(stats.get("totalCount")).isEqualTo(20L);
        }

        @Test
        @DisplayName("should return zeros when no dictation records exist")
        void getDictationStats_empty() {
            when(dictationRecordRepository.countByUserIdAndIsCorrectTrue(userId)).thenReturn(0L);
            when(dictationRecordRepository.countByUserIdAndIsCorrectFalse(userId)).thenReturn(0L);

            Map<String, Object> stats = wordReviewService.getDictationStats(userId);

            assertThat(stats.get("correctCount")).isEqualTo(0L);
            assertThat(stats.get("wrongCount")).isEqualTo(0L);
            assertThat(stats.get("totalCount")).isEqualTo(0L);
        }
    }
}
