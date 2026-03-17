package com.smartnotes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncPushResponse {

    private List<SyncResultEntry> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncResultEntry {

        private String entityType;
        private String clientId;
        private Long entityId;
        private Integer serverVersion;
        private String status;
    }
}
