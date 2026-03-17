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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WordReviewService {

    private final WordRepository wordRepository;
    private final WordProgressRepository wordProgressRepository;
    private final DictationRecordRepository dictationRecordRepository;
    private final WrongWordRepository wrongWordRepository;

    public WordReviewResponse getReviewWords(Long userId, WordReviewRequest req) {
        List<Word> reviewWords = new ArrayList<>();
        boolean hasMore = false;

        if (req.getWordIds() != null && !req.getWordIds().isEmpty()) {
            // Get specific words by IDs
            reviewWords = wordRepository.findAllById(req.getWordIds()).stream()
                    .filter(w -> !w.getDeleted())
                    .collect(Collectors.toList());
        } else {
            LocalDateTime now = LocalDateTime.now();

            // Get words due for review
            List<WordProgress> dueProgress = wordProgressRepository
                    .findByUserIdAndNextReviewAtBeforeAndNextReviewAtIsNotNull(userId, now);

            if (!dueProgress.isEmpty()) {
                List<Long> dueWordIds = dueProgress.stream()
                        .map(WordProgress::getWordId)
                        .collect(Collectors.toList());

                List<Word> dueWords = wordRepository.findAllById(dueWordIds).stream()
                        .filter(w -> !w.getDeleted())
                        .collect(Collectors.toList());

                int limit = req.getPageSize() != null ? req.getPageSize() : 20;
                if (dueWords.size() > limit) {
                    reviewWords = dueWords.subList(0, limit);
                    hasMore = true;
                } else {
                    reviewWords = dueWords;
                }
            }

            // If no due words and bookId is provided, fallback to first N unreviewed words from book
            if (reviewWords.isEmpty() && req.getBookId() != null) {
                List<Word> allBookWords = wordRepository
                        .findByBookIdAndDeletedFalseOrderBySortOrder(req.getBookId());

                // Filter out words that already have progress
                List<Long> existingProgressWordIds = allBookWords.stream()
                        .filter(w -> wordProgressRepository.findByUserIdAndWordId(userId, w.getId()).isPresent())
                        .map(Word::getId)
                        .collect(Collectors.toList());

                List<Word> unreviewedWords = allBookWords.stream()
                        .filter(w -> !existingProgressWordIds.contains(w.getId()))
                        .collect(Collectors.toList());

                int limit = req.getPageSize() != null ? req.getPageSize() : 20;
                if (unreviewedWords.size() > limit) {
                    reviewWords = unreviewedWords.subList(0, limit);
                    hasMore = true;
                } else {
                    reviewWords = unreviewedWords;
                }
            }
        }

        // Build WordReviewItem list with mastery levels
        Map<Long, WordProgress> progressMap = new HashMap<>();
        for (Word w : reviewWords) {
            wordProgressRepository.findByUserIdAndWordId(userId, w.getId())
                    .ifPresent(p -> progressMap.put(w.getId(), p));
        }

        List<WordReviewResponse.WordReviewItem> items = new ArrayList<>();
        for (Word w : reviewWords) {
            WordProgress progress = progressMap.get(w.getId());
            items.add(WordReviewResponse.WordReviewItem.builder()
                    .wordId(w.getId())
                    .word(w.getWord())
                    .phonetic(w.getPhonetic())
                    .meaning(w.getMeaning())
                    .exampleSentence(w.getExampleSentence())
                    .masteryLevel(progress != null ? progress.getMasteryLevel() : 0)
                    .build());
        }

        return WordReviewResponse.builder()
                .words(items)
                .hasMore(hasMore)
                .build();
    }

    public void submitReviewResult(Long userId, WordReviewResultRequest req) {
        Word word = wordRepository.findById(req.getWordId())
                .filter(w -> !w.getDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.WORD_NOT_FOUND));

        WordProgress progress = wordProgressRepository.findByUserIdAndWordId(userId, req.getWordId())
                .orElseGet(() -> {
                    WordProgress p = new WordProgress();
                    p.setUserId(userId);
                    p.setWordId(req.getWordId());
                    p.setMasteryLevel(0);
                    p.setReviewCount(0);
                    p.setCorrectCount(0);
                    p.setWrongCount(0);
                    return p;
                });

        progress.setReviewCount(progress.getReviewCount() + 1);
        progress.setLastReviewedAt(LocalDateTime.now());

        if (req.getIsCorrect()) {
            progress.setCorrectCount(progress.getCorrectCount() + 1);

            // Increase mastery level (max 5)
            int newLevel = Math.min(progress.getMasteryLevel() + 1, 5);
            progress.setMasteryLevel(newLevel);

            // Set next review date based on spaced repetition
            progress.setNextReviewAt(calculateNextReview(LocalDateTime.now(), newLevel));

            // If word was previously wrong, update WrongWord
            wrongWordRepository.findByUserIdAndWordId(userId, req.getWordId()).ifPresent(wrongWord -> {
                wrongWord.setWrongCount(wrongWord.getWrongCount() + 1);
                wrongWord.setMastered(true);
                wrongWord.setUpdatedAt(LocalDateTime.now());
                wrongWordRepository.save(wrongWord);
            });
        } else {
            progress.setWrongCount(progress.getWrongCount() + 1);

            // Decrease mastery level (min 0)
            int newLevel = Math.max(progress.getMasteryLevel() - 1, 0);
            progress.setMasteryLevel(newLevel);

            // Reset next review to now
            progress.setNextReviewAt(LocalDateTime.now());

            // Update or create WrongWord record
            WrongWord wrongWord = wrongWordRepository.findByUserIdAndWordId(userId, req.getWordId())
                    .orElseGet(() -> {
                        WrongWord ww = new WrongWord();
                        ww.setUserId(userId);
                        ww.setWordId(req.getWordId());
                        ww.setWrongCount(0);
                        ww.setMastered(false);
                        return ww;
                    });
            wrongWord.setWrongCount(wrongWord.getWrongCount() + 1);
            wrongWord.setLastWrongAt(LocalDateTime.now());
            wrongWord.setMastered(false);
            wrongWordRepository.save(wrongWord);
        }

        wordProgressRepository.save(progress);

        // Record DictationRecord if mode is DICTATION
        if ("DICTATION".equalsIgnoreCase(req.getMode())) {
            DictationRecord record = new DictationRecord();
            record.setUserId(userId);
            record.setWordId(req.getWordId());
            record.setIsCorrect(req.getIsCorrect());
            dictationRecordRepository.save(record);
        }
    }

    @Transactional(readOnly = true)
    public List<WordReviewResponse.WordReviewItem> getWrongWords(Long userId) {
        List<WrongWord> wrongWords = wrongWordRepository.findByUserIdAndMasteredFalseOrderByLastWrongAtDesc(userId);

        List<WordReviewResponse.WordReviewItem> items = new ArrayList<>();
        for (WrongWord ww : wrongWords) {
            Word word = wordRepository.findById(ww.getWordId())
                    .filter(w -> !w.getDeleted())
                    .orElse(null);
            if (word != null) {
                items.add(WordReviewResponse.WordReviewItem.builder()
                        .wordId(word.getId())
                        .word(word.getWord())
                        .phonetic(word.getPhonetic())
                        .meaning(word.getMeaning())
                        .exampleSentence(word.getExampleSentence())
                        .masteryLevel(0)
                        .build());
            }
        }
        return items;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDictationStats(Long userId) {
        long correctCount = dictationRecordRepository.countByUserIdAndIsCorrectTrue(userId);
        long wrongCount = dictationRecordRepository.countByUserIdAndIsCorrectFalse(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("correctCount", correctCount);
        stats.put("wrongCount", wrongCount);
        stats.put("totalCount", correctCount + wrongCount);
        return stats;
    }

    private LocalDateTime calculateNextReview(LocalDateTime now, int masteryLevel) {
        return switch (masteryLevel) {
            case 1 -> now.plusDays(1);
            case 2 -> now.plusDays(3);
            case 3 -> now.plusDays(7);
            case 4 -> now.plusDays(14);
            case 5 -> now.plusDays(30);
            default -> now.plusDays(1);
        };
    }
}
