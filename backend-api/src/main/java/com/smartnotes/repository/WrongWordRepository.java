package com.smartnotes.repository;

import com.smartnotes.entity.WrongWord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WrongWordRepository extends JpaRepository<WrongWord, Long> {

    Optional<WrongWord> findByUserIdAndWordId(Long userId, Long wordId);

    List<WrongWord> findByUserIdAndMasteredFalseOrderByLastWrongAtDesc(Long userId);

    List<WrongWord> findByUserIdAndMasteredTrue(Long userId);
}
