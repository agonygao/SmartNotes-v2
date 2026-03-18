package com.smartnotes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartnotes.dto.SyncPushRequest;
import com.smartnotes.dto.SyncPushResponse;
import com.smartnotes.dto.SyncPullResponse;
import com.smartnotes.dto.SyncConflictResponse;
import com.smartnotes.dto.ErrorCode;
import com.smartnotes.entity.*;
import com.smartnotes.exception.BusinessException;
import com.smartnotes.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SyncService Unit Tests")
class SyncServiceTest {

    @Mock private SyncLogRepository syncLogRepository;
    @Mock private SyncCursorRepository syncCursorRepository;
    @Mock private ConflictLogRepository conflictLogRepository;
    @Mock private NoteRepository noteRepository;
    @Mock private WordBookRepository wordBookRepository;
    @Mock private WordRepository wordRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private EntityManager entityManager;
    @Mock private TypedQuery<BaseEntity> typedQuery;

    @InjectMocks
    private SyncService syncService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        try {
            var field = SyncService.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(syncService, objectMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SyncCursor createCursor(Long userId, long cursor) {
        SyncCursor sc = new SyncCursor();
        sc.setId(1L);
        sc.setUserId(userId);
        sc.setCursor(cursor);
        sc.setUpdatedAt(LocalDateTime.now());
        return sc;
    }

    // ==================== Push Tests ====================

    @Nested
    @DisplayName("push()")
    class PushTests {

        @Test
        @DisplayName("should return empty results for null changes list")
        void push_nullChanges() {
            SyncPushResponse result = syncService.push(userId, null);
            assertThat(result.getResults()).isEmpty();
            verify(syncCursorRepository, never()).save(any());
        }

        @Test
        @DisplayName("should return empty results for empty changes list")
        void push_emptyChanges() {
            SyncPushResponse result = syncService.push(userId, List.of());
            assertThat(result.getResults()).isEmpty();
            verify(syncCursorRepository, never()).save(any());
        }

        @Test
        @DisplayName("should ACCEPT a valid CREATE for NOTE entity")
        void push_createNote_success() {
            SyncCursor cursor = createCursor(userId, 0);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));
            when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(inv -> inv.getArgument(0));
            when(entityManager.createQuery(anyString(), eq(BaseEntity.class))).thenReturn(typedQuery);
            when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
            when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);
            when(typedQuery.getResultList()).thenReturn(List.of());

            Note savedNote = new Note();
            savedNote.setId(100L);
            savedNote.setVersion(1);
            when(noteRepository.save(any(Note.class))).thenReturn(savedNote);
            when(syncLogRepository.save(any(SyncLog.class))).thenAnswer(inv -> inv.getArgument(0));

            SyncPushRequest change = SyncPushRequest.builder()
                    .entityType("NOTE")
                    .clientId("client-note-1")
                    .action("CREATE")
                    .data("{\"title\":\"Synced Note\",\"content\":\"Hello\"}")
                    .build();

            SyncPushResponse result = syncService.push(userId, List.of(change));

            assertThat(result.getResults()).hasSize(1);
            SyncPushResponse.SyncResultEntry entry = result.getResults().get(0);
            assertThat(entry.getStatus()).isEqualTo("ACCEPTED");
            assertThat(entry.getEntityType()).isEqualTo("NOTE");
            assertThat(entry.getEntityId()).isEqualTo(100L);
            assertThat(entry.getClientId()).isEqualTo("client-note-1");
            verify(noteRepository).save(any(Note.class));
            verify(syncLogRepository).save(any(SyncLog.class));
        }

