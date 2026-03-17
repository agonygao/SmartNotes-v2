package com.smartnotes.repository;

import com.smartnotes.entity.SyncLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {

    List<SyncLog> findByUserIdAndSyncCursorGreaterThanOrderBySyncCursorAsc(Long userId, Long cursor);

    List<SyncLog> findByUserIdAndSyncCursorGreaterThan(Long userId, Long cursor, Pageable pageable);

    void deleteByUserIdAndSyncCursorLessThan(Long userId, Long cursor);
}
