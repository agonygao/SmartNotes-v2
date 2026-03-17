package com.smartnotes.repository;

import com.smartnotes.entity.SyncCursor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SyncCursorRepository extends JpaRepository<SyncCursor, Long> {

    Optional<SyncCursor> findByUserId(Long userId);
}
