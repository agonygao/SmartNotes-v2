package com.smartnotes.repository;

import com.smartnotes.entity.ConflictLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConflictLogRepository extends JpaRepository<ConflictLog, Long> {

    List<ConflictLog> findByUserIdAndResolvedFalse(Long userId);

    List<ConflictLog> findByUserId(Long userId);
}
