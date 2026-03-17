package com.smartnotes.controller;

import com.smartnotes.dto.ApiResponse;
import com.smartnotes.dto.WordBookRequest;
import com.smartnotes.dto.WordBookResponse;
import com.smartnotes.service.WordBookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wordbooks")
@RequiredArgsConstructor
public class WordBookController {

    private final WordBookService wordBookService;

    @PostMapping
    public ApiResponse<WordBookResponse> createWordBook(@Valid @RequestBody WordBookRequest req) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(wordBookService.createWordBook(userId, req));
    }

    @GetMapping
    public ApiResponse<List<WordBookResponse>> listWordBooks() {
        Long userId = getCurrentUserId();
        return ApiResponse.success(wordBookService.listWordBooks(userId));
    }

    @GetMapping("/defaults")
    public ApiResponse<List<WordBookResponse>> getDefaultWordBooks() {
        return ApiResponse.success(wordBookService.getDefaultWordBooks());
    }

    @GetMapping("/{id}")
    public ApiResponse<WordBookResponse> getWordBook(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(wordBookService.getWordBook(userId, id));
    }

    @PutMapping("/{id}")
    public ApiResponse<WordBookResponse> updateWordBook(@PathVariable Long id,
                                                         @Valid @RequestBody WordBookRequest req) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(wordBookService.updateWordBook(userId, id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteWordBook(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        wordBookService.deleteWordBook(userId, id);
        return ApiResponse.success();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
