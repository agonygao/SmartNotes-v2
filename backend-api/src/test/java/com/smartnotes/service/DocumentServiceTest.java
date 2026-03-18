package com.smartnotes.service;

import com.smartnotes.dto.DocumentUploadResponse;
import com.smartnotes.dto.ErrorCode;
import com.smartnotes.dto.PageResponse;
import com.smartnotes.entity.Document;
import com.smartnotes.exception.BusinessException;
import com.smartnotes.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DocumentService Unit Tests")
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private DocumentService documentService;

    @TempDir
    Path tempDir;

    private Long userId = 1L;

    @BeforeEach
    void setUp() throws IOException {
        ReflectionTestUtils.setField(documentService, "uploadDir", tempDir.toString());
        Files.createDirectories(tempDir);
    }

    private Document createDocument(Long id, String filename, String originalFilename,
                                    String fileType, long fileSize, boolean previewAvailable) {
        Document doc = new Document();
        doc.setId(id);
        doc.setUserId(userId);
        doc.setFilename(filename);
        doc.setOriginalFilename(originalFilename);
        doc.setFileType(fileType);
        doc.setFileSize(fileSize);
        doc.setFilePath(userId + "/" + filename);
        doc.setPreviewAvailable(previewAvailable);
        doc.setDeleted(false);
        doc.setVersion(1);
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        return doc;
    }

    // ==================== uploadDocument Tests ====================

    @Nested
    @DisplayName("uploadDocument()")
    class UploadDocumentTests {

        @Test
        @DisplayName("should upload a PDF file successfully")
        void upload_pdf_success() throws IOException {
            when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
            when(multipartFile.getSize()).thenReturn(1024L);
            when(multipartFile.getContentType()).thenReturn("application/pdf");
            doNothing().when(multipartFile).transferTo(any(File.class));
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
                Document doc = inv.getArgument(0);
                doc.setId(1L);
                doc.setCreatedAt(LocalDateTime.now());
                doc.setUpdatedAt(LocalDateTime.now());
                return doc;
            });

            DocumentUploadResponse result = documentService.uploadDocument(userId, multipartFile);

            assertThat(result).isNotNull();
            assertThat(result.getOriginalFilename()).isEqualTo("test.pdf");
            assertThat(result.getFileType()).isEqualTo("pdf");
            assertThat(result.getFileSize()).isEqualTo(1024L);
            assertThat(result.getPreviewAvailable()).isTrue();
            verify(documentRepository).save(any(Document.class));
        }

        @Test
        @DisplayName("should upload a markdown file with preview enabled")
        void upload_markdown_preview() throws IOException {
            when(multipartFile.getOriginalFilename()).thenReturn("notes.md");
            when(multipartFile.getSize()).thenReturn(500L);
            when(multipartFile.getContentType()).thenReturn("text/markdown");
            doNothing().when(multipartFile).transferTo(any(File.class));
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
                Document doc = inv.getArgument(0);
                doc.setId(2L);
                doc.setCreatedAt(LocalDateTime.now());
                doc.setUpdatedAt(LocalDateTime.now());
                return doc;
            });

            DocumentUploadResponse result = documentService.uploadDocument(userId, multipartFile);

            assertThat(result.getFileType()).isEqualTo("markdown");
            assertThat(result.getPreviewAvailable()).isTrue();
        }

        @Test
        @DisplayName("should upload a TXT file with preview enabled")
        void upload_txt_preview() throws IOException {
            when(multipartFile.getOriginalFilename()).thenReturn("readme.txt");
            when(multipartFile.getSize()).thenReturn(200L);
            when(multipartFile.getContentType()).thenReturn("text/plain");
            doNothing().when(multipartFile).transferTo(any(File.class));
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
                Document doc = inv.getArgument(0);
                doc.setId(3L);
                doc.setCreatedAt(LocalDateTime.now());
                doc.setUpdatedAt(LocalDateTime.now());
                return doc;
            });

            DocumentUploadResponse result = documentService.uploadDocument(userId, multipartFile);

            assertThat(result.getFileType()).isEqualTo("text");
            assertThat(result.getPreviewAvailable()).isTrue();
        }

        @Test
        @DisplayName("should upload a DOCX file without preview")
        void upload_docx_noPreview() throws IOException {
            when(multipartFile.getOriginalFilename()).thenReturn("report.docx");
            when(multipartFile.getSize()).thenReturn(50000L);
            when(multipartFile.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            doNothing().when(multipartFile).transferTo(any(File.class));
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
                Document doc = inv.getArgument(0);
                doc.setId(4L);
                doc.setCreatedAt(LocalDateTime.now());
                doc.setUpdatedAt(LocalDateTime.now());
                return doc;
            });

            DocumentUploadResponse result = documentService.uploadDocument(userId, multipartFile);

            assertThat(result.getFileType()).isEqualTo("word");
            assertThat(result.getPreviewAvailable()).isFalse();
        }

        @Test
        @DisplayName("should upload an XLSX file without preview")
        void upload_xlsx_noPreview() throws IOException {
            when(multipartFile.getOriginalFilename()).thenReturn("data.xlsx");
            when(multipartFile.getSize()).thenReturn(10000L);
            when(multipartFile.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            doNothing().when(multipartFile).transferTo(any(File.class));
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
                Document doc = inv.getArgument(0);
                doc.setId(5L);
                doc.setCreatedAt(LocalDateTime.now());
                doc.setUpdatedAt(LocalDateTime.now());
                return doc;
            });

            DocumentUploadResponse result = documentService.uploadDocument(userId, multipartFile);

            assertThat(result.getFileType()).isEqualTo("excel");
            assertThat(result.getPreviewAvailable()).isFalse();
        }

        @Test
        @DisplayName("should upload a legacy DOC file without preview")
        void upload_doc_noPreview() throws IOException {
            when(multipartFile.getOriginalFilename()).thenReturn("legacy.doc");
            when(multipartFile.getSize()).thenReturn(30000L);
            when(multipartFile.getContentType()).thenReturn("application/msword");
            doNothing().when(multipartFile).transferTo(any(File.class));
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
                Document doc = inv.getArgument(0);
                doc.setId(6L);
                doc.setCreatedAt(LocalDateTime.now());
                doc.setUpdatedAt(LocalDateTime.now());
                return doc;
            });

            DocumentUploadResponse result = documentService.uploadDocument(userId, multipartFile);

            assertThat(result.getFileType()).isEqualTo("word");
            assertThat(result.getPreviewAvailable()).isFalse();
        }

        @Test
        @DisplayName("should upload a legacy XLS file without preview")
        void upload_xls_noPreview() throws IOException {
            when(multipartFile.getOriginalFilename()).thenReturn("legacy.xls");
            when(multipartFile.getSize()).thenReturn(15000L);
            when(multipartFile.getContentType()).thenReturn("application/vnd.ms-excel");
            doNothing().when(multipartFile).transferTo(any(File.class));
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
                Document doc = inv.getArgument(0);
                doc.setId(7L);
                doc.setCreatedAt(LocalDateTime.now());
                doc.setUpdatedAt(LocalDateTime.now());
                return doc;
            });

            DocumentUploadResponse result = documentService.uploadDocument(userId, multipartFile);

            assertThat(result.getFileType()).isEqualTo("excel");
            assertThat(result.getPreviewAvailable()).isFalse();
        }

        @Test
        @DisplayName("should handle filename with multiple dots correctly")
        void upload_multipleDotsInFilename() throws IOException {
            when(multipartFile.getOriginalFilename()).thenReturn("my.report.final.v2.pdf");
            when(multipartFile.getSize()).thenReturn(2048L);
            when(multipartFile.getContentType()).thenReturn("application/pdf");
            doNothing().when(multipartFile).transferTo(any(File.class));
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
                Document doc = inv.getArgument(0);
                doc.setId(8L);
                doc.setCreatedAt(LocalDateTime.now());
                doc.setUpdatedAt(LocalDateTime.now());
                return doc;
            });

            DocumentUploadResponse result = documentService.uploadDocument(userId, multipartFile);

            assertThat(result.getFileType()).isEqualTo("pdf");
            assertThat(result.getPreviewAvailable()).isTrue();
            assertThat(result.getOriginalFilename()).isEqualTo("my.report.final.v2.pdf");
        }

        @Test
        @DisplayName("should accept file at exactly 50MB size limit")
        void upload_exactlyMaxSize() throws IOException {
            when(multipartFile.getOriginalFilename()).thenReturn("exactly50mb.pdf");
            when(multipartFile.getSize()).thenReturn(50L * 1024 * 1024);
            when(multipartFile.getContentType()).thenReturn("application/pdf");
            doNothing().when(multipartFile).transferTo(any(File.class));
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
                Document doc = inv.getArgument(0);
                doc.setId(9L);
                doc.setCreatedAt(LocalDateTime.now());
                doc.setUpdatedAt(LocalDateTime.now());
                return doc;
            });

            DocumentUploadResponse result = documentService.uploadDocument(userId, multipartFile);

            assertThat(result).isNotNull();
            assertThat(result.getFileType()).isEqualTo("pdf");
            verify(documentRepository).save(any(Document.class));
        }

        @Test
        @DisplayName("should throw UNSUPPORTED_FILE_TYPE for unsupported extension")
        void upload_unsupportedType() {
            when(multipartFile.getOriginalFilename()).thenReturn("malware.exe");

            assertThatThrownBy(() -> documentService.uploadDocument(userId, multipartFile))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE);

            verify(documentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BAD_REQUEST for null filename")
        void upload_nullFilename() {
            when(multipartFile.getOriginalFilename()).thenReturn(null);

            assertThatThrownBy(() -> documentService.uploadDocument(userId, multipartFile))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.BAD_REQUEST);
        }

        @Test
        @DisplayName("should throw BAD_REQUEST for blank filename")
        void upload_blankFilename() {
            when(multipartFile.getOriginalFilename()).thenReturn("   ");

            assertThatThrownBy(() -> documentService.uploadDocument(userId, multipartFile))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.BAD_REQUEST);
        }

        @Test
        @DisplayName("should throw UNSUPPORTED_FILE_TYPE for filename without extension")
        void upload_noExtension() {
            when(multipartFile.getOriginalFilename()).thenReturn("noext");

            assertThatThrownBy(() -> documentService.uploadDocument(userId, multipartFile))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }

        @Test
        @DisplayName("should throw FILE_TOO_LARGE for files exceeding 50MB")
        void upload_fileTooLarge() {
            when(multipartFile.getOriginalFilename()).thenReturn("huge.pdf");
            when(multipartFile.getSize()).thenReturn(51L * 1024 * 1024);

            assertThatThrownBy(() -> documentService.uploadDocument(userId, multipartFile))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.FILE_TOO_LARGE);

            verify(documentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should handle case-insensitive file extensions")
        void upload_caseInsensitiveExtension() throws IOException {
            when(multipartFile.getOriginalFilename()).thenReturn("test.PDF");
            when(multipartFile.getSize()).thenReturn(1024L);
            when(multipartFile.getContentType()).thenReturn("application/pdf");
            doNothing().when(multipartFile).transferTo(any(File.class));
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
                Document doc = inv.getArgument(0);
                doc.setId(6L);
                doc.setCreatedAt(LocalDateTime.now());
                doc.setUpdatedAt(LocalDateTime.now());
                return doc;
            });

            DocumentUploadResponse result = documentService.uploadDocument(userId, multipartFile);

            assertThat(result.getFileType()).isEqualTo("pdf");
        }

        @Test
        @DisplayName("should throw FILE_UPLOAD_ERROR when transferTo fails")
        void upload_ioError() throws IOException {
            when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
            when(multipartFile.getSize()).thenReturn(1024L);
            doThrow(new IOException("Disk full")).when(multipartFile).transferTo(any(File.class));

            assertThatThrownBy(() -> documentService.uploadDocument(userId, multipartFile))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.FILE_UPLOAD_ERROR);

            verify(documentRepository, never()).save(any());
        }
    }

    // ==================== getDocument Tests ====================

    @Nested
    @DisplayName("getDocument()")
    class GetDocumentTests {

        @Test
        @DisplayName("should return document metadata")
        void getDocument_success() {
            Document doc = createDocument(1L, "uuid.pdf", "test.pdf", "pdf", 1024, true);
            when(documentRepository.findByIdAndUserIdAndDeletedFalse(1L, userId))
                    .thenReturn(Optional.of(doc));

            DocumentUploadResponse result = documentService.getDocument(userId, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getOriginalFilename()).isEqualTo("test.pdf");
            assertThat(result.getFileType()).isEqualTo("pdf");
        }

        @Test
        @DisplayName("should throw DOCUMENT_NOT_FOUND for non-existent document")
        void getDocument_notFound() {
            when(documentRepository.findByIdAndUserIdAndDeletedFalse(999L, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.getDocument(userId, 999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("should enforce ownership check")
        void getDocument_ownershipCheck() {
            Long otherUserId = 2L;
            when(documentRepository.findByIdAndUserIdAndDeletedFalse(1L, otherUserId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.getDocument(otherUserId, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.DOCUMENT_NOT_FOUND);
        }
    }

    // ==================== listDocuments Tests ====================

    @Nested
    @DisplayName("listDocuments()")
    class ListDocumentsTests {

        @Test
        @DisplayName("should list documents for user")
        void listDocuments_success() {
            Document doc1 = createDocument(1L, "uuid.pdf", "test.pdf", "pdf", 1024, true);
            Document doc2 = createDocument(2L, "uuid.docx", "report.docx", "word", 50000, false);
            Page<Document> page = new PageImpl<>(List.of(doc1, doc2));
            when(documentRepository.findByUserIdAndDeletedFalse(eq(userId), any(Pageable.class)))
                    .thenReturn(page);

            PageResponse<DocumentUploadResponse> result = documentService.listDocuments(userId, 0, 10);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();
        }

        @Test
        @DisplayName("should return empty list when no documents exist")
        void listDocuments_empty() {
            Page<Document> emptyPage = new PageImpl<>(List.of());
            when(documentRepository.findByUserIdAndDeletedFalse(eq(userId), any(Pageable.class)))
                    .thenReturn(emptyPage);

            PageResponse<DocumentUploadResponse> result = documentService.listDocuments(userId, 0, 10);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("should handle pagination correctly")
        void listDocuments_pagination() {
            Document doc = createDocument(1L, "uuid.pdf", "test.pdf", "pdf", 1024, true);
            Page<Document> page = new PageImpl<>(List.of(doc), org.springframework.data.domain.PageRequest.of(1, 5), 11);
            when(documentRepository.findByUserIdAndDeletedFalse(eq(userId), any(Pageable.class)))
                    .thenReturn(page);

            PageResponse<DocumentUploadResponse> result = documentService.listDocuments(userId, 1, 5);

            assertThat(result.getPage()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(5);
            assertThat(result.getTotalElements()).isEqualTo(11);
            assertThat(result.getTotalPages()).isEqualTo(3);
            assertThat(result.isFirst()).isFalse();
        }
    }

    // ==================== deleteDocument Tests ====================

    @Nested
    @DisplayName("deleteDocument()")
    class DeleteDocumentTests {

        @Test
        @DisplayName("should soft delete a document")
        void deleteDocument_success() throws IOException {
            Document doc = createDocument(1L, "uuid.pdf", "test.pdf", "pdf", 1024, true);
            Path filePath = tempDir.resolve(userId.toString()).resolve("uuid.pdf");
            Files.createDirectories(filePath.getParent());
            Files.createFile(filePath);

            when(documentRepository.findByIdAndUserIdAndDeletedFalse(1L, userId))
                    .thenReturn(Optional.of(doc));
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            documentService.deleteDocument(userId, 1L);

            verify(documentRepository).save(argThat(d -> Boolean.TRUE.equals(d.getDeleted())));
        }

        @Test
        @DisplayName("should throw DOCUMENT_NOT_FOUND for non-existent document")
        void deleteDocument_notFound() {
            when(documentRepository.findByIdAndUserIdAndDeletedFalse(999L, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.deleteDocument(userId, 999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.DOCUMENT_NOT_FOUND);

            verify(documentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should handle missing physical file gracefully on delete")
        void deleteDocument_fileNotFoundOnDisk() {
            Document doc = createDocument(1L, "uuid.pdf", "test.pdf", "pdf", 1024, true);
            when(documentRepository.findByIdAndUserIdAndDeletedFalse(1L, userId))
                    .thenReturn(Optional.of(doc));
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            // Should not throw - just logs a warning
            documentService.deleteDocument(userId, 1L);

            verify(documentRepository).save(argThat(d -> Boolean.TRUE.equals(d.getDeleted())));
        }
    }

    // ==================== downloadDocument Tests ====================

    @Nested
    @DisplayName("downloadDocument()")
    class DownloadDocumentTests {

        @Test
        @DisplayName("should return document when file exists on disk")
        void downloadDocument_success() throws IOException {
            Document doc = createDocument(1L, "uuid.pdf", "test.pdf", "pdf", 1024, true);
            Path filePath = tempDir.resolve(userId.toString()).resolve("uuid.pdf");
            Files.createDirectories(filePath.getParent());
            Files.createFile(filePath);

            when(documentRepository.findByIdAndUserIdAndDeletedFalse(1L, userId))
                    .thenReturn(Optional.of(doc));

            Document result = documentService.downloadDocument(userId, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw DOCUMENT_NOT_FOUND when document not in DB")
        void downloadDocument_notFound() {
            when(documentRepository.findByIdAndUserIdAndDeletedFalse(999L, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.downloadDocument(userId, 999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("should throw FILE_UPLOAD_ERROR when file missing on disk")
        void downloadDocument_fileMissingOnDisk() {
            Document doc = createDocument(1L, "uuid.pdf", "test.pdf", "pdf", 1024, true);
            when(documentRepository.findByIdAndUserIdAndDeletedFalse(1L, userId))
                    .thenReturn(Optional.of(doc));

            assertThatThrownBy(() -> documentService.downloadDocument(userId, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.FILE_UPLOAD_ERROR);
        }
    }

    // ==================== convertToResponse Tests ====================

    @Nested
    @DisplayName("convertToResponse()")
    class ConvertToResponseTests {

        @Test
        @DisplayName("should correctly map all Document fields to DTO")
        void convertToResponse_allFields() {
            LocalDateTime now = LocalDateTime.now();
            Document doc = createDocument(1L, "uuid.pdf", "test.pdf", "pdf", 1024, true);
            doc.setCreatedAt(now);

            DocumentUploadResponse response = documentService.convertToResponse(doc);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getFilename()).isEqualTo("uuid.pdf");
            assertThat(response.getOriginalFilename()).isEqualTo("test.pdf");
            assertThat(response.getFileType()).isEqualTo("pdf");
            assertThat(response.getFileSize()).isEqualTo(1024);
            assertThat(response.getPreviewAvailable()).isTrue();
            assertThat(response.getCreatedAt()).isEqualTo(now);
        }
    }

    // ==================== init Tests ====================

    @Nested
    @DisplayName("init()")
    class InitTests {

        @Test
        @DisplayName("should create upload directory on init")
        void init_createsDirectory() throws IOException {
            Path newDir = tempDir.resolve("new-upload-dir");
            ReflectionTestUtils.setField(documentService, "uploadDir", newDir.toString());

            documentService.init();

            assertThat(Files.exists(newDir)).isTrue();
        }
    }
}
