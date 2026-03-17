package com.smartnotes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartnotes.dto.SyncConflictResponse;
import com.smartnotes.dto.SyncPullResponse;
import com.smartnotes.dto.SyncPushRequest;
import com.smartnotes.dto.SyncPushResponse;
import com.smartnotes.entity.*;
import com.smartnotes.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SyncService {

    private final SyncLogRepository syncLogRepository;
    private final SyncCursorRepository syncCursorRepository;
    private final ConflictLogRepository conflictLogRepository;
    private final NoteRepository noteRepository;
    private final WordBookRepository wordBookRepository;
    private final WordRepository wordRepository;
    private final DocumentRepository documentRepository;
    private final EntityManager entityManager;

    private final ObjectMapper objectMapper;

    private static final Set<String> VALID_ENTITY_TYPES = Set.of("NOTE", "WORD_BOOK", "WORD", "DOCUMENT");
    private static final int MAX_PAGE_SIZE = 500;

    // ==================== Push ====================

    public SyncPushResponse push(Long userId, List<SyncPushRequest> changes) {
        if (changes == null || changes.isEmpty()) {
            return SyncPushResponse.builder().results(Collections.emptyList()).build();
        }

        SyncCursor syncCursor = getOrCreateCursor(userId);
        long currentMaxCursor = syncCursor.getCursor();

        List<SyncPushResponse.SyncResultEntry> results = new ArrayList<>();

        for (SyncPushRequest change : changes) {
            try {
                SyncPushResponse.SyncResultEntry result = processPushChange(userId, change, currentMaxCursor);
                results.add(result);
                // If accepted, advance the cursor counter
                if ("ACCEPTED".equals(result.getStatus())) {
                    currentMaxCursor = result.getServerVersion() != null ? result.getServerVersion() : currentMaxCursor;
                }
            } catch (Exception e) {
                log.error("Error processing sync push change: entityType={}, entityId={}, error={}",
                        change.getEntityType(), change.getEntityId(), e.getMessage(), e);
                results.add(SyncPushResponse.SyncResultEntry.builder()
                        .entityType(change.getEntityType())
                        .clientId(change.getClientId())
                        .entityId(change.getEntityId())
                        .status("ERROR")
                        .build());
            }
        }

        // Update cursor
        syncCursor.setCursor(currentMaxCursor);
        syncCursor.setLastSyncedAt(LocalDateTime.now());
        syncCursorRepository.save(syncCursor);

        return SyncPushResponse.builder().results(results).build();
    }

    private SyncPushResponse.SyncResultEntry processPushChange(Long userId, SyncPushRequest change, long currentMaxCursor) {
        // Validate entity type
        String entityType = change.getEntityType();
        if (entityType == null || !VALID_ENTITY_TYPES.contains(entityType.toUpperCase())) {
            return SyncPushResponse.SyncResultEntry.builder()
                    .entityType(change.getEntityType())
                    .clientId(change.getClientId())
                    .entityId(change.getEntityId())
                    .status("INVALID_ENTITY_TYPE")
                    .build();
        }
        entityType = entityType.toUpperCase();

        // Validate action
        SyncAction action;
        try {
            action = SyncAction.valueOf(change.getAction().toUpperCase());
        } catch (Exception e) {
            return SyncPushResponse.SyncResultEntry.builder()
                    .entityType(entityType)
                    .clientId(change.getClientId())
                    .entityId(change.getEntityId())
                    .status("INVALID_ACTION")
                    .build();
        }

        // Generate new sync cursor
        long newCursor = currentMaxCursor + 1;

        switch (action) {
            case CREATE:
                return processCreate(userId, change, entityType, newCursor);
            case UPDATE:
                return processUpdate(userId, change, entityType, newCursor);
            case DELETE:
                return processDelete(userId, change, entityType, newCursor);
            default:
                return SyncPushResponse.SyncResultEntry.builder()
                        .entityType(entityType)
                        .clientId(change.getClientId())
                        .entityId(change.getEntityId())
                        .status("INVALID_ACTION")
                        .build();
        }
    }

    private SyncPushResponse.SyncResultEntry processCreate(Long userId, SyncPushRequest change,
                                                          String entityType, long newCursor) {
        // Check if entity already exists by clientId
        Optional<? extends BaseEntity> existing = findEntityByClientId(entityType, change.getClientId());

        if (existing.isPresent()) {
            // Entity already exists - conflict
            return SyncPushResponse.SyncResultEntry.builder()
                    .entityType(entityType)
                    .clientId(change.getClientId())
                    .entityId(existing.get().getId())
                    .serverVersion(existing.get().getVersion())
                    .status("CONFLICT")
                    .build();
        }

        // Placeholder: the actual entity creation is done via normal CRUD.
        // We log the sync entry with entityId 0 (not yet assigned) or the provided entityId.
        // Use the provided entityId from the client as a provisional ID; the real entity
        // will be created via normal CRUD endpoints and associated later.
        SyncLog syncLog = buildSyncLog(userId, entityType, change.getEntityId() != null ? change.getEntityId() : 0L,
                SyncAction.CREATE, newCursor, change.getClientId());
        syncLogRepository.save(syncLog);

        return SyncPushResponse.SyncResultEntry.builder()
                .entityType(entityType)
                .clientId(change.getClientId())
                .entityId(change.getEntityId())
                .serverVersion((int) newCursor)
                .status("ACCEPTED")
                .build();
    }

    private SyncPushResponse.SyncResultEntry processUpdate(Long userId, SyncPushRequest change,
                                                          String entityType, long newCursor) {
        if (change.getEntityId() == null) {
            return SyncPushResponse.SyncResultEntry.builder()
                    .entityType(entityType)
                    .clientId(change.getClientId())
                    .entityId(null)
                    .status("INVALID_ENTITY_ID")
                    .build();
        }

        Optional<? extends BaseEntity> entityOpt = findEntityById(entityType, change.getEntityId());

        if (entityOpt.isEmpty()) {
            return SyncPushResponse.SyncResultEntry.builder()
                    .entityType(entityType)
                    .clientId(change.getClientId())
                    .entityId(change.getEntityId())
                    .status("NOT_FOUND")
                    .build();
        }

        BaseEntity entity = entityOpt.get();
        Integer clientVersion = change.getVersion() != null ? change.getVersion() : 0;
        Integer serverVersion = entity.getVersion() != null ? entity.getVersion() : 1;

        // Version mismatch -> conflict
        if (!clientVersion.equals(serverVersion)) {
            // Create conflict log
            ConflictLog conflictLog = new ConflictLog();
            conflictLog.setUserId(userId);
            conflictLog.setEntityType(entityType);
            conflictLog.setEntityId(entity.getId());
            conflictLog.setClientId(change.getClientId());
            conflictLog.setLocalVersion(clientVersion);
            conflictLog.setServerVersion(serverVersion);
            conflictLog.setLocalData(change.getData());
            conflictLog.setServerData(serializeEntityToJson(entity));
            conflictLog.setResolved(false);
            conflictLogRepository.save(conflictLog);

            return SyncPushResponse.SyncResultEntry.builder()
                    .entityType(entityType)
                    .clientId(change.getClientId())
                    .entityId(entity.getId())
                    .serverVersion(serverVersion)
                    .status("CONFLICT")
                    .build();
        }

        // Version matches - update entity version
        entity.setVersion(serverVersion + 1);
        saveEntity(entityType, entity);

        // Log the sync entry
        SyncLog syncLog = buildSyncLog(userId, entityType, entity.getId(),
                SyncAction.UPDATE, newCursor, change.getClientId());
        syncLogRepository.save(syncLog);

        return SyncPushResponse.SyncResultEntry.builder()
                .entityType(entityType)
                .clientId(change.getClientId())
                .entityId(entity.getId())
                .serverVersion(serverVersion + 1)
                .status("ACCEPTED")
                .build();
    }

    private SyncPushResponse.SyncResultEntry processDelete(Long userId, SyncPushRequest change,
                                                          String entityType, long newCursor) {
        if (change.getEntityId() == null) {
            return SyncPushResponse.SyncResultEntry.builder()
                    .entityType(entityType)
                    .clientId(change.getClientId())
                    .entityId(null)
                    .status("INVALID_ENTITY_ID")
                    .build();
        }

        Optional<? extends BaseEntity> entityOpt = findEntityById(entityType, change.getEntityId());

        if (entityOpt.isEmpty()) {
            // Entity not found - already deleted or never existed, just log it
            SyncLog syncLog = buildSyncLog(userId, entityType, change.getEntityId(),
                    SyncAction.DELETE, newCursor, change.getClientId());
            syncLogRepository.save(syncLog);

            return SyncPushResponse.SyncResultEntry.builder()
                    .entityType(entityType)
                    .clientId(change.getClientId())
                    .entityId(change.getEntityId())
                    .serverVersion((int) newCursor)
                    .status("ACCEPTED")
                    .build();
        }

        BaseEntity entity = entityOpt.get();
        // Soft delete
        entity.setDeleted(true);
        saveEntity(entityType, entity);

        // Log the sync entry
        SyncLog syncLog = buildSyncLog(userId, entityType, entity.getId(),
                SyncAction.DELETE, newCursor, change.getClientId());
        syncLogRepository.save(syncLog);

        return SyncPushResponse.SyncResultEntry.builder()
                .entityType(entityType)
                .clientId(change.getClientId())
                .entityId(entity.getId())
                .serverVersion((int) newCursor)
                .status("ACCEPTED")
                .build();
    }

    // ==================== Pull ====================

    @Transactional(readOnly = true)
    public SyncPullResponse pull(Long userId, Long sinceCursor, int pageSize) {
        SyncCursor syncCursor = getOrCreateCursor(userId);

        if (pageSize < 1) {
            pageSize = 100;
        }
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }

        // Fetch pageSize + 1 to determine hasMore
        List<SyncLog> allLogs = syncLogRepository.findByUserIdAndSyncCursorGreaterThan(
                userId, sinceCursor, PageRequest.of(0, pageSize + 1));

        boolean hasMore = allLogs.size() > pageSize;
        List<SyncLog> logs = hasMore ? allLogs.subList(0, pageSize) : allLogs;

        List<SyncPullResponse.SyncChangeEntry> changes = logs.stream()
                .map(log -> buildChangeEntry(log))
                .collect(Collectors.toList());

        // Determine the new cursor value (last processed cursor)
        long newCursor = sinceCursor;
        if (!logs.isEmpty()) {
            newCursor = logs.get(logs.size() - 1).getSyncCursor();
        }

        return SyncPullResponse.builder()
                .cursor(newCursor)
                .hasMore(hasMore)
                .changes(changes)
                .build();
    }

    private SyncPullResponse.SyncChangeEntry buildChangeEntry(SyncLog log) {
        String data = null;
        Integer version = null;

        // For non-DELETE actions, fetch the actual entity data
        if (log.getAction() != SyncAction.DELETE) {
            Optional<? extends BaseEntity> entityOpt = findEntityById(log.getEntityType(), log.getEntityId());
            if (entityOpt.isPresent()) {
                BaseEntity entity = entityOpt.get();
                if (!Boolean.TRUE.equals(entity.getDeleted())) {
                    data = serializeEntityToJson(entity);
                    version = entity.getVersion();
                }
            }
        }

        // serverTimestamp: epoch millis of createdAt
        long serverTimestamp = log.getCreatedAt() != null
                ? log.getCreatedAt().toEpochSecond(ZoneOffset.UTC) * 1000
                : 0L;

        return SyncPullResponse.SyncChangeEntry.builder()
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .action(log.getAction().name())
                .data(data)
                .version(version)
                .serverTimestamp(serverTimestamp)
                .build();
    }

    // ==================== Status ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getSyncStatus(Long userId) {
        SyncCursor syncCursor = getOrCreateCursor(userId);

        // Count pending changes (logs with cursor > user's cursor)
        long pendingChanges = syncLogRepository.findByUserIdAndSyncCursorGreaterThanOrderBySyncCursorAsc(
                userId, syncCursor.getCursor()).size();

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("cursor", syncCursor.getCursor());
        status.put("lastSyncedAt", syncCursor.getLastSyncedAt());
        status.put("pendingChanges", pendingChanges);

        return status;
    }

    // ==================== Conflicts ====================

    @Transactional(readOnly = true)
    public SyncConflictResponse getConflicts(Long userId) {
        List<ConflictLog> conflicts = conflictLogRepository.findByUserIdAndResolvedFalse(userId);

        List<SyncConflictResponse.ConflictEntry> entries = conflicts.stream()
                .map(this::toConflictEntry)
                .collect(Collectors.toList());

        return SyncConflictResponse.builder()
                .conflicts(entries)
                .build();
    }

    private SyncConflictResponse.ConflictEntry toConflictEntry(ConflictLog conflict) {
        return SyncConflictResponse.ConflictEntry.builder()
                .entityType(conflict.getEntityType())
                .entityId(conflict.getEntityId())
                .clientId(conflict.getClientId())
                .localVersion(conflict.getLocalVersion())
                .serverVersion(conflict.getServerVersion())
                .localData(conflict.getLocalData())
                .serverData(conflict.getServerData())
                .createdAt(conflict.getCreatedAt())
                .build();
    }

    // ==================== Helper Methods ====================

    private SyncCursor getOrCreateCursor(Long userId) {
        return syncCursorRepository.findByUserId(userId)
                .orElseGet(() -> {
                    SyncCursor newCursor = new SyncCursor();
                    newCursor.setUserId(userId);
                    newCursor.setCursor(0L);
                    newCursor.setUpdatedAt(LocalDateTime.now());
                    return syncCursorRepository.save(newCursor);
                });
    }

    private SyncLog buildSyncLog(Long userId, String entityType, Long entityId,
                                 SyncAction action, Long syncCursor, String clientId) {
        SyncLog syncLog = new SyncLog();
        syncLog.setUserId(userId);
        syncLog.setEntityType(entityType);
        syncLog.setEntityId(entityId);
        syncLog.setAction(action);
        syncLog.setSyncCursor(syncCursor);
        syncLog.setClientId(clientId);
        return syncLog;
    }

    @SuppressWarnings("unchecked")
    private Optional<? extends BaseEntity> findEntityById(String entityType, Long entityId) {
        return switch (entityType) {
            case "NOTE" -> noteRepository.findById(entityId).map(e -> (BaseEntity) e);
            case "WORD_BOOK" -> wordBookRepository.findById(entityId).map(e -> (BaseEntity) e);
            case "WORD" -> wordRepository.findById(entityId).map(e -> (BaseEntity) e);
            case "DOCUMENT" -> documentRepository.findById(entityId).map(e -> (BaseEntity) e);
            default -> Optional.empty();
        };
    }

    private Optional<? extends BaseEntity> findEntityByClientId(String entityType, String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return Optional.empty();
        }
        // Use EntityManager JPQL to look up entity by clientId across all entity types.
        // All entities extend BaseEntity which has the clientId field.
        String entityClassName = switch (entityType) {
            case "NOTE" -> Note.class.getName();
            case "WORD_BOOK" -> WordBook.class.getName();
            case "WORD" -> Word.class.getName();
            case "DOCUMENT" -> Document.class.getName();
            default -> null;
        };
        if (entityClassName == null) {
            return Optional.empty();
        }
        try {
            String jpql = "SELECT e FROM " + entityClassName + " e WHERE e.clientId = :clientId AND e.deleted = false";
            TypedQuery<? extends BaseEntity> query = entityManager.createQuery(jpql, BaseEntity.class);
            query.setParameter("clientId", clientId);
            query.setMaxResults(1);
            List<? extends BaseEntity> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            log.warn("Failed to look up entity by clientId: entityType={}, clientId={}, error={}",
                    entityType, clientId, e.getMessage());
            return Optional.empty();
        }
    }

    private void saveEntity(String entityType, BaseEntity entity) {
        switch (entityType) {
            case "NOTE" -> noteRepository.save((Note) entity);
            case "WORD_BOOK" -> wordBookRepository.save((WordBook) entity);
            case "WORD" -> wordRepository.save((Word) entity);
            case "DOCUMENT" -> documentRepository.save((Document) entity);
        }
    }

    private String serializeEntityToJson(BaseEntity entity) {
        try {
            return objectMapper.writeValueAsString(entity);
        } catch (Exception e) {
            log.warn("Failed to serialize entity to JSON: entityType={}, entityId={}, error={}",
                    entity.getClass().getSimpleName(), entity.getId(), e.getMessage());
            return null;
        }
    }
}
