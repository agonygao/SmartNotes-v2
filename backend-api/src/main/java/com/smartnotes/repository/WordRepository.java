package com.smartnotes.repository;

import com.smartnotes.entity.Word;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WordRepository extends JpaRepository<Word, Long> {

    List<Word> findByBookIdAndDeletedFalseOrderBySortOrder(Long bookId);

    Page<Word> findByBookIdAndDeletedFalse(Long bookId, Pageable pageable);

    Optional<Word> findByIdAndBookIdAndDeletedFalse(Long id, Long bookId);

    boolean existsByBookIdAndWordAndDeletedFalse(Long bookId, String word);

    long countByBookIdAndDeletedFalse(Long bookId);

    List<Word> findByBookIdAndDeletedFalseAndWordContainingIgnoreCase(Long bookId, String keyword);

    /**
     * Find a word by ID, ensuring the owning word book belongs to the given user.
     */
    @Query("SELECT w FROM Word w JOIN WordBook wb ON w.bookId = wb.id WHERE w.id = :wordId AND wb.userId = :userId AND w.deleted = false")
    Optional<Word> findByIdAndUserIdAndDeletedFalse(@Param("wordId") Long wordId, @Param("userId") Long userId);

    /**
     * Find a word by clientId, ensuring the owning word book belongs to the given user.
     */
    @Query("SELECT w FROM Word w JOIN WordBook wb ON w.bookId = wb.id WHERE w.clientId = :clientId AND wb.userId = :userId AND w.deleted = false")
    Optional<Word> findByClientIdAndUserIdAndDeletedFalse(@Param("clientId") String clientId, @Param("userId") Long userId);
}
