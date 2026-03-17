package com.smartnotes.controller;

import com.smartnotes.dto.ApiResponse;
import com.smartnotes.dto.WordReviewRequest;
import com.smartnotes.dto.WordReviewResponse;
import com.smartnotes.dto.WordReviewResultRequest;
import com.smartnotes.service.WordReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class WordReviewController {

    private final WordReviewService wordReviewService;

    @PostMapping("/words")
    public ApiResponse<WordReviewResponse> getReviewWords(@RequestBody WordReviewRequest req) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(wordReviewService.getReviewWords(userId, req));
    }

    @PostMapping("/result")
    public ApiResponse<Void> submitReviewResult(@Valid @RequestBody WordReviewResultRequest req) {
        Long userId = getCurrentUserId();
        wordReviewService.submitReviewResult(userId, req);
        return ApiResponse.success();
    }

    @GetMapping("/wrong-words")
    public ApiResponse<List<WordReviewResponse.WordReviewItem>> getWrongWords() {
        Long userId = getCurrentUserId();
        return ApiResponse.success(wordReviewService.getWrongWords(userId));
    }

    @GetMapping("/dictation/stats")
    public ApiResponse<Map<String, Object>> getDictationStats() {
        Long userId = getCurrentUserId();
        return ApiResponse.success(wordReviewService.getDictationStats(userId));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
