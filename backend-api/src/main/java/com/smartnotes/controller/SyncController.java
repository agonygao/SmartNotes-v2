package com.smartnotes.controller;

import com.smartnotes.dto.ApiResponse;
import com.smartnotes.dto.SyncConflictResponse;
import com.smartnotes.dto.SyncPullResponse;
import com.smartnotes.dto.SyncPushRequest;
import com.smartnotes.dto.SyncPushResponse;
import com.smartnotes.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;

    /**
     * Push local changes to the server.
     */
    @PostMapping("/push")
    public ApiResponse<SyncPushResponse> push(@RequestBody List<SyncPushRequest> changes) {
        Long userId = getCurrentUserId();
        log.info("Sync push: userId={}, changesCount={}", userId, changes != null ? changes.size() : 0);
        SyncPushResponse response = syncService.push(userId, changes);
        return ApiResponse.success(response);
    }

    /**
     * Pull server changes since the given cursor.
     */
    @GetMapping("/pull")
    public ApiResponse<SyncPullResponse> pull(
            @RequestParam(defaultValue = "0") Long cursor,
            @RequestParam(defaultValue = "100") int pageSize) {
        Long userId = getCurrentUserId();
        log.info("Sync pull: userId={}, cursor={}, pageSize={}", userId, cursor, pageSize);
        SyncPullResponse response = syncService.pull(userId, cursor, pageSize);
        return ApiResponse.success(response);
    }

    /**
     * Get current sync status for the authenticated user.
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        Long userId = getCurrentUserId();
        log.info("Sync status: userId={}", userId);
        Map<String, Object> status = syncService.getSyncStatus(userId);
        return ApiResponse.success(status);
    }

    /**
     * Get unresolved sync conflicts for the authenticated user.
     */
    @GetMapping("/conflicts")
    public ApiResponse<SyncConflictResponse> conflicts() {
        Long userId = getCurrentUserId();
        log.info("Sync conflicts: userId={}", userId);
        SyncConflictResponse response = syncService.getConflicts(userId);
        return ApiResponse.success(response);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
