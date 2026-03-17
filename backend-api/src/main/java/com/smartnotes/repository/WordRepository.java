package com.smartnotes.repository;

import com.smartnotes.entity.Word;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WordRepository extends JpaRepository<Word, Long> {

    List<Word> findByBookIdAndDeletedFalseOrderBySortOrder(Long bookId);

    Page<Word> findByBookIdAndDeletedFalse(Long bookId, Pageable pageable);

    Optional<Word> findByIdAndBookIdAndDeletedFalse(Long id, Long bookId);

    boolean existsByBookIdAndWordAndDeletedFalse(Long bookId, String word);

    long countByBookIdAndDeletedFalse(Long bookId);

    List<Word> findByBookIdAndDeletedFalseAndWordContainingIgnoreCase(Long bookId, String keyword);
}
