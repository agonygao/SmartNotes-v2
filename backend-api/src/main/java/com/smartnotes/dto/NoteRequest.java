package com.smartnotes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    @Size(max = 10000, message = "内容长度不能超过10000个字符")
    private String content;

    @NotNull(message = "笔记类型不能为空")
    @Builder.Default
    private String type = "NORMAL";
    private String checklistItems;
    private String reminderTime;
    private String reminderRepeatRule;
    private String reminderRingtone;
    private Boolean isEncrypted;
}