        @Test
        @DisplayName("should return CONFLICT when creating entity with existing clientId")
        void push_create_conflict() {
            SyncCursor cursor = createCursor(userId, 0);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));
            when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(inv -> inv.getArgument(0));

            Note existingNote = new Note();
            existingNote.setId(100L);
            existingNote.setVersion(1);
            when(entityManager.createQuery(anyString(), eq(BaseEntity.class))).thenReturn(typedQuery);
            when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
            when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);
            when(typedQuery.getResultList()).thenReturn(List.of(existingNote));

            SyncPushRequest change = SyncPushRequest.builder()
                    .entityType("NOTE")
                    .clientId("client-note-1")
                    .action("CREATE")
                    .data("{\"title\":\"Dup Note\"}")
                    .build();

            SyncPushResponse result = syncService.push(userId, List.of(change));

            assertThat(result.getResults()).hasSize(1);
            assertThat(result.getResults().get(0).getStatus()).isEqualTo("CONFLICT");
            assertThat(result.getResults().get(0).getEntityId()).isEqualTo(100L);
            verify(noteRepository, never()).save(any());
        }

        @Test
        @DisplayName("should ACCEPT UPDATE when versions match")
        void push_update_versionMatch() {
            SyncCursor cursor = createCursor(userId, 5);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));
            when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(inv -> inv.getArgument(0));

            Note existingNote = new Note();
            existingNote.setId(100L);
            existingNote.setVersion(2);
            when(noteRepository.findById(100L)).thenReturn(Optional.of(existingNote));
            when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));
            when(syncLogRepository.save(any(SyncLog.class))).thenAnswer(inv -> inv.getArgument(0));

            SyncPushRequest change = SyncPushRequest.builder()
                    .entityType("NOTE")
                    .entityId(100L)
                    .clientId("client-note-1")
                    .action("UPDATE")
                    .version(2)
                    .data("{\"title\":\"Updated\"}")
                    .build();

            SyncPushResponse result = syncService.push(userId, List.of(change));

            assertThat(result.getResults()).hasSize(1);
            assertThat(result.getResults().get(0).getStatus()).isEqualTo("ACCEPTED");
            assertThat(result.getResults().get(0).getServerVersion()).isEqualTo(3);
        }

        @Test
        @DisplayName("should return CONFLICT when UPDATE versions mismatch")
        void push_update_versionConflict() {
            SyncCursor cursor = createCursor(userId, 5);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));
            when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(inv -> inv.getArgument(0));

            Note existingNote = new Note();
            existingNote.setId(100L);
            existingNote.setVersion(3);
            when(noteRepository.findById(100L)).thenReturn(Optional.of(existingNote));
            when(conflictLogRepository.save(any(ConflictLog.class))).thenAnswer(inv -> inv.getArgument(0));

            SyncPushRequest change = SyncPushRequest.builder()
                    .entityType("NOTE")
                    .entityId(100L)
                    .clientId("client-note-1")
                    .action("UPDATE")
                    .version(1)
                    .data("{\"title\":\"Stale update\"}")
                    .build();

            SyncPushResponse result = syncService.push(userId, List.of(change));

            assertThat(result.getResults()).hasSize(1);
            assertThat(result.getResults().get(0).getStatus()).isEqualTo("CONFLICT");
            assertThat(result.getResults().get(0).getServerVersion()).isEqualTo(3);
            verify(conflictLogRepository).save(any(ConflictLog.class));
            verify(noteRepository, never()).save(any());
        }

        @Test
        @DisplayName("should return NOT_FOUND for UPDATE on non-existent entity")
        void push_update_notFound() {
            SyncCursor cursor = createCursor(userId, 5);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));
            when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(inv -> inv.getArgument(0));
            when(noteRepository.findById(999L)).thenReturn(Optional.empty());

            SyncPushRequest change = SyncPushRequest.builder()
                    .entityType("NOTE")
                    .entityId(999L)
                    .action("UPDATE")
                    .version(1)
                    .build();

            SyncPushResponse result = syncService.push(userId, List.of(change));

            assertThat(result.getResults().get(0).getStatus()).isEqualTo("NOT_FOUND");
        }

        @Test
        @DisplayName("should return INVALID_ENTITY_ID when UPDATE entityId is null")
        void push_update_nullEntityId() {
            SyncCursor cursor = createCursor(userId, 0);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));
            when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(inv -> inv.getArgument(0));

            SyncPushRequest change = SyncPushRequest.builder()
                    .entityType("NOTE")
                    .entityId(null)
                    .action("UPDATE")
                    .build();

            SyncPushResponse result = syncService.push(userId, List.of(change));

            assertThat(result.getResults().get(0).getStatus()).isEqualTo("INVALID_ENTITY_ID");
        }

        @Test
        @DisplayName("should ACCEPT DELETE for existing entity (soft delete)")
        void push_delete_existing() {
            SyncCursor cursor = createCursor(userId, 5);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));
            when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(inv -> inv.getArgument(0));

            Note existingNote = new Note();
            existingNote.setId(100L);
            existingNote.setDeleted(false);
            when(noteRepository.findById(100L)).thenReturn(Optional.of(existingNote));
            when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));
            when(syncLogRepository.save(any(SyncLog.class))).thenAnswer(inv -> inv.getArgument(0));

            SyncPushRequest change = SyncPushRequest.builder()
                    .entityType("NOTE")
                    .entityId(100L)
                    .action("DELETE")
                    .build();

            SyncPushResponse result = syncService.push(userId, List.of(change));

            assertThat(result.getResults().get(0).getStatus()).isEqualTo("ACCEPTED");
            verify(noteRepository).save(argThat(note -> Boolean.TRUE.equals(note.getDeleted())));
        }

        @Test
        @DisplayName("should ACCEPT DELETE for non-existent entity (idempotent)")
        void push_delete_notFound() {
            SyncCursor cursor = createCursor(userId, 5);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));
            when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(inv -> inv.getArgument(0));
            when(noteRepository.findById(999L)).thenReturn(Optional.empty());
            when(syncLogRepository.save(any(SyncLog.class))).thenAnswer(inv -> inv.getArgument(0));

            SyncPushRequest change = SyncPushRequest.builder()
                    .entityType("NOTE")
                    .entityId(999L)
                    .action("DELETE")
                    .build();

            SyncPushResponse result = syncService.push(userId, List.of(change));

            assertThat(result.getResults().get(0).getStatus()).isEqualTo("ACCEPTED");
        }

        @Test
        @DisplayName("should return INVALID_ENTITY_TYPE for unknown entity type")
        void push_invalidEntityType() {
            SyncCursor cursor = createCursor(userId, 0);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));
            when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(inv -> inv.getArgument(0));

            SyncPushRequest change = SyncPushRequest.builder()
                    .entityType("UNKNOWN")
                    .action("CREATE")
                    .build();

            SyncPushResponse result = syncService.push(userId, List.of(change));

            assertThat(result.getResults().get(0).getStatus()).isEqualTo("INVALID_ENTITY_TYPE");
        }

        @Test
        @DisplayName("should ACCEPT CREATE for WORD entity with bookId")
        void push_createWord_success() {
            SyncCursor cursor = createCursor(userId, 0);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));
            when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(inv -> inv.getArgument(0));
            when(entityManager.createQuery(anyString(), eq(BaseEntity.class))).thenReturn(typedQuery);
            when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
            when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);
            when(typedQuery.getResultList()).thenReturn(List.of());

            Word savedWord = new Word();
            savedWord.setId(300L);
            savedWord.setVersion(1);
            when(wordRepository.save(any(Word.class))).thenReturn(savedWord);
            when(syncLogRepository.save(any(SyncLog.class))).thenAnswer(inv -> inv.getArgument(0));

            SyncPushRequest change = SyncPushRequest.builder()
                    .entityType("WORD")
                    .entityId(10L) // bookId
                    .clientId("client-word-1")
                    .action("CREATE")
                    .data("{\"word\":\"hello\",\"meaning\":\"你好\"}")
                    .build();

            SyncPushResponse result = syncService.push(userId, List.of(change));

            assertThat(result.getResults().get(0).getStatus()).isEqualTo("ACCEPTED");
            assertThat(result.getResults().get(0).getEntityType()).isEqualTo("WORD");
            assertThat(result.getResults().get(0).getEntityId()).isEqualTo(300L);
            verify(wordRepository).save(any(Word.class));
        }

        @Test
        @DisplayName("should return INVALID_ACTION for null action (NPE caught)")
        void push_nullAction() {
            SyncCursor cursor = createCursor(userId, 0);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));
            when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(inv -> inv.getArgument(0));

            SyncPushRequest change = SyncPushRequest.builder()
                    .entityType("NOTE")
                    .action(null)
                    .build();

            SyncPushResponse result = syncService.push(userId, List.of(change));

            // null action causes NPE in toUpperCase(), caught by inner catch returning INVALID_ACTION
            assertThat(result.getResults().get(0).getStatus()).isEqualTo("INVALID_ACTION");
        }

        @Test
        @DisplayName("should return INVALID_ACTION for unknown action")
        void push_invalidAction() {
            SyncCursor cursor = createCursor(userId, 0);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));
            when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(inv -> inv.getArgument(0));

            SyncPushRequest change = SyncPushRequest.builder()
                    .entityType("NOTE")
                    .action("INVALID")
                    .build();

            SyncPushResponse result = syncService.push(userId, List.of(change));

            assertThat(result.getResults().get(0).getStatus()).isEqualTo("INVALID_ACTION");
        }

        @Test
        @DisplayName("should return ERROR for DOCUMENT CREATE (not supported from sync)")
        void push_createDocument_error() {
            SyncCursor cursor = createCursor(userId, 0);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));
            when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(inv -> inv.getArgument(0));
            when(entityManager.createQuery(anyString(), eq(BaseEntity.class))).thenReturn(typedQuery);
            when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
            when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);
            when(typedQuery.getResultList()).thenReturn(List.of());

            SyncPushRequest change = SyncPushRequest.builder()
                    .entityType("DOCUMENT")
                    .clientId("client-doc-1")
                    .action("CREATE")
                    .data("{}")
                    .build();

            SyncPushResponse result = syncService.push(userId, List.of(change));

            assertThat(result.getResults().get(0).getStatus()).isEqualTo("ERROR");
        }

        @Test
        @DisplayName("should handle entity type case-insensitively")
        void push_entityTypeCaseInsensitive() {
            SyncCursor cursor = createCursor(userId, 0);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));
            when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(inv -> inv.getArgument(0));
            when(entityManager.createQuery(anyString(), eq(BaseEntity.class))).thenReturn(typedQuery);
            when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
            when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);
            when(typedQuery.getResultList()).thenReturn(List.of());

            Note savedNote = new Note();
            savedNote.setId(100L);
            savedNote.setVersion(1);
            when(noteRepository.save(any(Note.class))).thenReturn(savedNote);
            when(syncLogRepository.save(any(SyncLog.class))).thenAnswer(inv -> inv.getArgument(0));

            SyncPushRequest change = SyncPushRequest.builder()
                    .entityType("note")
                    .clientId("client-1")
                    .action("create")
                    .data("{\"title\":\"Test\"}")
                    .build();

            SyncPushResponse result = syncService.push(userId, List.of(change));

            assertThat(result.getResults().get(0).getStatus()).isEqualTo("ACCEPTED");
        }

        @Test
        @DisplayName("should create cursor if not exists during push")
        void push_createsCursorIfNotExists() {
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(inv -> {
                SyncCursor sc = inv.getArgument(0);
                sc.setId(1L);
                return sc;
            });
            when(entityManager.createQuery(anyString(), eq(BaseEntity.class))).thenReturn(typedQuery);
            when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
            when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);
            when(typedQuery.getResultList()).thenReturn(List.of());

            Note savedNote = new Note();
            savedNote.setId(100L);
            savedNote.setVersion(1);
            when(noteRepository.save(any(Note.class))).thenReturn(savedNote);
            when(syncLogRepository.save(any(SyncLog.class))).thenAnswer(inv -> inv.getArgument(0));

            SyncPushRequest change = SyncPushRequest.builder()
                    .entityType("NOTE")
                    .clientId("client-1")
                    .action("CREATE")
                    .data("{\"title\":\"Test\"}")
                    .build();

            SyncPushResponse result = syncService.push(userId, List.of(change));

            assertThat(result.getResults().get(0).getStatus()).isEqualTo("ACCEPTED");
            // save is called twice: once in getOrCreateCursor, once at end of push
            verify(syncCursorRepository, times(2)).save(any(SyncCursor.class));
        }

        @Test
        @DisplayName("should handle multiple changes in one push")
        void push_multipleChanges() {
            SyncCursor cursor = createCursor(userId, 0);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));
            when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(inv -> inv.getArgument(0));
            when(entityManager.createQuery(anyString(), eq(BaseEntity.class))).thenReturn(typedQuery);
            when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
            when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);
            when(typedQuery.getResultList()).thenReturn(List.of());

            Note savedNote = new Note();
            savedNote.setId(100L);
            savedNote.setVersion(1);
            when(noteRepository.save(any(Note.class))).thenReturn(savedNote);
            when(syncLogRepository.save(any(SyncLog.class))).thenAnswer(inv -> inv.getArgument(0));
            when(noteRepository.findById(100L)).thenReturn(Optional.of(savedNote));

            SyncPushRequest create = SyncPushRequest.builder()
                    .entityType("NOTE")
                    .clientId("client-1")
                    .action("CREATE")
                    .data("{\"title\":\"Note 1\"}")
                    .build();

            SyncPushRequest update = SyncPushRequest.builder()
                    .entityType("NOTE")
                    .entityId(100L)
                    .action("UPDATE")
                    .version(1)
                    .data("{\"title\":\"Updated\"}")
                    .build();

            SyncPushResponse result = syncService.push(userId, List.of(create, update));

            assertThat(result.getResults()).hasSize(2);
            assertThat(result.getResults().get(0).getStatus()).isEqualTo("ACCEPTED");
            assertThat(result.getResults().get(1).getStatus()).isEqualTo("ACCEPTED");
        }

        @Test
        @DisplayName("should return ERROR status when entity creation fails")
        void push_createEntityFailure() {
            SyncCursor cursor = createCursor(userId, 0);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));
            when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(inv -> inv.getArgument(0));
            when(entityManager.createQuery(anyString(), eq(BaseEntity.class))).thenReturn(typedQuery);
            when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
            when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);
            when(typedQuery.getResultList()).thenReturn(List.of());
            when(noteRepository.save(any(Note.class))).thenThrow(new RuntimeException("DB error"));

            SyncPushRequest change = SyncPushRequest.builder()
                    .entityType("NOTE")
                    .clientId("client-1")
                    .action("CREATE")
                    .data("{\"title\":\"Bad Data\"}")
                    .build();

            SyncPushResponse result = syncService.push(userId, List.of(change));

            assertThat(result.getResults().get(0).getStatus()).isEqualTo("ERROR");
        }

        @Test
        @DisplayName("should return INVALID_ENTITY_ID when DELETE entityId is null")
        void push_delete_nullEntityId() {
            SyncCursor cursor = createCursor(userId, 0);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));
            when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(inv -> inv.getArgument(0));

            SyncPushRequest change = SyncPushRequest.builder()
                    .entityType("NOTE")
                    .entityId(null)
                    .action("DELETE")
                    .build();

            SyncPushResponse result = syncService.push(userId, List.of(change));

            assertThat(result.getResults().get(0).getStatus()).isEqualTo("INVALID_ENTITY_ID");
        }

        @Test
        @DisplayName("should ACCEPT CREATE for WORD_BOOK entity")
        void push_createWordBook_success() {
            SyncCursor cursor = createCursor(userId, 0);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));
            when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(inv -> inv.getArgument(0));
            when(entityManager.createQuery(anyString(), eq(BaseEntity.class))).thenReturn(typedQuery);
            when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
            when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);
            when(typedQuery.getResultList()).thenReturn(List.of());

            WordBook savedBook = new WordBook();
            savedBook.setId(200L);
            savedBook.setVersion(1);
            when(wordBookRepository.save(any(WordBook.class))).thenReturn(savedBook);
            when(syncLogRepository.save(any(SyncLog.class))).thenAnswer(inv -> inv.getArgument(0));

            SyncPushRequest change = SyncPushRequest.builder()
                    .entityType("WORD_BOOK")
                    .clientId("client-wb-1")
                    .action("CREATE")
                    .data("{\"name\":\"My Book\"}")
                    .build();

            SyncPushResponse result = syncService.push(userId, List.of(change));

            assertThat(result.getResults().get(0).getStatus()).isEqualTo("ACCEPTED");
            assertThat(result.getResults().get(0).getEntityType()).isEqualTo("WORD_BOOK");
            assertThat(result.getResults().get(0).getEntityId()).isEqualTo(200L);
        }
    }

    // ==================== Pull Tests ====================

    @Nested
    @DisplayName("pull()")
    class PullTests {

        @Test
        @DisplayName("should return empty changes when no logs exist after cursor")
        void pull_noChanges() {
            SyncCursor cursor = createCursor(userId, 10);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));
            when(syncLogRepository.findByUserIdAndSyncCursorGreaterThan(eq(userId), eq(10L), any(Pageable.class)))
                    .thenReturn(List.of());

            SyncPullResponse result = syncService.pull(userId, 10L, 100);

            assertThat(result.getChanges()).isEmpty();
            assertThat(result.getCursor()).isEqualTo(10L);
            assertThat(result.getHasMore()).isFalse();
        }

        @Test
        @DisplayName("should return changes since cursor")
        void pull_withChanges() {
            SyncCursor cursor = createCursor(userId, 10);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));

            SyncLog log1 = new SyncLog();
            log1.setId(1L);
            log1.setUserId(userId);
            log1.setEntityType("NOTE");
            log1.setEntityId(100L);
            log1.setAction(SyncAction.CREATE);
            log1.setSyncCursor(11L);
            log1.setCreatedAt(LocalDateTime.now());

            when(syncLogRepository.findByUserIdAndSyncCursorGreaterThan(eq(userId), eq(5L), any(Pageable.class)))
                    .thenReturn(List.of(log1));

            SyncPullResponse result = syncService.pull(userId, 5L, 100);

            assertThat(result.getChanges()).hasSize(1);
            assertThat(result.getChanges().get(0).getEntityType()).isEqualTo("NOTE");
            assertThat(result.getChanges().get(0).getAction()).isEqualTo("CREATE");
            assertThat(result.getCursor()).isEqualTo(11L);
            assertThat(result.getHasMore()).isFalse();
        }

        @Test
        @DisplayName("should set hasMore true when more pages exist")
        void pull_hasMore() {
            SyncCursor cursor = createCursor(userId, 10);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));

            List<SyncLog> logs = new java.util.ArrayList<>();
            for (int i = 0; i < 101; i++) {
                SyncLog log = new SyncLog();
                log.setId((long) i);
                log.setUserId(userId);
                log.setEntityType("NOTE");
                log.setEntityId((long) i);
                log.setAction(SyncAction.CREATE);
                log.setSyncCursor(11L + i);
                log.setCreatedAt(LocalDateTime.now());
                logs.add(log);
            }

            when(syncLogRepository.findByUserIdAndSyncCursorGreaterThan(eq(userId), eq(5L), any(Pageable.class)))
                    .thenReturn(logs);

            SyncPullResponse result = syncService.pull(userId, 5L, 100);

            assertThat(result.getChanges()).hasSize(100);
            assertThat(result.getHasMore()).isTrue();
        }

        @Test
        @DisplayName("should clamp pageSize to MAX_PAGE_SIZE")
        void pull_pageSizeClamped() {
            SyncCursor cursor = createCursor(userId, 10);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));
            when(syncLogRepository.findByUserIdAndSyncCursorGreaterThan(eq(userId), eq(5L), any(Pageable.class)))
                    .thenReturn(List.of());

            syncService.pull(userId, 5L, 1000);
            verify(syncLogRepository).findByUserIdAndSyncCursorGreaterThan(eq(userId), eq(5L), any(Pageable.class));
        }

        @Test
        @DisplayName("should default pageSize to 100 when less than 1")
        void pull_defaultPageSize() {
            SyncCursor cursor = createCursor(userId, 10);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));
            when(syncLogRepository.findByUserIdAndSyncCursorGreaterThan(eq(userId), eq(5L), any(Pageable.class)))
                    .thenReturn(List.of());

            syncService.pull(userId, 5L, 0);
            verify(syncLogRepository).findByUserIdAndSyncCursorGreaterThan(eq(userId), eq(5L), any(Pageable.class));
        }

        @Test
        @DisplayName("should create cursor if not exists during pull")
        void pull_createsCursorIfNotExists() {
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(inv -> {
                SyncCursor sc = inv.getArgument(0);
                sc.setId(1L);
                return sc;
            });
            when(syncLogRepository.findByUserIdAndSyncCursorGreaterThan(eq(userId), eq(5L), any(Pageable.class)))
                    .thenReturn(List.of());

            SyncPullResponse result = syncService.pull(userId, 5L, 100);

            assertThat(result).isNotNull();
            verify(syncCursorRepository).save(any(SyncCursor.class));
        }

        @Test
        @DisplayName("should include entity data for non-DELETE actions")
        void pull_includesEntityData() {
            SyncCursor cursor = createCursor(userId, 10);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));

            SyncLog log1 = new SyncLog();
            log1.setId(1L);
            log1.setUserId(userId);
            log1.setEntityType("NOTE");
            log1.setEntityId(100L);
            log1.setAction(SyncAction.CREATE);
            log1.setSyncCursor(11L);
            log1.setCreatedAt(LocalDateTime.now());

            Note note = new Note();
            note.setId(100L);
            note.setTitle("Test Note");
            note.setVersion(1);
            note.setDeleted(false);

            when(syncLogRepository.findByUserIdAndSyncCursorGreaterThan(eq(userId), eq(5L), any(Pageable.class)))
                    .thenReturn(List.of(log1));
            when(noteRepository.findById(100L)).thenReturn(Optional.of(note));

            SyncPullResponse result = syncService.pull(userId, 5L, 100);

            assertThat(result.getChanges()).hasSize(1);
            assertThat(result.getChanges().get(0).getData()).isNotNull();
            assertThat(result.getChanges().get(0).getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("should not include data for DELETE actions")
        void pull_deleteNoData() {
            SyncCursor cursor = createCursor(userId, 10);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));

            SyncLog log1 = new SyncLog();
            log1.setId(1L);
            log1.setUserId(userId);
            log1.setEntityType("NOTE");
            log1.setEntityId(100L);
            log1.setAction(SyncAction.DELETE);
            log1.setSyncCursor(11L);
            log1.setCreatedAt(LocalDateTime.now());

            when(syncLogRepository.findByUserIdAndSyncCursorGreaterThan(eq(userId), eq(5L), any(Pageable.class)))
                    .thenReturn(List.of(log1));

            SyncPullResponse result = syncService.pull(userId, 5L, 100);

            assertThat(result.getChanges()).hasSize(1);
            assertThat(result.getChanges().get(0).getAction()).isEqualTo("DELETE");
            assertThat(result.getChanges().get(0).getData()).isNull();
        }

        @Test
        @DisplayName("should not include data for soft-deleted entities")
        void pull_softDeletedEntityNoData() {
            SyncCursor cursor = createCursor(userId, 10);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));

            SyncLog log1 = new SyncLog();
            log1.setId(1L);
            log1.setUserId(userId);
            log1.setEntityType("NOTE");
            log1.setEntityId(100L);
            log1.setAction(SyncAction.UPDATE);
            log1.setSyncCursor(11L);
            log1.setCreatedAt(LocalDateTime.now());

            Note deletedNote = new Note();
            deletedNote.setId(100L);
            deletedNote.setDeleted(true);

            when(syncLogRepository.findByUserIdAndSyncCursorGreaterThan(eq(userId), eq(5L), any(Pageable.class)))
                    .thenReturn(List.of(log1));
            when(noteRepository.findById(100L)).thenReturn(Optional.of(deletedNote));

            SyncPullResponse result = syncService.pull(userId, 5L, 100);

            assertThat(result.getChanges()).hasSize(1);
            assertThat(result.getChanges().get(0).getData()).isNull();
        }

        @Test
        @DisplayName("should include serverTimestamp from log createdAt")
        void pull_includesServerTimestamp() {
            SyncCursor cursor = createCursor(userId, 10);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));

            SyncLog log1 = new SyncLog();
            log1.setId(1L);
            log1.setUserId(userId);
            log1.setEntityType("NOTE");
            log1.setEntityId(100L);
            log1.setAction(SyncAction.CREATE);
            log1.setSyncCursor(11L);
            log1.setCreatedAt(LocalDateTime.of(2026, 3, 18, 12, 0, 0));

            when(syncLogRepository.findByUserIdAndSyncCursorGreaterThan(eq(userId), eq(5L), any(Pageable.class)))
                    .thenReturn(List.of(log1));

            SyncPullResponse result = syncService.pull(userId, 5L, 100);

            assertThat(result.getChanges().get(0).getServerTimestamp()).isNotNull();
            assertThat(result.getChanges().get(0).getServerTimestamp()).isGreaterThan(0);
        }
    }

    // ==================== SyncStatus Tests ====================

    @Nested
    @DisplayName("getSyncStatus()")
    class GetSyncStatusTests {

        @Test
        @DisplayName("should return sync status with pending changes count")
        void getSyncStatus_success() {
            SyncCursor cursor = createCursor(userId, 10);
            cursor.setLastSyncedAt(LocalDateTime.now().minusMinutes(5));
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));

            SyncLog pendingLog = new SyncLog();
            pendingLog.setId(1L);
            pendingLog.setUserId(userId);
            pendingLog.setSyncCursor(15L);
            when(syncLogRepository.findByUserIdAndSyncCursorGreaterThanOrderBySyncCursorAsc(userId, 10L))
                    .thenReturn(List.of(pendingLog));

            Map<String, Object> status = syncService.getSyncStatus(userId);

            assertThat(status.get("cursor")).isEqualTo(10L);
            assertThat(status.get("lastSyncedAt")).isNotNull();
            assertThat(((Number) status.get("pendingChanges")).longValue()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should return zero pending when no pending changes")
        void getSyncStatus_noPending() {
            SyncCursor cursor = createCursor(userId, 10);
            when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(cursor));
            when(syncLogRepository.findByUserIdAndSyncCursorGreaterThanOrderBySyncCursorAsc(userId, 10L))
                    .thenReturn(List.of());

            Map<String, Object> status = syncService.getSyncStatus(userId);

            assertThat(((Number) status.get("pendingChanges")).longValue()).isEqualTo(0L);
        }
    }

    // ==================== Conflicts Tests ====================

    @Nested
    @DisplayName("getConflicts()")
    class GetConflictsTests {

        @Test
        @DisplayName("should return unresolved conflicts")
        void getConflicts_success() {
            ConflictLog conflict = new ConflictLog();
            conflict.setId(1L);
            conflict.setUserId(userId);
            conflict.setEntityType("NOTE");
            conflict.setEntityId(100L);
            conflict.setClientId("client-1");
            conflict.setLocalVersion(1);
            conflict.setServerVersion(3);
            conflict.setLocalData("{\"title\":\"Local\"}");
            conflict.setServerData("{\"title\":\"Server\"}");
            conflict.setResolved(false);
            conflict.setCreatedAt(LocalDateTime.now());

            when(conflictLogRepository.findByUserIdAndResolvedFalse(userId))
                    .thenReturn(List.of(conflict));

            SyncConflictResponse result = syncService.getConflicts(userId);

            assertThat(result.getConflicts()).hasSize(1);
            SyncConflictResponse.ConflictEntry entry = result.getConflicts().get(0);
            assertThat(entry.getEntityType()).isEqualTo("NOTE");
            assertThat(entry.getLocalVersion()).isEqualTo(1);
            assertThat(entry.getServerVersion()).isEqualTo(3);
        }

        @Test
        @DisplayName("should return empty list when no unresolved conflicts")
        void getConflicts_empty() {
            when(conflictLogRepository.findByUserIdAndResolvedFalse(userId))
                    .thenReturn(List.of());

            SyncConflictResponse result = syncService.getConflicts(userId);

            assertThat(result.getConflicts()).isEmpty();
        }
    }

    // ==================== Resolve Conflict Tests ====================

    @Nested
    @DisplayName("resolveConflict()")
    class ResolveConflictTests {

        @Test
        @DisplayName("should resolve conflict with SERVER_WINS")
        void resolveConflict_serverWins() {
            ConflictLog conflict = new ConflictLog();
            conflict.setId(1L);
            conflict.setUserId(userId);
            conflict.setEntityType("NOTE");
            conflict.setEntityId(100L);
            conflict.setResolved(false);
            when(conflictLogRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(conflict));
            when(conflictLogRepository.save(any(ConflictLog.class))).thenAnswer(inv -> inv.getArgument(0));

            SyncConflictResponse.ConflictEntry result = syncService.resolveConflict(userId, 1L, "SERVER_WINS", null);

            assertThat(result).isNotNull();
            verify(conflictLogRepository).save(argThat(c -> Boolean.TRUE.equals(c.getResolved())));
            verify(noteRepository, never()).save(any());
        }

        @Test
        @DisplayName("should resolve conflict with LOCAL_WINS and apply data")
        void resolveConflict_localWins() {
            ConflictLog conflict = new ConflictLog();
            conflict.setId(1L);
            conflict.setUserId(userId);
            conflict.setEntityType("NOTE");
            conflict.setEntityId(100L);
            conflict.setLocalData("{\"title\":\"Local Title\"}");
            conflict.setResolved(false);
            when(conflictLogRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(conflict));
            when(conflictLogRepository.save(any(ConflictLog.class))).thenAnswer(inv -> inv.getArgument(0));

            Note existingNote = new Note();
            existingNote.setId(100L);
            existingNote.setTitle("Server Title");
            existingNote.setVersion(3);
            when(noteRepository.findById(100L)).thenReturn(Optional.of(existingNote));
            when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

            SyncConflictResponse.ConflictEntry result = syncService.resolveConflict(userId, 1L, "LOCAL_WINS", null);

            assertThat(result).isNotNull();
            verify(noteRepository).save(any(Note.class));
            verify(conflictLogRepository).save(argThat(c -> Boolean.TRUE.equals(c.getResolved())));
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when conflict already resolved")
        void resolveConflict_alreadyResolved() {
            ConflictLog conflict = new ConflictLog();
            conflict.setId(1L);
            conflict.setUserId(userId);
            conflict.setResolved(true);
            when(conflictLogRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(conflict));

            assertThatThrownBy(() -> syncService.resolveConflict(userId, 1L, "LOCAL_WINS", null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.BAD_REQUEST);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when conflict does not exist")
        void resolveConflict_notFound() {
            when(conflictLogRepository.findByIdAndUserId(999L, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> syncService.resolveConflict(userId, 999L, "LOCAL_WINS", null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }

        @Test
        @DisplayName("should throw BAD_REQUEST for invalid resolution")
        void resolveConflict_invalidResolution() {
            ConflictLog conflict = new ConflictLog();
            conflict.setId(1L);
            conflict.setUserId(userId);
            conflict.setResolved(false);
            when(conflictLogRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(conflict));

            assertThatThrownBy(() -> syncService.resolveConflict(userId, 1L, "INVALID", null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.BAD_REQUEST);
        }

        @Test
        @DisplayName("should use resolvedData when provided for LOCAL_WINS")
        void resolveConflict_localWinsWithResolvedData() {
            ConflictLog conflict = new ConflictLog();
            conflict.setId(1L);
            conflict.setUserId(userId);
            conflict.setEntityType("NOTE");
            conflict.setEntityId(100L);
            conflict.setLocalData("{\"title\":\"Old Local\"}");
            conflict.setResolved(false);
            when(conflictLogRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(conflict));
            when(conflictLogRepository.save(any(ConflictLog.class))).thenAnswer(inv -> inv.getArgument(0));

            Note existingNote = new Note();
            existingNote.setId(100L);
            existingNote.setTitle("Server Title");
            existingNote.setVersion(3);
            when(noteRepository.findById(100L)).thenReturn(Optional.of(existingNote));
            when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

            syncService.resolveConflict(userId, 1L, "LOCAL_WINS", "{\"title\":\"Merged Title\"}");

            verify(noteRepository).save(argThat(note -> "Merged Title".equals(note.getTitle())));
        }

        @Test
        @DisplayName("should handle LOCAL_WINS when entity no longer exists")
        void resolveConflict_localWins_entityGone() {
            ConflictLog conflict = new ConflictLog();
            conflict.setId(1L);
            conflict.setUserId(userId);
            conflict.setEntityType("NOTE");
            conflict.setEntityId(100L);
            conflict.setLocalData("{\"title\":\"Local\"}");
            conflict.setResolved(false);
            when(conflictLogRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(conflict));
            when(conflictLogRepository.save(any(ConflictLog.class))).thenAnswer(inv -> inv.getArgument(0));
            when(noteRepository.findById(100L)).thenReturn(Optional.empty());

            SyncConflictResponse.ConflictEntry result = syncService.resolveConflict(userId, 1L, "LOCAL_WINS", null);

            assertThat(result).isNotNull();
            verify(noteRepository, never()).save(any());
        }
    }
}
