package com.smartnotes.controller;

import com.smartnotes.dto.ApiResponse;
import com.smartnotes.dto.DocumentUploadResponse;
import com.smartnotes.dto.PageResponse;
import com.smartnotes.entity.Document;
import com.smartnotes.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * Upload a document.
     */
    @PostMapping("/upload")
    public ApiResponse<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file) {
        Long userId = getCurrentUserId();
        DocumentUploadResponse response = documentService.uploadDocument(userId, file);
        return ApiResponse.success(response);
    }

    /**
     * List all documents for the current user (paginated).
     */
    @GetMapping
    public ApiResponse<PageResponse<DocumentUploadResponse>> listDocuments(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Long userId = getCurrentUserId();
        PageResponse<DocumentUploadResponse> response = documentService.listDocuments(userId, page, size);
        return ApiResponse.success(response);
    }

    /**
     * Get a single document's metadata by ID.
     */
    @GetMapping("/{id}")
    public ApiResponse<DocumentUploadResponse> getDocument(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        DocumentUploadResponse response = documentService.getDocument(userId, id);
        return ApiResponse.success(response);
    }

    /**
     * Download a document file by ID. Returns the raw file resource directly
     * for successful downloads. Error responses use ApiResponse via exception handling.
     */
    @GetMapping(value = "/{id}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        Document document = documentService.downloadDocument(userId, id);
        Resource resource = documentService.getDocumentResource(document);

        String safeName = sanitizeFilename(document.getOriginalFilename());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + safeName + "\"")
                .body(resource);
    }

    /**
     * Sanitize a filename to prevent path traversal attacks.
     * Strips path separators, truncates to 255 chars, and URL-encodes the result.
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) return "download";
        // Strip any path separators
        String safe = filename.replaceAll("[\\\\/]", "_");
        // Truncate to 255 characters
        if (safe.length() > 255) {
            safe = safe.substring(0, 255);
        }
        return URLEncoder.encode(safe, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * Soft-delete a document by ID.
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDocument(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        documentService.deleteDocument(userId, id);
        return ApiResponse.success();
    }

    /**
     * Extract the current authenticated user's ID from the security context.
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
