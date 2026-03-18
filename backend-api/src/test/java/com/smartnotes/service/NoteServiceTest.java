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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoteService Unit Tests")
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private EncryptionUtil encryptionUtil;

    @InjectMocks
    private NoteService noteService;

    private Long userId = 1L;
    private Note existingNote;

    @BeforeEach
    void setUp() {
        existingNote = new Note();
        existingNote.setId(10L);
        existingNote.setUserId(userId);
        existingNote.setTitle("Test Note");
        existingNote.setContent("Test Content");
        existingNote.setType(NoteType.NORMAL);
        existingNote.setIsCompleted(false);
        existingNote.setIsPinned(false);
        existingNote.setIsEncrypted(false);
        existingNote.setVersion(1);
        existingNote.setDeleted(false);
        existingNote.setCreatedAt(LocalDateTime.now());
        existingNote.setUpdatedAt(LocalDateTime.now());
    }

    // ==================== createNote Tests ====================

    @Nested
    @DisplayName("createNote()")
    class CreateNoteTests {

        @Test
        @DisplayName("should create a NORMAL type note")
        void createNote_normalType() {
            NoteRequest request = NoteRequest.builder()
                    .title("New Note")
                    .content("New Content")
                    .type("NORMAL")
                    .build();

            when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> {
                Note note = invocation.getArgument(0);
                note.setId(1L);
                note.setCreatedAt(LocalDateTime.now());
                note.setUpdatedAt(LocalDateTime.now());
                note.setVersion(1);
                return note;
            });

            NoteResponse result = noteService.createNote(userId, request);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("New Note");
            assertThat(result.getContent()).isEqualTo("New Content");
            assertThat(result.getType()).isEqualTo("NORMAL");
            assertThat(result.getIsEncrypted()).isFalse();

            verify(noteRepository).save(argThat(note ->
                    note.getUserId().equals(userId) &&
                    note.getType() == NoteType.NORMAL
            ));
            verify(encryptionUtil, never()).encrypt(anyString());
        }

        @Test
        @DisplayName("should create a CHECKLIST type note")
        void createNote_checklistType() {
            NoteRequest request = NoteRequest.builder()
                    .title("Shopping List")
                    .content(null)
                    .type("CHECKLIST")
                    .checklistItems("[{\"text\":\"Milk\",\"checked\":false}]")
                    .build();

            when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> {
                Note note = invocation.getArgument(0);
                note.setId(2L);
                note.setCreatedAt(LocalDateTime.now());
                note.setUpdatedAt(LocalDateTime.now());
                note.setVersion(1);
                return note;
            });

            NoteResponse result = noteService.createNote(userId, request);

            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo("CHECKLIST");
            assertThat(result.getChecklistItems()).contains("Milk");

            verify(noteRepository).save(argThat(note ->
                    note.getType() == NoteType.CHECKLIST &&
                    note.getChecklistItems() != null
            ));
        }

        @Test
        @DisplayName("should create a REMINDER type note with reminder time")
        void createNote_reminderType() {
            String reminderTime = "2026-04-01T09:00:00";
            NoteRequest request = NoteRequest.builder()
                    .title("Meeting Reminder")
                    .content("Team standup")
                    .type("REMINDER")
                    .reminderTime(reminderTime)
                    .reminderRepeatRule("DAILY")
                    .build();

            when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> {
                Note note = invocation.getArgument(0);
                note.setId(3L);
                note.setCreatedAt(LocalDateTime.now());
                note.setUpdatedAt(LocalDateTime.now());
                note.setVersion(1);
                return note;
            });

            NoteResponse result = noteService.createNote(userId, request);

            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo("REMINDER");
            assertThat(result.getReminderTime()).isEqualTo(LocalDateTime.of(2026, 4, 1, 9, 0, 0));
            assertThat(result.getReminderRepeatRule()).isEqualTo("DAILY");

            verify(noteRepository).save(argThat(note ->
                    note.getReminderTime() != null &&
                    "DAILY".equals(note.getReminderRepeatRule())
            ));
        }

        @Test
        @DisplayName("should enforce encryption for SECRET type notes")
        void createNote_secretType() {
            NoteRequest request = NoteRequest.builder()
                    .title("Secret Note")
                    .content("Secret Content")
                    .type("SECRET")
                    .isEncrypted(false)
                    .build();

            when(encryptionUtil.encrypt("Secret Content")).thenReturn("encrypted-content");
            when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> {
                Note note = invocation.getArgument(0);
                note.setId(4L);
                note.setCreatedAt(LocalDateTime.now());
                note.setUpdatedAt(LocalDateTime.now());
                note.setVersion(1);
                return note;
            });

            NoteResponse result = noteService.createNote(userId, request);

            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo("SECRET");
            assertThat(result.getIsEncrypted()).isTrue();

            verify(noteRepository).save(argThat(note ->
                    note.getType() == NoteType.SECRET &&
                    Boolean.TRUE.equals(note.getIsEncrypted()) &&
                    "encrypted-content".equals(note.getContent())
            ));
            verify(encryptionUtil).encrypt("Secret Content");
        }

        @Test
        @DisplayName("should create SECRET note without encrypting null content")
        void createNote_secretType_nullContent() {
            NoteRequest request = NoteRequest.builder()
                    .title("Empty Secret")
                    .content(null)
                    .type("SECRET")
                    .build();

            when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> {
                Note note = invocation.getArgument(0);
                note.setId(5L);
                note.setCreatedAt(LocalDateTime.now());
                note.setUpdatedAt(LocalDateTime.now());
                note.setVersion(1);
                return note;
            });

            NoteResponse result = noteService.createNote(userId, request);

            assertThat(result).isNotNull();
            assertThat(result.getIsEncrypted()).isTrue();

            verify(encryptionUtil, never()).encrypt(anyString());
        }
    }

    // ==================== updateNote Tests ====================

    @Nested
    @DisplayName("updateNote()")
    class UpdateNoteTests {

        @Test
        @DisplayName("should update an existing note")
        void updateNote_success() {
            NoteRequest request = NoteRequest.builder()
                    .title("Updated Title")
                    .content("Updated Content")
                    .build();

            when(noteRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                    .thenReturn(Optional.of(existingNote));
            when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

            NoteResponse result = noteService.updateNote(userId, 10L, request);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Updated Title");
            assertThat(result.getContent()).isEqualTo("Updated Content");
            assertThat(result.getVersion()).isEqualTo(2);

            verify(noteRepository).findByIdAndUserIdAndDeletedFalse(10L, userId);
            verify(noteRepository).save(any(Note.class));
        }

        @Test
        @DisplayName("should enforce ownership - throw when note belongs to another user")
        void updateNote_ownershipCheck() {
            Long otherUserId = 2L;
            NoteRequest request = NoteRequest.builder()
                    .title("Hacked Title")
                    .build();

            when(noteRepository.findByIdAndUserIdAndDeletedFalse(10L, otherUserId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> noteService.updateNote(otherUserId, 10L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.NOTE_NOT_FOUND);

            verify(noteRepository, never()).save(any(Note.class));
        }

        @Test
        @DisplayName("should throw NOTE_NOT_FOUND for non-existent note")
        void updateNote_notFound() {
            NoteRequest request = NoteRequest.builder()
                    .title("Updated Title")
                    .build();

            when(noteRepository.findByIdAndUserIdAndDeletedFalse(999L, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> noteService.updateNote(userId, 999L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.NOTE_NOT_FOUND);

            verify(noteRepository, never()).save(any(Note.class));
        }

        @Test
        @DisplayName("should encrypt content when changing type to SECRET")
        void updateNote_changeToSecret() {
            existingNote.setType(NoteType.NORMAL);
            existingNote.setIsEncrypted(false);

            NoteRequest request = NoteRequest.builder()
                    .type("SECRET")
                    .content("Now secret content")
                    .build();

            when(noteRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                    .thenReturn(Optional.of(existingNote));
            when(encryptionUtil.encrypt("Now secret content")).thenReturn("encrypted-now-secret");
            when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

            NoteResponse result = noteService.updateNote(userId, 10L, request);

            assertThat(result.getType()).isEqualTo("SECRET");
            assertThat(result.getIsEncrypted()).isTrue();

            verify(encryptionUtil).encrypt("Now secret content");
            verify(noteRepository).save(argThat(note ->
                    Boolean.TRUE.equals(note.getIsEncrypted())
            ));
        }

        @Test
        @DisplayName("should throw BAD_REQUEST for invalid reminderTime format")
        void updateNote_invalidReminderTime() {
            NoteRequest request = NoteRequest.builder()
                    .reminderTime("not-a-valid-date")
                    .build();

            when(noteRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                    .thenReturn(Optional.of(existingNote));

            assertThatThrownBy(() -> noteService.updateNote(userId, 10L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.BAD_REQUEST);

            verify(noteRepository, never()).save(any(Note.class));
        }
    }

    // ==================== deleteNote Tests ====================

    @Nested
    @DisplayName("deleteNote()")
    class DeleteNoteTests {

        @Test
        @DisplayName("should soft delete a note")
        void deleteNote_success() {
            when(noteRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                    .thenReturn(Optional.of(existingNote));
            when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

            noteService.deleteNote(userId, 10L);

            verify(noteRepository).save(argThat(note ->
                    Boolean.TRUE.equals(note.getDeleted()) &&
                    note.getVersion() == 2
            ));
        }

        @Test
        @DisplayName("should throw NOTE_NOT_FOUND for non-existent note")
        void deleteNote_notFound() {
            when(noteRepository.findByIdAndUserIdAndDeletedFalse(999L, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> noteService.deleteNote(userId, 999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.NOTE_NOT_FOUND);

            verify(noteRepository, never()).save(any(Note.class));
        }

        @Test
        @DisplayName("should enforce ownership check on delete")
        void deleteNote_ownershipCheck() {
            Long otherUserId = 2L;

            when(noteRepository.findByIdAndUserIdAndDeletedFalse(10L, otherUserId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> noteService.deleteNote(otherUserId, 10L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.NOTE_NOT_FOUND);
        }
    }

    // ==================== getNote Tests ====================

    @Nested
    @DisplayName("getNote()")
    class GetNoteTests {

        @Test
        @DisplayName("should return note for valid ID")
        void getNote_success() {
            when(noteRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                    .thenReturn(Optional.of(existingNote));

            NoteResponse result = noteService.getNote(userId, 10L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getTitle()).isEqualTo("Test Note");
            assertThat(result.getContent()).isEqualTo("Test Content");
            assertThat(result.getType()).isEqualTo("NORMAL");
        }

        @Test
        @DisplayName("should decrypt content for SECRET notes")
        void getNote_secretNote_decrypted() {
            existingNote.setType(NoteType.SECRET);
            existingNote.setIsEncrypted(true);
            existingNote.setContent("encrypted-content");

            when(noteRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                    .thenReturn(Optional.of(existingNote));
            when(encryptionUtil.decrypt("encrypted-content")).thenReturn("decrypted-content");

            NoteResponse result = noteService.getNote(userId, 10L);

            assertThat(result.getContent()).isEqualTo("decrypted-content");
            verify(encryptionUtil).decrypt("encrypted-content");
        }

        @Test
        @DisplayName("should throw NOTE_NOT_FOUND for non-existent note")
        void getNote_notFound() {
            when(noteRepository.findByIdAndUserIdAndDeletedFalse(999L, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> noteService.getNote(userId, 999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.NOTE_NOT_FOUND);
        }
    }

    // ==================== listNotes Tests ====================

    @Nested
    @DisplayName("listNotes()")
    class ListNotesTests {

        @Test
        @DisplayName("should list notes without type filter")
        void listNotes_noFilter() {
            List<Note> notes = List.of(existingNote);
            Page<Note> notePage = new PageImpl<>(notes);

            when(noteRepository.findByUserIdAndDeletedFalse(eq(userId), any(Pageable.class)))
                    .thenReturn(notePage);

            PageResponse<NoteResponse> result = noteService.listNotes(userId, 0, 10, null);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1L);
            assertThat(result.getTotalPages()).isEqualTo(1);

            verify(noteRepository).findByUserIdAndDeletedFalse(eq(userId), any(Pageable.class));
            verify(noteRepository, never()).findByUserIdAndDeletedFalseAndType(anyLong(), any(NoteType.class), any(Pageable.class));
        }

        @Test
        @DisplayName("should list notes with type filter")
        void listNotes_withTypeFilter() {
            existingNote.setType(NoteType.REMINDER);
            List<Note> notes = List.of(existingNote);
            Page<Note> notePage = new PageImpl<>(notes);

            when(noteRepository.findByUserIdAndDeletedFalseAndType(eq(userId), eq(NoteType.REMINDER), any(Pageable.class)))
                    .thenReturn(notePage);

            PageResponse<NoteResponse> result = noteService.listNotes(userId, 0, 10, "REMINDER");

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getType()).isEqualTo("REMINDER");

            verify(noteRepository).findByUserIdAndDeletedFalseAndType(eq(userId), eq(NoteType.REMINDER), any(Pageable.class));
            verify(noteRepository, never()).findByUserIdAndDeletedFalse(anyLong(), any(Pageable.class));
        }

        @Test
        @DisplayName("should return empty page when no notes exist")
        void listNotes_empty() {
            Page<Note> emptyPage = new PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(0, 10), 0);

            when(noteRepository.findByUserIdAndDeletedFalse(eq(userId), any(Pageable.class)))
                    .thenReturn(emptyPage);

            PageResponse<NoteResponse> result = noteService.listNotes(userId, 0, 10, null);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0L);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();
        }

        @Test
        @DisplayName("should handle pagination parameters correctly")
        void listNotes_pagination() {
            List<Note> notes = List.of(existingNote);
            Page<Note> notePage = new PageImpl<>(notes, org.springframework.data.domain.PageRequest.of(1, 5), 15);

            when(noteRepository.findByUserIdAndDeletedFalse(eq(userId), any(Pageable.class)))
                    .thenReturn(notePage);

            PageResponse<NoteResponse> result = noteService.listNotes(userId, 1, 5, null);

            assertThat(result.getPage()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(5);
            assertThat(result.getTotalElements()).isEqualTo(15L);
            assertThat(result.getTotalPages()).isEqualTo(3);
            assertThat(result.isFirst()).isFalse();
        }
    }

    // ==================== togglePin Tests ====================

    @Nested
    @DisplayName("togglePin()")
    class TogglePinTests {

        @Test
        @DisplayName("should toggle isPinned from false to true")
        void togglePin_falseToTrue() {
            existingNote.setIsPinned(false);

            when(noteRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                    .thenReturn(Optional.of(existingNote));
            when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

            NoteResponse result = noteService.togglePin(userId, 10L);

            assertThat(result).isNotNull();
            assertThat(result.getIsPinned()).isTrue();

            verify(noteRepository).save(argThat(note ->
                    Boolean.TRUE.equals(note.getIsPinned()) &&
                    note.getVersion() == 2
            ));
        }

        @Test
        @DisplayName("should toggle isPinned from true to false")
        void togglePin_trueToFalse() {
            existingNote.setIsPinned(true);

            when(noteRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                    .thenReturn(Optional.of(existingNote));
            when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

            NoteResponse result = noteService.togglePin(userId, 10L);

            assertThat(result.getIsPinned()).isFalse();
        }

        @Test
        @DisplayName("should throw NOTE_NOT_FOUND when note not found")
        void togglePin_notFound() {
            when(noteRepository.findByIdAndUserIdAndDeletedFalse(999L, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> noteService.togglePin(userId, 999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.NOTE_NOT_FOUND);
        }
    }

    // ==================== toggleComplete Tests ====================

    @Nested
    @DisplayName("toggleComplete()")
    class ToggleCompleteTests {

        @Test
        @DisplayName("should toggle isCompleted from false to true")
        void toggleComplete_falseToTrue() {
            existingNote.setIsCompleted(false);

            when(noteRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                    .thenReturn(Optional.of(existingNote));
            when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

            NoteResponse result = noteService.toggleComplete(userId, 10L);

            assertThat(result.getIsCompleted()).isTrue();

            verify(noteRepository).save(argThat(note ->
                    Boolean.TRUE.equals(note.getIsCompleted()) &&
                    note.getVersion() == 2
            ));
        }

        @Test
        @DisplayName("should toggle isCompleted from true to false")
        void toggleComplete_trueToFalse() {
            existingNote.setIsCompleted(true);

            when(noteRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                    .thenReturn(Optional.of(existingNote));
            when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

            NoteResponse result = noteService.toggleComplete(userId, 10L);

            assertThat(result.getIsCompleted()).isFalse();
        }

        @Test
        @DisplayName("should throw NOTE_NOT_FOUND when note not found")
        void toggleComplete_notFound() {
            when(noteRepository.findByIdAndUserIdAndDeletedFalse(999L, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> noteService.toggleComplete(userId, 999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.NOTE_NOT_FOUND);
        }
    }

    // ==================== searchNotes Tests ====================

    @Nested
    @DisplayName("searchNotes()")
    class SearchNotesTests {

        @Test
        @DisplayName("should return notes matching title keyword")
        void searchNotes_titleMatch() {
            Note note1 = new Note();
            note1.setId(1L);
            note1.setUserId(userId);
            note1.setTitle("Java Programming");
            note1.setContent("Content about Java");
            note1.setType(NoteType.NORMAL);
            note1.setIsEncrypted(false);
            note1.setDeleted(false);
            note1.setCreatedAt(LocalDateTime.now());
            note1.setUpdatedAt(LocalDateTime.now());

            when(noteRepository.findByUserIdAndDeletedFalseAndTitleContainingIgnoreCase(userId, "Java"))
                    .thenReturn(List.of(note1));
            when(noteRepository.findByUserIdAndDeletedFalse(eq(userId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(note1)));

            List<NoteResponse> results = noteService.searchNotes(userId, "Java");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTitle()).isEqualTo("Java Programming");
        }

        @Test
        @DisplayName("should return notes matching content keyword")
        void searchNotes_contentMatch() {
            Note note1 = new Note();
            note1.setId(1L);
            note1.setUserId(userId);
            note1.setTitle("Random Title");
            note1.setContent("Important keyword here");
            note1.setType(NoteType.NORMAL);
            note1.setIsEncrypted(false);
            note1.setDeleted(false);
            note1.setCreatedAt(LocalDateTime.now());
            note1.setUpdatedAt(LocalDateTime.now());

            when(noteRepository.findByUserIdAndDeletedFalseAndTitleContainingIgnoreCase(userId, "keyword"))
                    .thenReturn(List.of());
            when(noteRepository.findByUserIdAndDeletedFalse(eq(userId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(note1)));

            List<NoteResponse> results = noteService.searchNotes(userId, "keyword");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should return empty list for null keyword")
        void searchNotes_nullKeyword() {
            List<NoteResponse> results = noteService.searchNotes(userId, null);

            assertThat(results).isEmpty();
            verify(noteRepository, never()).findByUserIdAndDeletedFalseAndTitleContainingIgnoreCase(anyLong(), anyString());
        }

        @Test
        @DisplayName("should return empty list for blank keyword")
        void searchNotes_blankKeyword() {
            List<NoteResponse> results = noteService.searchNotes(userId, "   ");

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should be case-insensitive for content search")
        void searchNotes_caseInsensitive() {
            Note note1 = new Note();
            note1.setId(1L);
            note1.setUserId(userId);
            note1.setTitle("Title");
            note1.setContent("IMPORTANT");
            note1.setType(NoteType.NORMAL);
            note1.setIsEncrypted(false);
            note1.setDeleted(false);
            note1.setCreatedAt(LocalDateTime.now());
            note1.setUpdatedAt(LocalDateTime.now());

            when(noteRepository.findByUserIdAndDeletedFalseAndTitleContainingIgnoreCase(userId, "important"))
                    .thenReturn(List.of());
            when(noteRepository.findByUserIdAndDeletedFalse(eq(userId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(note1)));

            List<NoteResponse> results = noteService.searchNotes(userId, "important");

            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("should search within decrypted SECRET note content")
        void searchNotes_encryptedContentMatch() {
            Note secretNote = new Note();
            secretNote.setId(2L);
            secretNote.setUserId(userId);
            secretNote.setTitle("Hidden Note");
            secretNote.setContent("encrypted-data");
            secretNote.setType(NoteType.SECRET);
            secretNote.setIsEncrypted(true);
            secretNote.setDeleted(false);
            secretNote.setCreatedAt(LocalDateTime.now());
            secretNote.setUpdatedAt(LocalDateTime.now());

            when(noteRepository.findByUserIdAndDeletedFalseAndTitleContainingIgnoreCase(userId, "needle"))
                    .thenReturn(List.of());
            when(noteRepository.findByUserIdAndDeletedFalse(eq(userId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(secretNote)));
            // decrypt is called twice: once in searchNotes content loop, once in convertToResponse
            when(encryptionUtil.decrypt("encrypted-data")).thenReturn("this contains the needle");

            List<NoteResponse> results = noteService.searchNotes(userId, "needle");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo(2L);
            verify(encryptionUtil, times(2)).decrypt("encrypted-data");
        }

        @Test
        @DisplayName("should not duplicate notes found by both title and content")
        void searchNotes_noDuplicates() {
            Note note1 = new Note();
            note1.setId(1L);
            note1.setUserId(userId);
            note1.setTitle("keyword");
            note1.setContent("contains keyword too");
            note1.setType(NoteType.NORMAL);
            note1.setIsEncrypted(false);
            note1.setDeleted(false);
            note1.setCreatedAt(LocalDateTime.now());
            note1.setUpdatedAt(LocalDateTime.now());

            when(noteRepository.findByUserIdAndDeletedFalseAndTitleContainingIgnoreCase(userId, "keyword"))
                    .thenReturn(List.of(note1));
            when(noteRepository.findByUserIdAndDeletedFalse(eq(userId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(note1)));

            List<NoteResponse> results = noteService.searchNotes(userId, "keyword");

            assertThat(results).hasSize(1);
        }
    }

    // ==================== getDueReminders Tests ====================

    @Nested
    @DisplayName("getDueReminders()")
    class GetDueRemindersTests {

        @Test
        @DisplayName("should return due reminder notes")
        void getDueReminders_hasDue() {
            Note reminder = new Note();
            reminder.setId(50L);
            reminder.setUserId(userId);
            reminder.setTitle("Meeting");
            reminder.setType(NoteType.REMINDER);
            reminder.setIsCompleted(false);
            reminder.setDeleted(false);
            reminder.setCreatedAt(LocalDateTime.now());
            reminder.setUpdatedAt(LocalDateTime.now());

            when(noteRepository.findDueReminders(eq(userId), any(LocalDateTime.class)))
                    .thenReturn(List.of(reminder));

            List<NoteResponse> results = noteService.getDueReminders(userId);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getType()).isEqualTo("REMINDER");
        }

        @Test
        @DisplayName("should return empty list when no reminders are due")
        void getDueReminders_noneDue() {
            when(noteRepository.findDueReminders(eq(userId), any(LocalDateTime.class)))
                    .thenReturn(List.of());

            List<NoteResponse> results = noteService.getDueReminders(userId);

            assertThat(results).isEmpty();
        }
    }

    // ==================== updateNote Edge Cases ====================

    @Nested
    @DisplayName("updateNote() - edge cases")
    class UpdateNoteEdgeCases {

        @Test
        @DisplayName("should not encrypt content when note stays SECRET and content not provided")
        void updateNote_secretStaysSecret_noNewContent() {
            existingNote.setType(NoteType.SECRET);
            existingNote.setIsEncrypted(true);
            existingNote.setContent("encrypted-content");

            NoteRequest request = NoteRequest.builder()
                    .title("Updated Secret Title")
                    .type("SECRET")
                    .build();

            when(noteRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                    .thenReturn(Optional.of(existingNote));
            when(encryptionUtil.decrypt("encrypted-content")).thenReturn("decrypted-content");
            when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

            NoteResponse result = noteService.updateNote(userId, 10L, request);

            assertThat(result.getTitle()).isEqualTo("Updated Secret Title");
            assertThat(result.getType()).isEqualTo("SECRET");
            // Content was not changed, but still decrypted in response
            assertThat(result.getContent()).isEqualTo("decrypted-content");
            // Should not re-encrypt since wasSecret is true
            verify(encryptionUtil, never()).encrypt(anyString());
        }
    }

    // ==================== createNote Edge Cases ====================

    @Nested
    @DisplayName("createNote() - edge cases")
    class CreateNoteEdgeCases {

        @Test
        @DisplayName("should create note with default values when all fields are null")
        void createNote_allNullFields() {
            NoteRequest request = NoteRequest.builder().build();

            when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> {
                Note note = invocation.getArgument(0);
                note.setId(1L);
                note.setCreatedAt(LocalDateTime.now());
                note.setUpdatedAt(LocalDateTime.now());
                note.setVersion(1);
                return note;
            });

            NoteResponse result = noteService.createNote(userId, request);

            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo("NORMAL");
            verify(noteRepository).save(argThat(note ->
                    note.getUserId().equals(userId) &&
                    note.getType() == NoteType.NORMAL &&
                    Boolean.FALSE.equals(note.getIsEncrypted())
            ));
        }

        @Test
        @DisplayName("should handle SECRET note with empty content")
        void createNote_secretEmptyContent() {
            NoteRequest request = NoteRequest.builder()
                    .title("Empty Secret")
                    .content("")
                    .type("SECRET")
                    .build();

            when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> {
                Note note = invocation.getArgument(0);
                note.setId(1L);
                note.setCreatedAt(LocalDateTime.now());
                note.setUpdatedAt(LocalDateTime.now());
                note.setVersion(1);
                return note;
            });

            NoteResponse result = noteService.createNote(userId, request);

            assertThat(result.getIsEncrypted()).isTrue();
            verify(encryptionUtil, never()).encrypt(anyString());
        }
    }

    // ==================== getNote Edge Cases ====================

    @Nested
    @DisplayName("getNote() - edge cases")
    class GetNoteEdgeCases {

        @Test
        @DisplayName("should handle decryption failure gracefully")
        void getNote_decryptionFailure() {
            existingNote.setType(NoteType.SECRET);
            existingNote.setIsEncrypted(true);
            existingNote.setContent("corrupted-encrypted-data");

            when(noteRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                    .thenReturn(Optional.of(existingNote));
            when(encryptionUtil.decrypt("corrupted-encrypted-data"))
                    .thenThrow(new RuntimeException("Decryption failed"));

            NoteResponse result = noteService.getNote(userId, 10L);

            // Should still return the note, but with raw content
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(10L);
        }
    }
}
