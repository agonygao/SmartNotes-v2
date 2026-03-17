package com.smartnotes.service;

import com.smartnotes.dto.DocumentUploadResponse;
import com.smartnotes.dto.ErrorCode;
import com.smartnotes.dto.PageResponse;
import com.smartnotes.entity.Document;
import com.smartnotes.exception.BusinessException;
import com.smartnotes.repository.DocumentRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    private static final Map<String, String> SUPPORTED_FILE_TYPES = Map.of(
            "md", "markdown",
            "txt", "text",
            "pdf", "pdf",
            "doc", "word",
            "docx", "word",
            "xls", "excel",
            "xlsx", "excel"
    );

    private static final Set<String> PREVIEWABLE_TYPES = Set.of("md", "txt", "pdf");

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            log.info("Upload directory initialized: {}", uploadDir);
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", uploadDir, e);
        }
    }

    @Transactional
    public DocumentUploadResponse uploadDocument(Long userId, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Filename must not be empty");
        }

        // Extract and validate file extension
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!SUPPORTED_FILE_TYPES.containsKey(extension)) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE,
                    "File extension '." + extension + "' is not supported");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE,
                    "File size exceeds the 50MB limit");
        }

        // Generate unique filename and determine storage paths
        String uniqueFilename = UUID.randomUUID() + "." + extension;
        String userDir = uploadDir + File.separator + userId;
        String relativePath = userId + File.separator + uniqueFilename;

        try {
            // Ensure user subdirectory exists
            Files.createDirectories(Paths.get(userDir));

            // Save file to disk
            File destination = new File(userDir, uniqueFilename);
            file.transferTo(destination);
            log.info("File saved to disk: {}", destination.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to save file to disk for user {}: {}", userId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR, "Could not save file to storage");
        }

        // Create and persist Document entity
        Document document = new Document();
        document.setUserId(userId);
        document.setFilename(uniqueFilename);
        document.setOriginalFilename(originalFilename);
        document.setFileType(SUPPORTED_FILE_TYPES.get(extension));
        document.setFileSize(file.getSize());
        document.setFilePath(relativePath);
        document.setMimeType(file.getContentType());
        document.setPreviewAvailable(PREVIEWABLE_TYPES.contains(extension));

        document = documentRepository.save(document);
        log.info("Document entity saved: id={}, filename={}", document.getId(), document.getFilename());

        return convertToResponse(document);
    }

    @Transactional(readOnly = true)
    public DocumentUploadResponse getDocument(Long userId, Long id) {
        Document document = documentRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));
        return convertToResponse(document);
    }

    @Transactional(readOnly = true)
    public PageResponse<DocumentUploadResponse> listDocuments(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Document> documentPage = documentRepository.findByUserIdAndDeletedFalse(userId, pageable);

        return PageResponse.<DocumentUploadResponse>builder()
                .content(documentPage.getContent().stream().map(this::convertToResponse).toList())
                .totalElements(documentPage.getTotalElements())
                .totalPages(documentPage.getTotalPages())
                .page(documentPage.getNumber())
                .size(documentPage.getSize())
                .first(documentPage.isFirst())
                .last(documentPage.isLast())
                .build();
    }

    @Transactional
    public void deleteDocument(Long userId, Long id) {
        Document document = documentRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));

        // Soft delete
        document.setDeleted(true);
        documentRepository.save(document);

        // Optionally delete physical file
        Path filePath = Paths.get(uploadDir, document.getFilePath());
        try {
            Files.deleteIfExists(filePath);
            log.info("Physical file deleted: {}", filePath);
        } catch (IOException e) {
            log.warn("Failed to delete physical file: {}, error: {}", filePath, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Document downloadDocument(Long userId, Long id) {
        Document document = documentRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));

        Path filePath = Paths.get(uploadDir, document.getFilePath());
        Resource resource = new FileSystemResource(filePath);

        if (!resource.exists() || !resource.isReadable()) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR, "File not found on disk");
        }

        return document;
    }

    /**
     * Build a FileSystemResource from a document's stored file path.
     */
    public Resource getDocumentResource(Document document) {
        Path filePath = Paths.get(uploadDir, document.getFilePath());
        return new FileSystemResource(filePath);
    }

    /**
     * Maps a Document entity to a DocumentUploadResponse DTO.
     */
    public DocumentUploadResponse convertToResponse(Document document) {
        return DocumentUploadResponse.builder()
                .id(document.getId())
                .filename(document.getFilename())
                .originalFilename(document.getOriginalFilename())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .previewAvailable(document.getPreviewAvailable())
                .createdAt(document.getCreatedAt())
                .build();
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }
}
