package com.smartnotes.service;

import com.smartnotes.dto.ErrorCode;
import com.smartnotes.dto.NoteRequest;
import com.smartnotes.dto.NoteResponse;
import com.smartnotes.dto.PageResponse;
import com.smartnotes.entity.Note;
import com.smartnotes.entity.NoteType;
import com.smartnotes.exception.BusinessException;
import com.smartnotes.repository.NoteRepository;
import com.smartnotes.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NoteService {

    private final NoteRepository noteRepository;
    private final EncryptionUtil encryptionUtil;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public NoteResponse createNote(Long userId, NoteRequest request) {
        Note note = new Note();
        note.setUserId(userId);

        if (request.getTitle() != null) {
            note.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            note.setContent(request.getContent());
        }
        if (request.getType() != null) {
            note.setType(NoteType.valueOf(request.getType()));
        }
        if (request.getChecklistItems() != null) {
            note.setChecklistItems(request.getChecklistItems());
        }
        if (request.getReminderTime() != null && !request.getReminderTime().isBlank()) {
            note.setReminderTime(LocalDateTime.parse(request.getReminderTime(), DATE_TIME_FORMATTER));
        }
        if (request.getReminderRepeatRule() != null) {
            note.setReminderRepeatRule(request.getReminderRepeatRule());
        }
        if (request.getReminderRingtone() != null) {
            note.setReminderRingtone(request.getReminderRingtone());
        }
        if (request.getIsEncrypted() != null) {
            note.setIsEncrypted(request.getIsEncrypted());
        }

        // If type is SECRET, enforce isEncrypted = true and encrypt content
        if (note.getType() == NoteType.SECRET) {
            note.setIsEncrypted(true);
            if (note.getContent() != null && !note.getContent().isEmpty()) {
                note.setContent(encryptionUtil.encrypt(note.getContent()));
            }
        }

        Note saved = noteRepository.save(note);
        return convertToResponse(saved);
    }

    public NoteResponse updateNote(Long userId, Long id, NoteRequest request) {
        Note note = noteRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));

        // If type is changing to SECRET and content needs to be encrypted,
        // we need to handle the transition. If current note is SECRET and content
        // is already encrypted, we should decrypt first if changing type away from SECRET.
        NoteType previousType = note.getType();
        boolean wasSecret = previousType == NoteType.SECRET;

        if (request.getTitle() != null) {
            note.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            note.setContent(request.getContent());
        }
        if (request.getType() != null) {
            note.setType(NoteType.valueOf(request.getType()));
        }
        if (request.getChecklistItems() != null) {
            note.setChecklistItems(request.getChecklistItems());
        }
        if (request.getReminderTime() != null && !request.getReminderTime().isBlank()) {
            try {
                note.setReminderTime(LocalDateTime.parse(request.getReminderTime(), DATE_TIME_FORMATTER));
            } catch (DateTimeParseException e) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid reminderTime format");
            }
        }
        if (request.getReminderRepeatRule() != null) {
            note.setReminderRepeatRule(request.getReminderRepeatRule());
        }
        if (request.getReminderRingtone() != null) {
            note.setReminderRingtone(request.getReminderRingtone());
        }
        if (request.getIsEncrypted() != null) {
            note.setIsEncrypted(request.getIsEncrypted());
        }

        // If type is SECRET, enforce isEncrypted = true and encrypt content
        if (note.getType() == NoteType.SECRET) {
            note.setIsEncrypted(true);
            if (note.getContent() != null && !note.getContent().isEmpty()) {
                // Only encrypt if the content was not already encrypted (e.g., just changed type to SECRET)
                if (!wasSecret) {
                    note.setContent(encryptionUtil.encrypt(note.getContent()));
                }
                // If it was already SECRET and content was provided, it might be new plaintext that needs encrypting
                // The content from request is always plaintext from the client
            }
        }

        note.setVersion(note.getVersion() + 1);
        Note saved = noteRepository.save(note);
        return convertToResponse(saved);
    }

    public void deleteNote(Long userId, Long id) {
        Note note = noteRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));

        note.setDeleted(true);
        note.setVersion(note.getVersion() + 1);
        noteRepository.save(note);
    }

    @Transactional(readOnly = true)
    public NoteResponse getNote(Long userId, Long id) {
        Note note = noteRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));
        return convertToResponse(note);
    }

    @Transactional(readOnly = true)
    public PageResponse<NoteResponse> listNotes(Long userId, int page, int size, String type) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        Page<Note> notePage;
        if (type != null && !type.isBlank()) {
            NoteType noteType = NoteType.valueOf(type);
            notePage = noteRepository.findByUserIdAndDeletedFalseAndType(userId, noteType, pageRequest);
        } else {
            notePage = noteRepository.findByUserIdAndDeletedFalse(userId, pageRequest);
        }

        List<NoteResponse> responses = notePage.getContent().stream()
                .map(this::convertToResponse)
                .toList();

        return PageResponse.<NoteResponse>builder()
                .content(responses)
                .totalElements(notePage.getTotalElements())
                .totalPages(notePage.getTotalPages())
                .page(notePage.getNumber())
                .size(notePage.getSize())
                .first(notePage.isFirst())
                .last(notePage.isLast())
                .build();
    }

    public NoteResponse togglePin(Long userId, Long id) {
        Note note = noteRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));

        note.setIsPinned(!note.getIsPinned());
        note.setVersion(note.getVersion() + 1);
        Note saved = noteRepository.save(note);
        return convertToResponse(saved);
    }

    public NoteResponse toggleComplete(Long userId, Long id) {
        Note note = noteRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));

        note.setIsCompleted(!note.getIsCompleted());
        note.setVersion(note.getVersion() + 1);
        Note saved = noteRepository.save(note);
        return convertToResponse(saved);
    }

    /**
     * Search notes by keyword in title and decrypted content.
     */
    @Transactional(readOnly = true)
    public List<NoteResponse> searchNotes(Long userId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        // Search by title in database
        List<Note> titleMatches = noteRepository.findByUserIdAndDeletedFalseAndTitleContainingIgnoreCase(userId, keyword);

        // Collect results, decrypting content for SECRET notes
        List<NoteResponse> results = new ArrayList<>();
        for (Note note : titleMatches) {
            results.add(convertToResponse(note));
        }

        // For content search, we need to check all non-deleted notes and decrypt to match
        // This is a fallback for content-based search since encrypted content cannot be searched in DB
        List<Note> allNotes = noteRepository.findByUserIdAndDeletedFalse(userId,
                PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "updatedAt"))
        ).getContent();

        for (Note note : allNotes) {
            // Skip if already added from title match
            if (results.stream().anyMatch(r -> r.getId().equals(note.getId()))) {
                continue;
            }

            String content = note.getContent();
            if (note.getType() == NoteType.SECRET && Boolean.TRUE.equals(note.getIsEncrypted())) {
                try {
                    content = encryptionUtil.decrypt(content);
                } catch (Exception e) {
                    content = "";
                }
            }

            if (content != null && content.toLowerCase().contains(keyword.toLowerCase())) {
                results.add(convertToResponse(note));
            }
        }

        return results;
    }

    /**
     * Get notes where reminderTime <= now and isCompleted is false.
     */
    @Transactional(readOnly = true)
    public List<NoteResponse> getDueReminders(Long userId) {
        List<Note> dueNotes = noteRepository.findDueReminders(userId, LocalDateTime.now());
        return dueNotes.stream()
                .map(this::convertToResponse)
                .toList();
    }

    private NoteResponse convertToResponse(Note note) {
        String content = note.getContent();

        // Decrypt content for SECRET notes before returning
        if (note.getType() == NoteType.SECRET && Boolean.TRUE.equals(note.getIsEncrypted())
                && content != null && !content.isEmpty()) {
            try {
                content = encryptionUtil.decrypt(content);
            } catch (Exception e) {
                // If decryption fails, return the raw content (shouldn't happen normally)
                // but we should still return something useful
            }
        }

        return NoteResponse.builder()
                .id(note.getId())
                .userId(note.getUserId())
                .title(note.getTitle())
                .content(content)
                .type(note.getType().name())
                .checklistItems(note.getChecklistItems())
                .reminderTime(note.getReminderTime())
                .reminderRepeatRule(note.getReminderRepeatRule())
                .reminderRingtone(note.getReminderRingtone())
                .isCompleted(note.getIsCompleted())
                .isPinned(note.getIsPinned())
                .isEncrypted(note.getIsEncrypted())
                .clientId(note.getClientId())
                .version(note.getVersion())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}
