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
public class SyncPullResponse {

    private Long cursor;
    private Boolean hasMore;
    private List<SyncChangeEntry> changes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncChangeEntry {

        private String entityType;
        private Long entityId;
        private String action;
        private String data;
        private Integer version;
        private Long serverTimestamp;
    }
}
