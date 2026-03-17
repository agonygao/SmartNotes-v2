package com.smartnotes.service;

import com.smartnotes.dto.ErrorCode;
import com.smartnotes.dto.PageResponse;
import com.smartnotes.dto.WordResponse;
import com.smartnotes.entity.Word;
import com.smartnotes.entity.WordBook;
import com.smartnotes.entity.WordBookType;
import com.smartnotes.exception.BusinessException;
import com.smartnotes.repository.WordBookRepository;
import com.smartnotes.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WordService {

    private final WordRepository wordRepository;
    private final WordBookRepository wordBookRepository;

    public WordResponse addWord(Long userId, Long bookId, String word, String phonetic,
                                String meaning, String exampleSentence) {
        WordBook wordBook = findAccessibleWordBook(userId, bookId);

        if (wordRepository.existsByBookIdAndWordAndDeletedFalse(bookId, word)) {
            throw new BusinessException(ErrorCode.DUPLICATE_WORD);
        }

        long count = wordRepository.countByBookIdAndDeletedFalse(bookId);

        Word newWord = new Word();
        newWord.setBookId(bookId);
        newWord.setWord(word);
        newWord.setPhonetic(phonetic);
        newWord.setMeaning(meaning);
        newWord.setExampleSentence(exampleSentence);
        newWord.setSortOrder((int) count + 1);
        wordRepository.save(newWord);

        wordBook.setWordCount(wordBook.getWordCount() + 1);
        wordBookRepository.save(wordBook);

        return convertToResponse(newWord);
    }

    public WordResponse updateWord(Long userId, Long wordId, Long bookId, String word,
                                   String phonetic, String meaning, String exampleSentence) {
        findAccessibleWordBook(userId, bookId);

        Word existingWord = wordRepository.findByIdAndBookIdAndDeletedFalse(wordId, bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORD_NOT_FOUND));

        if (word != null) {
            existingWord.setWord(word);
        }
        if (phonetic != null) {
            existingWord.setPhonetic(phonetic);
        }
        if (meaning != null) {
            existingWord.setMeaning(meaning);
        }
        if (exampleSentence != null) {
            existingWord.setExampleSentence(exampleSentence);
        }
        existingWord.setVersion(existingWord.getVersion() + 1);
        wordRepository.save(existingWord);

        return convertToResponse(existingWord);
    }

    public void deleteWord(Long userId, Long wordId, Long bookId) {
        WordBook wordBook = findAccessibleWordBook(userId, bookId);

        Word word = wordRepository.findByIdAndBookIdAndDeletedFalse(wordId, bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORD_NOT_FOUND));

        word.setDeleted(true);
        wordRepository.save(word);

        wordBook.setWordCount(Math.max(0, wordBook.getWordCount() - 1));
        wordBookRepository.save(wordBook);
    }

    @Transactional(readOnly = true)
    public WordResponse getWord(Long userId, Long bookId, Long wordId) {
        findAccessibleWordBook(userId, bookId);

        Word word = wordRepository.findByIdAndBookIdAndDeletedFalse(wordId, bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORD_NOT_FOUND));

        return convertToResponse(word);
    }

    @Transactional(readOnly = true)
    public PageResponse<WordResponse> listWords(Long userId, Long bookId, int page, int size) {
        findAccessibleWordBook(userId, bookId);

        Page<Word> wordPage = wordRepository.findByBookIdAndDeletedFalse(bookId, PageRequest.of(page, size));

        List<WordResponse> content = new ArrayList<>();
        for (Word word : wordPage.getContent()) {
            content.add(convertToResponse(word));
        }

        return PageResponse.<WordResponse>builder()
                .content(content)
                .totalElements(wordPage.getTotalElements())
                .totalPages(wordPage.getTotalPages())
                .page(wordPage.getNumber())
                .size(wordPage.getSize())
                .first(wordPage.isFirst())
                .last(wordPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public List<WordResponse> searchWords(Long userId, Long bookId, String keyword) {
        findAccessibleWordBook(userId, bookId);

        List<Word> words = wordRepository.findByBookIdAndDeletedFalseAndWordContainingIgnoreCase(bookId, keyword);
        List<WordResponse> responses = new ArrayList<>();
        for (Word word : words) {
            responses.add(convertToResponse(word));
        }
        return responses;
    }

    private WordBook findAccessibleWordBook(Long userId, Long bookId) {
        // First try to find as user-owned book
        WordBook wordBook = wordBookRepository.findByIdAndUserIdAndDeletedFalse(bookId, userId).orElse(null);

        if (wordBook == null) {
            // If not found as user-owned, check if it's a default book
            wordBook = wordBookRepository.findById(bookId)
                    .filter(b -> !b.getDeleted() && b.getIsDefault())
                    .orElse(null);
        }

        if (wordBook == null) {
            throw new BusinessException(ErrorCode.WORD_BOOK_NOT_FOUND);
        }

        return wordBook;
    }

    public WordResponse convertToResponse(Word word) {
        return WordResponse.builder()
                .id(word.getId())
                .bookId(word.getBookId())
                .word(word.getWord())
                .phonetic(word.getPhonetic())
                .meaning(word.getMeaning())
                .exampleSentence(word.getExampleSentence())
                .sortOrder(word.getSortOrder())
                .clientId(word.getClientId())
                .version(word.getVersion())
                .createdAt(word.getCreatedAt())
                .updatedAt(word.getUpdatedAt())
                .build();
    }
}
