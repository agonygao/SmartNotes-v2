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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WordBookService {

    private final WordBookRepository wordBookRepository;
    private final WordRepository wordRepository;
    private final DefaultWordData defaultWordData;

    public WordBookResponse createWordBook(Long userId, WordBookRequest req) {
        WordBook wordBook = new WordBook();
        wordBook.setUserId(userId);
        wordBook.setName(req.getName());
        wordBook.setDescription(req.getDescription());
        wordBook.setType(parseType(req.getType(), WordBookType.CUSTOM));
        wordBook.setWordCount(0);
        wordBook.setIsDefault(false);
        wordBookRepository.save(wordBook);
        return convertToResponse(wordBook);
    }

    public WordBookResponse updateWordBook(Long userId, Long id, WordBookRequest req) {
        WordBook wordBook = wordBookRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORD_BOOK_NOT_FOUND));
        if (req.getName() != null) {
            wordBook.setName(req.getName());
        }
        if (req.getDescription() != null) {
            wordBook.setDescription(req.getDescription());
        }
        if (req.getType() != null) {
            wordBook.setType(parseType(req.getType(), wordBook.getType()));
        }
        wordBook.setVersion(wordBook.getVersion() + 1);
        wordBookRepository.save(wordBook);
        return convertToResponse(wordBook);
    }

    public void deleteWordBook(Long userId, Long id) {
        WordBook wordBook = wordBookRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORD_BOOK_NOT_FOUND));
        if (wordBook.getType() == WordBookType.CET4 || wordBook.getType() == WordBookType.CET6) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Cannot delete default CET4/CET6 word books");
        }
        wordBook.setDeleted(true);
        wordBookRepository.save(wordBook);
    }

    @Transactional(readOnly = true)
    public WordBookResponse getWordBook(Long userId, Long id) {
        WordBook wordBook = wordBookRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORD_BOOK_NOT_FOUND));
        return convertToResponse(wordBook);
    }

    @Transactional(readOnly = true)
    public List<WordBookResponse> listWordBooks(Long userId) {
        List<WordBook> books = wordBookRepository.findByUserIdAndDeletedFalse(userId);
        List<WordBookResponse> responses = new ArrayList<>();
        for (WordBook book : books) {
            responses.add(convertToResponse(book));
        }
        return responses;
    }

    public List<WordBookResponse> initializeDefaultWordBooks(Long userId) {
        List<WordBookResponse> defaultBooks = new ArrayList<>();

        // Create CET4 default book if not exists
        if (!wordBookRepository.existsByTypeAndIsDefaultTrue(WordBookType.CET4)) {
            WordBook cet4Book = new WordBook();
            cet4Book.setUserId(null); // shared book
            cet4Book.setName("CET-4 Core Vocabulary");
            cet4Book.setDescription("College English Test Band 4 core vocabulary");
            cet4Book.setType(WordBookType.CET4);
            cet4Book.setWordCount(0);
            cet4Book.setIsDefault(true);
            wordBookRepository.save(cet4Book);

            // Inject default CET4 words
            List<Word> cet4Words = defaultWordData.getCet4Words();
            for (Word word : cet4Words) {
                word.setBookId(cet4Book.getId());
            }
            wordRepository.saveAll(cet4Words);
            cet4Book.setWordCount(cet4Words.size());
            wordBookRepository.save(cet4Book);

            defaultBooks.add(convertToResponse(cet4Book));
        } else {
            wordBookRepository.findByTypeAndIsDefaultTrue(WordBookType.CET4)
                    .ifPresent(book -> defaultBooks.add(convertToResponse(book)));
        }

        // Create CET6 default book if not exists
        if (!wordBookRepository.existsByTypeAndIsDefaultTrue(WordBookType.CET6)) {
            WordBook cet6Book = new WordBook();
            cet6Book.setUserId(null); // shared book
            cet6Book.setName("CET-6 Core Vocabulary");
            cet6Book.setDescription("College English Test Band 6 core vocabulary");
            cet6Book.setType(WordBookType.CET6);
            cet6Book.setWordCount(0);
            cet6Book.setIsDefault(true);
            wordBookRepository.save(cet6Book);

            // Inject default CET6 words
            List<Word> cet6Words = defaultWordData.getCet6Words();
            for (Word word : cet6Words) {
                word.setBookId(cet6Book.getId());
            }
            wordRepository.saveAll(cet6Words);
            cet6Book.setWordCount(cet6Words.size());
            wordBookRepository.save(cet6Book);

            defaultBooks.add(convertToResponse(cet6Book));
        } else {
            wordBookRepository.findByTypeAndIsDefaultTrue(WordBookType.CET6)
                    .ifPresent(book -> defaultBooks.add(convertToResponse(book)));
        }

        return defaultBooks;
    }

    @Transactional(readOnly = true)
    public List<WordBookResponse> getDefaultWordBooks() {
        List<WordBookResponse> responses = new ArrayList<>();
        wordBookRepository.findByTypeAndIsDefaultTrue(WordBookType.CET4)
                .ifPresent(book -> responses.add(convertToResponse(book)));
        wordBookRepository.findByTypeAndIsDefaultTrue(WordBookType.CET6)
                .ifPresent(book -> responses.add(convertToResponse(book)));
        return responses;
    }

    public WordBookResponse convertToResponse(WordBook wordBook) {
        return WordBookResponse.builder()
                .id(wordBook.getId())
                .userId(wordBook.getUserId())
                .name(wordBook.getName())
                .description(wordBook.getDescription())
                .type(wordBook.getType() != null ? wordBook.getType().name() : null)
                .wordCount(wordBook.getWordCount())
                .isDefault(wordBook.getIsDefault())
                .clientId(wordBook.getClientId())
                .version(wordBook.getVersion())
                .createdAt(wordBook.getCreatedAt())
                .updatedAt(wordBook.getUpdatedAt())
                .build();
    }

    private WordBookType parseType(String typeStr, WordBookType defaultType) {
        if (typeStr == null || typeStr.isBlank()) {
            return defaultType;
        }
        try {
            return WordBookType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultType;
        }
    }
}
