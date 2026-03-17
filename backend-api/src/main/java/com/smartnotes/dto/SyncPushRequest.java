package com.smartnotes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncPushRequest {

    private String clientId;
    private String entityType;
    private Long entityId;
    private String action;
    private String data;
    private Integer version;
}
