package com.smartnotes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteRequest {

    private String title;
    private String content;
    @Builder.Default
    private String type = "NORMAL";
    private String checklistItems;
    private String reminderTime;
    private String reminderRepeatRule;
    private String reminderRingtone;
    private Boolean isEncrypted;
}
