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
public class NoteResponse {

    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String type;
    private String checklistItems;
    private LocalDateTime reminderTime;
    private String reminderRepeatRule;
    private String reminderRingtone;
    private Boolean isCompleted;
    private Boolean isPinned;
    private Boolean isEncrypted;
    private String clientId;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
