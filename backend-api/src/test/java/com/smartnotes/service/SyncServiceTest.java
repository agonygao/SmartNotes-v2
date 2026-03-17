package com.smartnotes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartnotes.dto.*;
import com.smartnotes.entity.*;
import com.smartnotes.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SyncService Unit Tests")
class SyncServiceTest {

    @Mock
    private SyncLogRepository syncLogRepository;

    @Mock
    private SyncCursorRepository syncCursorRepository;

    @Mock
    private ConflictLogRepository conflictLogRepository;

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private WordBookRepository wordBookRepository;

    @Mock
    private WordRepository wordRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<BaseEntity> typedQuery;

    @InjectMocks
    private SyncService syncService;

    private Long userId = 1L;
    private SyncCursor syncCursor;

    @BeforeEach
    void setUp() {
        syncCursor = new SyncCursor();
        syncCursor.setId(1L);
        syncCursor.setUserId(userId);
        syncCursor.setCursor(0L);
        syncCursor.setUpdatedAt(LocalDateTime.now());

        // Use field injection for ObjectMapper since it's a concrete class
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        syncService = new SyncService(
                syncLogRepository, syncCursorRepository, conflictLogRepository,
                noteRepository, wordBookRepository, wordRepository, documentRepository,
                entityManager, objectMapper
        );
    }

    @Test
    @DisplayName("push - should return empty results for empty changes list")
    void push_emptyChanges() {
        SyncPushResponse result = syncService.push(userId, Collections.emptyList());

        assertNotNull(result);
        assertNotNull(result.getResults());
        assertTrue(result.getResults().isEmpty());

        verify(syncLogRepository, never()).save(any(SyncLog.class));
    }

    @Test
    @DisplayName("push - should return empty results for null changes")
    void push_nullChanges() {
        SyncPushResponse result = syncService.push(userId, null);

        assertNotNull(result);
        assertNotNull(result.getResults());
        assertTrue(result.getResults().isEmpty());

        verify(syncLogRepository, never()).save(any(SyncLog.class));
    }

