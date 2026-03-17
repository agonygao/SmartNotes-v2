package com.smartnotes.controller;

import com.smartnotes.dto.ApiResponse;
import com.smartnotes.dto.NoteRequest;
import com.smartnotes.dto.NoteResponse;
import com.smartnotes.dto.PageResponse;
import com.smartnotes.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }

    @PostMapping
    public ApiResponse<NoteResponse> createNote(@Valid @RequestBody NoteRequest request) {
        Long userId = getCurrentUserId();
        NoteResponse response = noteService.createNote(userId, request);
        return ApiResponse.success(response);
    }

    @GetMapping
    public ApiResponse<PageResponse<NoteResponse>> listNotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type) {
        Long userId = getCurrentUserId();
        PageResponse<NoteResponse> response = noteService.listNotes(userId, page, size, type);
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    public ApiResponse<NoteResponse> getNote(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        NoteResponse response = noteService.getNote(userId, id);
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}")
    public ApiResponse<NoteResponse> updateNote(@PathVariable Long id,
                                                @Valid @RequestBody NoteRequest request) {
        Long userId = getCurrentUserId();
        NoteResponse response = noteService.updateNote(userId, id, request);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNote(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        noteService.deleteNote(userId, id);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/pin")
    public ApiResponse<NoteResponse> togglePin(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        NoteResponse response = noteService.togglePin(userId, id);
        return ApiResponse.success(response);
    }

    @PatchMapping("/{id}/complete")
    public ApiResponse<NoteResponse> toggleComplete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        NoteResponse response = noteService.toggleComplete(userId, id);
        return ApiResponse.success(response);
    }
}
