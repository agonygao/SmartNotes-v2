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

import static org.junit.jupiter.api.Assertions.*;
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

    @Test
    @DisplayName("createNote - should create a note with NORMAL type")
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

        assertNotNull(result);
        assertEquals("New Note", result.getTitle());
        assertEquals("New Content", result.getContent());
        assertEquals("NORMAL", result.getType());
        assertFalse(result.getIsEncrypted());

        verify(noteRepository).save(argThat(note ->
                note.getUserId().equals(userId) &&
                note.getType() == NoteType.NORMAL
        ));
    }

    @Test
    @DisplayName("createNote - should enforce encryption for SECRET type notes")
    void createNote_secretType_enforcesEncryption() {
        NoteRequest request = NoteRequest.builder()
                .title("Secret Note")
                .content("Secret Content")
                .type("SECRET")
                .isEncrypted(false) // explicitly set to false, but service should override
                .build();

        when(encryptionUtil.encrypt("Secret Content")).thenReturn("encrypted-content");
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> {
            Note note = invocation.getArgument(0);
            note.setId(2L);
            note.setCreatedAt(LocalDateTime.now());
            note.setUpdatedAt(LocalDateTime.now());
            note.setVersion(1);
            return note;
        });

        NoteResponse result = noteService.createNote(userId, request);

        assertNotNull(result);
        assertEquals("SECRET", result.getType());
        assertTrue(result.getIsEncrypted(), "SECRET type notes must have isEncrypted = true");

        verify(noteRepository).save(argThat(note ->
                note.getType() == NoteType.SECRET &&
                Boolean.TRUE.equals(note.getIsEncrypted())
        ));
        verify(encryptionUtil).encrypt("Secret Content");
    }

    @Test
    @DisplayName("updateNote - should update an existing note successfully")
    void updateNote_success() {
        NoteRequest request = NoteRequest.builder()
                .title("Updated Title")
                .content("Updated Content")
                .build();

        when(noteRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                .thenReturn(Optional.of(existingNote));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NoteResponse result = noteService.updateNote(userId, 10L, request);

        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated Content", result.getContent());
        assertEquals(2, result.getVersion(), "Version should be incremented on update");

        verify(noteRepository).findByIdAndUserIdAndDeletedFalse(10L, userId);
        verify(noteRepository).save(any(Note.class));
    }

    @Test
    @DisplayName("updateNote - should throw exception when note not found")
    void updateNote_notFound() {
        NoteRequest request = NoteRequest.builder()
                .title("Updated Title")
                .build();

        when(noteRepository.findByIdAndUserIdAndDeletedFalse(999L, userId))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> noteService.updateNote(userId, 999L, request));

        assertEquals(ErrorCode.NOTE_NOT_FOUND, exception.getCode());
        verify(noteRepository, never()).save(any(Note.class));
    }

    @Test
    @DisplayName("deleteNote - should soft delete a note")
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
    @DisplayName("getNote - should return note response for valid ID")
    void getNote_success() {
        when(noteRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                .thenReturn(Optional.of(existingNote));

        NoteResponse result = noteService.getNote(userId, 10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(userId, result.getUserId());
        assertEquals("Test Note", result.getTitle());
        assertEquals("Test Content", result.getContent());
        assertEquals("NORMAL", result.getType());

        verify(noteRepository).findByIdAndUserIdAndDeletedFalse(10L, userId);
    }

    @Test
    @DisplayName("getNote - should throw exception when note not found")
    void getNote_notFound() {
        when(noteRepository.findByIdAndUserIdAndDeletedFalse(999L, userId))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> noteService.getNote(userId, 999L));

        assertEquals(ErrorCode.NOTE_NOT_FOUND, exception.getCode());
    }

    @Test
    @DisplayName("listNotes - should list notes without type filter")
    void listNotes_noFilter() {
        List<Note> notes = List.of(existingNote);
        Page<Note> notePage = new PageImpl<>(notes);

        when(noteRepository.findByUserIdAndDeletedFalse(eq(userId), any(Pageable.class)))
                .thenReturn(notePage);

        PageResponse<NoteResponse> result = noteService.listNotes(userId, 0, 10, null);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getTotalElements());
        assertEquals(1, result.getTotalPages());

        verify(noteRepository).findByUserIdAndDeletedFalse(eq(userId), any(Pageable.class));
        verify(noteRepository, never()).findByUserIdAndDeletedFalseAndType(anyLong(), any(NoteType.class), any(Pageable.class));
    }

    @Test
    @DisplayName("listNotes - should list notes with type filter")
    void listNotes_withType() {
        existingNote.setType(NoteType.REMINDER);
        List<Note> notes = List.of(existingNote);
        Page<Note> notePage = new PageImpl<>(notes);

        when(noteRepository.findByUserIdAndDeletedFalseAndType(eq(userId), eq(NoteType.REMINDER), any(Pageable.class)))
                .thenReturn(notePage);

        PageResponse<NoteResponse> result = noteService.listNotes(userId, 0, 10, "REMINDER");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("REMINDER", result.getContent().get(0).getType());

        verify(noteRepository).findByUserIdAndDeletedFalseAndType(eq(userId), eq(NoteType.REMINDER), any(Pageable.class));
        verify(noteRepository, never()).findByUserIdAndDeletedFalse(anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("togglePin - should toggle isPinned from false to true")
    void togglePin_success() {
        existingNote.setIsPinned(false);

        when(noteRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                .thenReturn(Optional.of(existingNote));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NoteResponse result = noteService.togglePin(userId, 10L);

        assertNotNull(result);
        assertTrue(result.getIsPinned(), "isPinned should be toggled to true");

        verify(noteRepository).save(argThat(note ->
                Boolean.TRUE.equals(note.getIsPinned()) &&
                note.getVersion() == 2
        ));
    }

    @Test
    @DisplayName("toggleComplete - should toggle isCompleted from false to true")
    void toggleComplete_success() {
        existingNote.setIsCompleted(false);

        when(noteRepository.findByIdAndUserIdAndDeletedFalse(10L, userId))
                .thenReturn(Optional.of(existingNote));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NoteResponse result = noteService.toggleComplete(userId, 10L);

        assertNotNull(result);
        assertTrue(result.getIsCompleted(), "isCompleted should be toggled to true");

        verify(noteRepository).save(argThat(note ->
                Boolean.TRUE.equals(note.getIsCompleted()) &&
                note.getVersion() == 2
        ));
    }
}
