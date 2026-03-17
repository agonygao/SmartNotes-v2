package com.smartnotes.controller;

import com.smartnotes.dto.ApiResponse;
import com.smartnotes.dto.PageResponse;
import com.smartnotes.dto.WordResponse;
import com.smartnotes.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wordbooks/{bookId}/words")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;

    @PostMapping
    public ApiResponse<WordResponse> addWord(@PathVariable Long bookId,
                                             @RequestParam String word,
                                             @RequestParam(required = false) String phonetic,
                                             @RequestParam(required = false) String meaning,
                                             @RequestParam(required = false) String exampleSentence) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(wordService.addWord(userId, bookId, word, phonetic, meaning, exampleSentence));
    }

    @GetMapping
    public ApiResponse<PageResponse<WordResponse>> listWords(@PathVariable Long bookId,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(wordService.listWords(userId, bookId, page, size));
    }

    @GetMapping("/search")
    public ApiResponse<List<WordResponse>> searchWords(@PathVariable Long bookId,
                                                        @RequestParam String keyword) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(wordService.searchWords(userId, bookId, keyword));
    }

    @GetMapping("/{wordId}")
    public ApiResponse<WordResponse> getWord(@PathVariable Long bookId,
                                              @PathVariable Long wordId) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(wordService.getWord(userId, bookId, wordId));
    }

    @PutMapping("/{wordId}")
    public ApiResponse<WordResponse> updateWord(@PathVariable Long bookId,
                                                 @PathVariable Long wordId,
                                                 @RequestParam(required = false) String word,
                                                 @RequestParam(required = false) String phonetic,
                                                 @RequestParam(required = false) String meaning,
                                                 @RequestParam(required = false) String exampleSentence) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(wordService.updateWord(userId, wordId, bookId, word, phonetic, meaning, exampleSentence));
    }

    @DeleteMapping("/{wordId}")
    public ApiResponse<Void> deleteWord(@PathVariable Long bookId,
                                         @PathVariable Long wordId) {
        Long userId = getCurrentUserId();
        wordService.deleteWord(userId, wordId, bookId);
        return ApiResponse.success();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
