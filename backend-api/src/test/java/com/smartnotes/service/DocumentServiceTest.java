package com.smartnotes.service;

import com.smartnotes.dto.DocumentUploadResponse;
import com.smartnotes.dto.ErrorCode;
import com.smartnotes.dto.PageResponse;
import com.smartnotes.entity.Document;
import com.smartnotes.exception.BusinessException;
import com.smartnotes.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentService Unit Tests")
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private DocumentService documentService;

    private Long userId = 1L;
    private Document existingDocument;

    @BeforeEach
    void setUp() {
        // Set the private uploadDir field using ReflectionTestUtils
        ReflectionTestUtils.setField(documentService, "uploadDir", System.getProperty("java.io.tmpdir") + "/test-uploads");

        existingDocument = new Document();
        existingDocument.setId(10L);
        existingDocument.setUserId(userId);
        existingDocument.setFilename("uuid-abc123.md");
        existingDocument.setOriginalFilename("test-document.md");
        existingDocument.setFileType("markdown");
        existingDocument.setFileSize(2048L);
        existingDocument.setFilePath(userId + "/uuid-abc123.md");
        existingDocument.setMimeType("text/markdown");
        existingDocument.setPreviewAvailable(true);
        existingDocument.setDeleted(false);
        existingDocument.setCreatedAt(java.time.LocalDateTime.now());
        existingDocument.setUpdatedAt(java.time.LocalDateTime.now());
    }

    @Test
    @DisplayName("uploadDocument - should successfully upload a valid file")
    void uploadDocument_success() throws IOException {
        when(multipartFile.getOriginalFilename()).thenReturn("test-document.md");
        when(multipartFile.getSize()).thenReturn(2048L);
        when(multipartFile.getContentType()).thenReturn("text/markdown");
        // TransferTo needs to succeed without actually writing
        doNothing().when(multipartFile).transferTo(any(java.io.File.class));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.setId(1L);
            doc.setCreatedAt(java.time.LocalDateTime.now());
            doc.setUpdatedAt(java.time.LocalDateTime.now());
            return doc;
        });

        DocumentUploadResponse result = documentService.uploadDocument(userId, multipartFile);

        assertNotNull(result);
        assertNotNull(result.getFilename());
        assertTrue(result.getFilename().endsWith(".md"));
        assertEquals("test-document.md", result.getOriginalFilename());
        assertEquals("markdown", result.getFileType());
        assertEquals(2048L, result.getFileSize());
        assertTrue(result.getPreviewAvailable(), "Markdown files should be previewable");

        verify(documentRepository).save(argThat(doc ->
                doc.getUserId().equals(userId) &&
                "test-document.md".equals(doc.getOriginalFilename()) &&
                "markdown".equals(doc.getFileType()) &&
                Boolean.TRUE.equals(doc.getPreviewAvailable())
        ));
    }

    @Test
    @DisplayName("uploadDocument - should throw exception when filename is empty")
    void uploadDocument_emptyFilename() {
        when(multipartFile.getOriginalFilename()).thenReturn("");
        when(multipartFile.getOriginalFilename()).thenReturn("");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> documentService.uploadDocument(userId, multipartFile));

        assertEquals(ErrorCode.BAD_REQUEST, exception.getCode());

        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    @DisplayName("uploadDocument - should throw exception when filename is null")
    void uploadDocument_nullFilename() {
        when(multipartFile.getOriginalFilename()).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> documentService.uploadDocument(userId, multipartFile));

        assertEquals(ErrorCode.BAD_REQUEST, exception.getCode());

        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    @DisplayName("uploadDocument - should throw exception for unsupported file type")
    void uploadDocument_unsupportedType() {
        when(multipartFile.getOriginalFilename()).thenReturn("malware.exe");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> documentService.uploadDocument(userId, multipartFile));

        assertEquals(ErrorCode.UNSUPPORTED_FILE_TYPE, exception.getCode());
        assertNotNull(exception.getDetail());
        assertTrue(exception.getDetail().contains("exe"), "Detail should mention the unsupported extension");

        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    @DisplayName("uploadDocument - should throw exception when file exceeds size limit")
    void uploadDocument_fileTooLarge() {
        when(multipartFile.getOriginalFilename()).thenReturn("huge-file.pdf");
        when(multipartFile.getSize()).thenReturn(60L * 1024 * 1024); // 60MB > 50MB limit

        BusinessException exception = assertThrows(BusinessException.class,
                () -> documentService.uploadDocument(userId, multipartFile));

        assertEquals(ErrorCode.FILE_TOO_LARGE, exception.getCode());

        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    @DisplayName("uploadDocument - should accept PDF file")
    void uploadDocument_pdfType() throws IOException {
        when(multipartFile.getOriginalFilename()).thenReturn("report.pdf");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        doNothing().when(multipartFile).transferTo(any(java.io.File.class));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.setId(2L);
            doc.setCreatedAt(java.time.LocalDateTime.now());
            doc.setUpdatedAt(java.time.LocalDateTime.now());
            return doc;
        });

        DocumentUploadResponse result = documentService.uploadDocument(userId, multipartFile);

        assertNotNull(result);
        assertEquals("pdf", result.getFileType());
        assertTrue(result.getPreviewAvailable(), "PDF files should be previewable");

        verify(documentRepository).save(any(Document.class));
    }

    @Test
    @DisplayName("uploadDocument - should accept DOCX file with previewAvailable=false")
    void uploadDocument_docxType() throws IOException {
        when(multipartFile.getOriginalFilename()).thenReturn("document.docx");
        when(multipartFile.getSize()).thenReturn(5120L);
        when(multipartFile.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        doNothing().when(multipartFile).transferTo(any(java.io.File.class));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.setId(3L);
            doc.setCreatedAt(java.time.LocalDateTime.now());
            doc.setUpdatedAt(java.time.LocalDateTime.now());
            return doc;
        });

        DocumentUploadResponse result = documentService.uploadDocument(userId, multipartFile);

        assertNotNull(result);
        assertEquals("word", result.getFileType());
        assertFalse(result.getPreviewAvailable(), "DOCX files should not be previewable");
    }

    @Test
    @DisplayName("getDocument - should return document response for valid ID")
    void getDocument_success() {
        when(documentRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                .thenReturn(Optional.of(existingDocument));

        DocumentUploadResponse result = documentService.getDocument(userId, 10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("uuid-abc123.md", result.getFilename());
        assertEquals("test-document.md", result.getOriginalFilename());
        assertEquals("markdown", result.getFileType());
        assertEquals(2048L, result.getFileSize());
        assertTrue(result.getPreviewAvailable());

        verify(documentRepository).findByIdAndUserIdAndDeletedFalse(10L, userId);
    }

    @Test
    @DisplayName("getDocument - should throw exception when document not found")
    void getDocument_notFound() {
        when(documentRepository.findByIdAndUserIdAndDeletedFalse(999L, userId))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> documentService.getDocument(userId, 999L));

        assertEquals(ErrorCode.DOCUMENT_NOT_FOUND, exception.getCode());
    }

    @Test
    @DisplayName("listDocuments - should return paginated list of documents")
    void listDocuments_success() {
        List<Document> documents = List.of(existingDocument);
        Page<Document> documentPage = new PageImpl<>(documents);

        when(documentRepository.findByUserIdAndDeletedFalse(eq(userId), any(Pageable.class)))
                .thenReturn(documentPage);

        PageResponse<DocumentUploadResponse> result = documentService.listDocuments(userId, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertEquals("test-document.md", result.getContent().get(0).getOriginalFilename());

        verify(documentRepository).findByUserIdAndDeletedFalse(eq(userId), any(Pageable.class));
    }

    @Test
    @DisplayName("listDocuments - should return empty page when no documents exist")
    void listDocuments_empty() {
        Page<Document> emptyPage = new PageImpl<>(List.of());

        when(documentRepository.findByUserIdAndDeletedFalse(eq(userId), any(Pageable.class)))
                .thenReturn(emptyPage);

        PageResponse<DocumentUploadResponse> result = documentService.listDocuments(userId, 0, 10);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0L, result.getTotalElements());
    }

    @Test
    @DisplayName("deleteDocument - should soft delete an existing document")
    void deleteDocument_success() {
        when(documentRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                .thenReturn(Optional.of(existingDocument));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        documentService.deleteDocument(userId, 10L);

        verify(documentRepository).save(argThat(doc ->
                Boolean.TRUE.equals(doc.getDeleted())
        ));
    }

    @Test
    @DisplayName("deleteDocument - should throw exception when document not found")
    void deleteDocument_notFound() {
        when(documentRepository.findByIdAndUserIdAndDeletedFalse(999L, userId))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> documentService.deleteDocument(userId, 999L));

        assertEquals(ErrorCode.DOCUMENT_NOT_FOUND, exception.getCode());

        verify(documentRepository, never()).save(any(Document.class));
    }
}
