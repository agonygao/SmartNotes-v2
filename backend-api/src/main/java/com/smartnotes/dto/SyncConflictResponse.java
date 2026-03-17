package com.smartnotes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncConflictResponse {

    private List<ConflictEntry> conflicts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConflictEntry {

        private String entityType;
        private Long entityId;
        private String clientId;
        private Integer localVersion;
        private Integer serverVersion;
        private String localData;
        private String serverData;
        private LocalDateTime createdAt;
    }
}
