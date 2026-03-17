package com.smartnotes.repository;

import com.smartnotes.entity.DictationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DictationRecordRepository extends JpaRepository<DictationRecord, Long> {

    List<DictationRecord> findByUserIdAndWordIdOrderByCreatedAtDesc(Long userId, Long wordId);

    long countByUserIdAndIsCorrectTrue(Long userId);

    long countByUserIdAndIsCorrectFalse(Long userId);
}
