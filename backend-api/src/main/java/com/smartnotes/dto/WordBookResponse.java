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
public class WordBookResponse {

    private Long id;
    private Long userId;
    private String name;
    private String description;
    private String type;
    private Integer wordCount;
    private Boolean isDefault;
    private String clientId;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
