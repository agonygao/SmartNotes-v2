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
public class DocumentUploadResponse {

    private Long id;
    private String filename;
    private String originalFilename;
    private String fileType;
    private Long fileSize;
    private Boolean previewAvailable;
    private LocalDateTime createdAt;
}
