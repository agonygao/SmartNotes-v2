package com.smartnotes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordResponse {

    private Long id;
    private Long bookId;
    private String word;
    private String phonetic;
    private String meaning;
    private String exampleSentence;
    private Integer sortOrder;
    private String clientId;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
