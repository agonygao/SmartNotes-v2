package com.smartnotes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "words")
public class Word extends BaseEntity {

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "word", nullable = false, length = 100)
    private String word;

    @Column(name = "phonetic", length = 255)
    private String phonetic;

    @Column(name = "meaning", columnDefinition = "TEXT")
    private String meaning;

    @Column(name = "example_sentence", columnDefinition = "TEXT")
    private String exampleSentence;

    @Column(name = "sort_order", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer sortOrder = 0;
}