    @Test
    @DisplayName("push - should successfully process a CREATE note action")
    void push_createNote_success() {
        when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(syncCursor));
        when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(syncLogRepository.save(any(SyncLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(entityManager.createQuery(anyString(), eq(BaseEntity.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
        when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());

        SyncPushRequest request = SyncPushRequest.builder()
                .entityType("NOTE")
                .action("CREATE")
                .entityId(100L)
                .clientId("client-uuid-1")
                .build();

        SyncPushResponse result = syncService.push(userId, List.of(request));

        assertNotNull(result);
        assertEquals(1, result.getResults().size());
        SyncPushResponse.SyncResultEntry entry = result.getResults().get(0);
        assertEquals("ACCEPTED", entry.getStatus());
        assertEquals("NOTE", entry.getEntityType());
        assertEquals("client-uuid-1", entry.getClientId());
        assertEquals(100L, entry.getEntityId());

        verify(syncLogRepository).save(any(SyncLog.class));
        verify(syncCursorRepository).save(any(SyncCursor.class));
    }

    @Test
    @DisplayName("push - should accept UPDATE when version matches")
    void push_updateNote_versionMatch() {
        when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(syncCursor));
        when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Note note = new Note();
        note.setId(50L);
        note.setUserId(userId);
        note.setTitle("Note Title");
        note.setType(NoteType.NORMAL);
        note.setVersion(3);
        note.setDeleted(false);
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());

        when(noteRepository.findById(50L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(syncLogRepository.save(any(SyncLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SyncPushRequest request = SyncPushRequest.builder()
                .entityType("NOTE")
                .action("UPDATE")
                .entityId(50L)
                .clientId("client-uuid-2")
                .version(3)
                .data("{\"title\":\"Updated Title\"}")
                .build();

        SyncPushResponse result = syncService.push(userId, List.of(request));

        assertNotNull(result);
        assertEquals(1, result.getResults().size());
        SyncPushResponse.SyncResultEntry entry = result.getResults().get(0);
        assertEquals("ACCEPTED", entry.getStatus());
        assertEquals(4, entry.getServerVersion(), "Server version should be incremented");

        verify(noteRepository).save(argThat(saved ->
                saved.getVersion() == 4
        ));
        verify(syncLogRepository).save(any(SyncLog.class));
    }

    @Test
    @DisplayName("push - should create conflict log when version does not match")
    void push_updateNote_versionConflict() {
        when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(syncCursor));
        when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conflictLogRepository.save(any(ConflictLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Note note = new Note();
        note.setId(50L);
        note.setUserId(userId);
        note.setTitle("Server Title");
        note.setType(NoteType.NORMAL);
        note.setVersion(5);
        note.setDeleted(false);
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());

        when(noteRepository.findById(50L)).thenReturn(Optional.of(note));

        // Client sends version 3, server has version 5 -> conflict
        SyncPushRequest request = SyncPushRequest.builder()
                .entityType("NOTE")
                .action("UPDATE")
                .entityId(50L)
                .clientId("client-uuid-3")
                .version(3)
                .data("{\"title\":\"Client Title\"}")
                .build();

        SyncPushResponse result = syncService.push(userId, List.of(request));

        assertNotNull(result);
        assertEquals(1, result.getResults().size());
        SyncPushResponse.SyncResultEntry entry = result.getResults().get(0);
        assertEquals("CONFLICT", entry.getStatus());
        assertEquals(5, entry.getServerVersion(), "Server version should remain the same");

        verify(conflictLogRepository).save(argThat(conflict ->
                conflict.getLocalVersion() == 3 &&
                conflict.getServerVersion() == 5 &&
                "NOTE".equals(conflict.getEntityType()) &&
                conflict.getEntityId().equals(50L)
        ));
        verify(noteRepository, never()).save(any(Note.class));
    }

    @Test
    @DisplayName("push - should soft delete note on DELETE action")
    void push_deleteNote_success() {
        when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(syncCursor));
        when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(syncLogRepository.save(any(SyncLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Note note = new Note();
        note.setId(50L);
        note.setUserId(userId);
        note.setTitle("To Delete");
        note.setType(NoteType.NORMAL);
        note.setVersion(1);
        note.setDeleted(false);
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());

        when(noteRepository.findById(50L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SyncPushRequest request = SyncPushRequest.builder()
                .entityType("NOTE")
                .action("DELETE")
                .entityId(50L)
                .clientId("client-uuid-4")
                .build();

        SyncPushResponse result = syncService.push(userId, List.of(request));

        assertNotNull(result);
        assertEquals(1, result.getResults().size());
        SyncPushResponse.SyncResultEntry entry = result.getResults().get(0);
        assertEquals("ACCEPTED", entry.getStatus());

        verify(noteRepository).save(argThat(saved ->
                Boolean.TRUE.equals(saved.getDeleted())
        ));
        verify(syncLogRepository).save(any(SyncLog.class));
    }

    @Test
    @DisplayName("push - should return NOT_FOUND for UPDATE with non-existent entity")
    void push_updateNote_notFound() {
        when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(syncCursor));
        when(syncCursorRepository.save(any(SyncCursor.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(noteRepository.findById(999L)).thenReturn(Optional.empty());

        SyncPushRequest request = SyncPushRequest.builder()
                .entityType("NOTE")
                .action("UPDATE")
                .entityId(999L)
                .clientId("client-uuid-5")
                .version(1)
                .build();

        SyncPushResponse result = syncService.push(userId, List.of(request));

        assertNotNull(result);
        assertEquals(1, result.getResults().size());
        assertEquals("NOT_FOUND", result.getResults().get(0).getStatus());

        verify(noteRepository, never()).save(any(Note.class));
        verify(syncLogRepository, never()).save(any(SyncLog.class));
    }

    @Test
    @DisplayName("pull - should return empty changes when no logs exist")
    void pull_noChanges() {
        when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(syncCursor));
        when(syncLogRepository.findByUserIdAndSyncCursorGreaterThan(eq(userId), eq(0L), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        SyncPullResponse result = syncService.pull(userId, 0L, 50);

        assertNotNull(result);
        assertEquals(0L, result.getCursor());
        assertFalse(result.getHasMore());
        assertNotNull(result.getChanges());
        assertTrue(result.getChanges().isEmpty());
    }

    @Test
    @DisplayName("pull - should return change entries when logs exist")
    void pull_withChanges() {
        SyncLog log1 = new SyncLog();
        log1.setId(1L);
        log1.setUserId(userId);
        log1.setEntityType("NOTE");
        log1.setEntityId(10L);
        log1.setAction(SyncAction.CREATE);
        log1.setSyncCursor(1L);
        log1.setClientId("client-uuid-a");
        log1.setCreatedAt(LocalDateTime.now());

        SyncLog log2 = new SyncLog();
        log2.setId(2L);
        log2.setUserId(userId);
        log2.setEntityType("WORD_BOOK");
        log2.setEntityId(20L);
        log2.setAction(SyncAction.UPDATE);
        log2.setSyncCursor(2L);
        log2.setClientId("client-uuid-b");
        log2.setCreatedAt(LocalDateTime.now());

        Note note = new Note();
        note.setId(10L);
        note.setUserId(userId);
        note.setTitle("Synced Note");
        note.setType(NoteType.NORMAL);
        note.setVersion(1);
        note.setDeleted(false);
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());

        when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(syncCursor));
        when(syncLogRepository.findByUserIdAndSyncCursorGreaterThan(eq(userId), eq(0L), any(Pageable.class)))
                .thenReturn(List.of(log1, log2));
        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        when(wordBookRepository.findById(20L)).thenReturn(Optional.empty());

        SyncPullResponse result = syncService.pull(userId, 0L, 50);

        assertNotNull(result);
        assertEquals(2L, result.getCursor(), "Cursor should be updated to last log's syncCursor");
        assertFalse(result.getHasMore());
        assertNotNull(result.getChanges());
        assertEquals(2, result.getChanges().size());

        // First change: CREATE NOTE
        SyncPullResponse.SyncChangeEntry change1 = result.getChanges().get(0);
        assertEquals("NOTE", change1.getEntityType());
        assertEquals(10L, change1.getEntityId());
        assertEquals("CREATE", change1.getAction());
        assertNotNull(change1.getData(), "Data should be populated for non-DELETE actions when entity exists");

        // Second change: UPDATE WORD_BOOK (entity not found, so data = null)
        SyncPullResponse.SyncChangeEntry change2 = result.getChanges().get(1);
        assertEquals("WORD_BOOK", change2.getEntityType());
        assertEquals(20L, change2.getEntityId());
        assertEquals("UPDATE", change2.getAction());
    }

    @Test
    @DisplayName("pull - should return hasMore=true when more pages exist")
    void pull_hasMore() {
        when(syncCursorRepository.findByUserId(userId)).thenReturn(Optional.of(syncCursor));

        // Return 3 logs when page size is 2 -> hasMore = true
        List<SyncLog> logs = new java.util.ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            SyncLog log = new SyncLog();
            log.setId((long) i);
            log.setUserId(userId);
            log.setEntityType("NOTE");
            log.setEntityId((long) i);
            log.setAction(SyncAction.CREATE);
            log.setSyncCursor((long) i);
            log.setCreatedAt(LocalDateTime.now());
            logs.add(log);
        }

        when(syncLogRepository.findByUserIdAndSyncCursorGreaterThan(eq(userId), eq(0L), any(Pageable.class)))
                .thenReturn(logs);
        when(noteRepository.findById(anyLong())).thenReturn(Optional.empty());

        SyncPullResponse result = syncService.pull(userId, 0L, 2);

        assertTrue(result.getHasMore(), "Should indicate more pages available");
        assertEquals(2, result.getChanges().size(), "Should return only pageSize entries");
    }

    @Test
    @DisplayName("getConflicts - should return empty when no conflicts exist")
    void getConflicts_noConflicts() {
        when(conflictLogRepository.findByUserIdAndResolvedFalse(userId))
                .thenReturn(Collections.emptyList());

        SyncConflictResponse result = syncService.getConflicts(userId);

        assertNotNull(result);
        assertNotNull(result.getConflicts());
        assertTrue(result.getConflicts().isEmpty());

        verify(conflictLogRepository).findByUserIdAndResolvedFalse(userId);
    }

    @Test
    @DisplayName("getConflicts - should return conflict entries when conflicts exist")
    void getConflicts_withConflicts() {
        ConflictLog conflict = new ConflictLog();
        conflict.setId(1L);
        conflict.setUserId(userId);
        conflict.setEntityType("NOTE");
        conflict.setEntityId(50L);
        conflict.setClientId("client-uuid-conflict");
        conflict.setLocalVersion(3);
        conflict.setServerVersion(5);
        conflict.setLocalData("{\"title\":\"Client Title\"}");
        conflict.setServerData("{\"title\":\"Server Title\"}");
        conflict.setResolved(false);
        conflict.setCreatedAt(LocalDateTime.now());
        conflict.setUpdatedAt(LocalDateTime.now());

        when(conflictLogRepository.findByUserIdAndResolvedFalse(userId))
                .thenReturn(List.of(conflict));

        SyncConflictResponse result = syncService.getConflicts(userId);

        assertNotNull(result);
        assertEquals(1, result.getConflicts().size());

        SyncConflictResponse.ConflictEntry entry = result.getConflicts().get(0);
        assertEquals("NOTE", entry.getEntityType());
        assertEquals(50L, entry.getEntityId());
        assertEquals("client-uuid-conflict", entry.getClientId());
        assertEquals(3, entry.getLocalVersion());
        assertEquals(5, entry.getServerVersion());
        assertEquals("{\"title\":\"Client Title\"}", entry.getLocalData());
        assertEquals("{\"title\":\"Server Title\"}", entry.getServerData());
    }
}
