package com.smartnotes.repository;

import com.smartnotes.entity.Note;
import com.smartnotes.entity.NoteType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    Page<Note> findByUserIdAndDeletedFalse(Long userId, Pageable pageable);

    Page<Note> findByUserIdAndDeletedFalseAndType(Long userId, NoteType type, Pageable pageable);

    Optional<Note> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

    List<Note> findByUserIdAndDeletedFalseAndIsPinnedTrueOrderByUpdatedAtDesc(Long userId);

    long countByUserIdAndDeletedFalse(Long userId);

    Optional<Note> findByClientIdAndUserIdAndDeletedFalse(String clientId, Long userId);

    @Query("SELECT n FROM Note n WHERE n.userId = :userId AND n.deleted = false AND n.isCompleted = false AND n.reminderTime IS NOT NULL AND n.reminderTime <= :now ORDER BY n.reminderTime ASC")
    List<Note> findDueReminders(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    List<Note> findByUserIdAndDeletedFalseAndTitleContainingIgnoreCase(Long userId, String keyword);
}
