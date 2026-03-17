package com.smartnotes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "notes")
public class Note extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 200)
    private String title = "";

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private NoteType type = NoteType.NORMAL;

    @Column(name = "checklist_items", columnDefinition = "json")
    private String checklistItems;

    @Column(name = "reminder_time")
    private LocalDateTime reminderTime;

    @Column(name = "reminder_repeat_rule", length = 100)
    private String reminderRepeatRule;

    @Column(name = "reminder_ringtone", length = 255)
    private String reminderRingtone;

    @Column(name = "is_completed", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isCompleted = false;

    @Column(name = "is_pinned", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isPinned = false;

    @Column(name = "is_encrypted", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isEncrypted = false;
}
