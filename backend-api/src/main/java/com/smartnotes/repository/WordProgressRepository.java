package com.smartnotes.repository;

import com.smartnotes.entity.WordProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WordProgressRepository extends JpaRepository<WordProgress, Long> {

    Optional<WordProgress> findByUserIdAndWordId(Long userId, Long wordId);

    List<WordProgress> findByUserIdAndNextReviewAtBeforeAndNextReviewAtIsNotNull(Long userId, LocalDateTime now);

    long countByUserId(Long userId);
}
