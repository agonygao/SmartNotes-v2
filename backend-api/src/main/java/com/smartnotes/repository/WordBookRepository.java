package com.smartnotes.repository;

import com.smartnotes.entity.WordBook;
import com.smartnotes.entity.WordBookType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WordBookRepository extends JpaRepository<WordBook, Long> {

    List<WordBook> findByUserIdAndDeletedFalse(Long userId);

    List<WordBook> findByUserIdAndDeletedFalseAndType(Long userId, WordBookType type);

    Optional<WordBook> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

    Optional<WordBook> findByTypeAndIsDefaultTrue(WordBookType type);

    boolean existsByTypeAndIsDefaultTrue(WordBookType type);
}
