package com.smartnotes.repository;

import com.smartnotes.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Page<Document> findByUserIdAndDeletedFalse(Long userId, Pageable pageable);

    Optional<Document> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

    long countByUserIdAndDeletedFalse(Long userId);

    Optional<Document> findByClientIdAndUserIdAndDeletedFalse(String clientId, Long userId);
}
