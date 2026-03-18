package com.smartnotes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartnotes.entity.User;
import com.smartnotes.repository.NoteRepository;
import com.smartnotes.repository.UserRepository;
import com.smartnotes.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.smartnotes.SmartNotesApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("NoteController Integration Tests")
class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private String authToken;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        noteRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setUsername("testuser");
        user.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("password123"));
        user.setEmail("test@example.com");
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setTokenVersion(0);
        user = userRepository.save(user);
        testUserId = user.getId();

        authToken = jwtUtil.generateAccessToken(user, user.getTokenVersion());
    }

    private Long createNoteViaApi(String title, String content, String type) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("content", content);
        body.put("type", type);

        String response = mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("data").get("id").asLong();
    }

    // ==================== Create Note Tests ====================

    @Nested
    @DisplayName("POST /api/notes")
    class CreateNoteTests {

        @Test
        @DisplayName("should create a NORMAL note")
        void createNote_normal() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("title", "My Note");
            body.put("content", "Hello World");
            body.put("type", "NORMAL");

            mockMvc.perform(post("/api/notes")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.title").value("My Note"))
                    .andExpect(jsonPath("$.data.content").value("Hello World"))
                    .andExpect(jsonPath("$.data.type").value("NORMAL"))
                    .andExpect(jsonPath("$.data.isPinned").value(false))
                    .andExpect(jsonPath("$.data.isCompleted").value(false))
                    .andExpect(jsonPath("$.data.isEncrypted").value(false));
        }

        @Test
        @DisplayName("should create a CHECKLIST note")
        void createNote_checklist() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("title", "Shopping List");
            body.put("type", "CHECKLIST");
            body.put("checklistItems", "[{\"text\":\"Milk\",\"checked\":false}]");

            mockMvc.perform(post("/api/notes")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.type").value("CHECKLIST"));
        }

        @Test
        @DisplayName("should create a REMINDER note with reminder time")
        void createNote_reminder() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("title", "Meeting");
            body.put("type", "REMINDER");
            body.put("reminderTime", "2026-06-01T09:00:00");
            body.put("reminderRepeatRule", "DAILY");

            mockMvc.perform(post("/api/notes")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.type").value("REMINDER"))
                    .andExpect(jsonPath("$.data.reminderRepeatRule").value("DAILY"));
        }

        @Test
        @DisplayName("should create a SECRET note with encrypted content")
        void createNote_secret() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("title", "Secret Note");
            body.put("content", "Secret data");
            body.put("type", "SECRET");

            mockMvc.perform(post("/api/notes")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.type").value("SECRET"))
                    .andExpect(jsonPath("$.data.isEncrypted").value(true))
                    // Content should be decrypted in response
                    .andExpect(jsonPath("$.data.content").value("Secret data"));
        }

        @Test
        @DisplayName("should require authentication")
        void createNote_unauthenticated() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("title", "No Auth Note");

            mockMvc.perform(post("/api/notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== List Notes Tests ====================

    @Nested
    @DisplayName("GET /api/notes")
    class ListNotesTests {

        @Test
        @DisplayName("should list all notes for user")
        void listNotes_all() throws Exception {
            createNoteViaApi("Note 1", "Content 1", "NORMAL");
            createNoteViaApi("Note 2", "Content 2", "NORMAL");

            mockMvc.perform(get("/api/notes")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(2));
        }

        @Test
        @DisplayName("should filter notes by type")
        void listNotes_byType() throws Exception {
            createNoteViaApi("Normal Note", "Content", "NORMAL");
            createNoteViaApi("Reminder Note", "Content", "REMINDER");
            createNoteViaApi("Another Reminder", "Content", "REMINDER");

            mockMvc.perform(get("/api/notes")
                            .header("Authorization", "Bearer " + authToken)
                            .param("type", "REMINDER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(2));
        }

        @Test
        @DisplayName("should support pagination")
        void listNotes_pagination() throws Exception {
            for (int i = 0; i < 5; i++) {
                createNoteViaApi("Note " + i, "Content " + i, "NORMAL");
            }

            mockMvc.perform(get("/api/notes")
                            .header("Authorization", "Bearer " + authToken)
                            .param("page", "0")
                            .param("size", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(3))
                    .andExpect(jsonPath("$.data.totalElements").value(5))
                    .andExpect(jsonPath("$.data.totalPages").value(2))
                    .andExpect(jsonPath("$.data.first").value(true));
        }

        @Test
        @DisplayName("should return empty list for new user")
        void listNotes_empty() throws Exception {
            mockMvc.perform(get("/api/notes")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isEmpty());
        }
    }

    // ==================== Get Note Tests ====================

    @Nested
    @DisplayName("GET /api/notes/{id}")
    class GetNoteTests {

        @Test
        @DisplayName("should get a note by ID")
        void getNote_success() throws Exception {
            Long noteId = createNoteViaApi("Test Note", "Content", "NORMAL");

            mockMvc.perform(get("/api/notes/" + noteId)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(noteId))
                    .andExpect(jsonPath("$.data.title").value("Test Note"));
        }

        @Test
        @DisplayName("should return error for non-existent note")
        void getNote_notFound() throws Exception {
            mockMvc.perform(get("/api/notes/99999")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(2001));
        }

        @Test
        @DisplayName("should decrypt SECRET note content")
        void getNote_secretDecrypted() throws Exception {
            Long noteId = createNoteViaApi("Secret", "Top Secret", "SECRET");

            mockMvc.perform(get("/api/notes/" + noteId)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").value("Top Secret"))
                    .andExpect(jsonPath("$.data.isEncrypted").value(true));
        }
    }

    // ==================== Update Note Tests ====================

    @Nested
    @DisplayName("PUT /api/notes/{id}")
    class UpdateNoteTests {

        @Test
        @DisplayName("should update an existing note")
        void updateNote_success() throws Exception {
            Long noteId = createNoteViaApi("Original", "Original content", "NORMAL");

            Map<String, Object> body = new HashMap<>();
            body.put("title", "Updated Title");
            body.put("content", "Updated content");

            mockMvc.perform(put("/api/notes/" + noteId)
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("Updated Title"))
                    .andExpect(jsonPath("$.data.content").value("Updated content"))
                    .andExpect(jsonPath("$.data.version").value(2));
        }

        @Test
        @DisplayName("should return error when updating non-existent note")
        void updateNote_notFound() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("title", "Ghost");

            mockMvc.perform(put("/api/notes/99999")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(2001));
        }

        @Test
        @DisplayName("should update note type to SECRET and encrypt content")
        void updateNote_changeToSecret() throws Exception {
            Long noteId = createNoteViaApi("Normal Note", "Sensitive data", "NORMAL");

            Map<String, Object> body = new HashMap<>();
            body.put("type", "SECRET");
            body.put("content", "Sensitive data");

            mockMvc.perform(put("/api/notes/" + noteId)
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.type").value("SECRET"))
                    .andExpect(jsonPath("$.data.isEncrypted").value(true))
                    .andExpect(jsonPath("$.data.content").value("Sensitive data"));
        }
    }

    // ==================== Delete Note Tests ====================

    @Nested
    @DisplayName("DELETE /api/notes/{id}")
    class DeleteNoteTests {

        @Test
        @DisplayName("should soft delete a note")
        void deleteNote_success() throws Exception {
            Long noteId = createNoteViaApi("To Delete", "Content", "NORMAL");

            mockMvc.perform(delete("/api/notes/" + noteId)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // Note should no longer appear in list
            mockMvc.perform(get("/api/notes")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isEmpty());
        }

        @Test
        @DisplayName("should return error when deleting non-existent note")
        void deleteNote_notFound() throws Exception {
            mockMvc.perform(delete("/api/notes/99999")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ==================== Toggle Pin Tests ====================

    @Nested
    @DisplayName("PATCH /api/notes/{id}/pin")
    class TogglePinTests {

        @Test
        @DisplayName("should toggle pin from false to true")
        void togglePin_on() throws Exception {
            Long noteId = createNoteViaApi("Pinnable", "Content", "NORMAL");

            mockMvc.perform(patch("/api/notes/" + noteId + "/pin")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isPinned").value(true));

            // Toggle back
            mockMvc.perform(patch("/api/notes/" + noteId + "/pin")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isPinned").value(false));
        }

        @Test
        @DisplayName("should return error for non-existent note")
        void togglePin_notFound() throws Exception {
            mockMvc.perform(patch("/api/notes/99999/pin")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ==================== Multi-User Isolation Tests ====================

    @Nested
    @DisplayName("Multi-user isolation")
    class MultiUserIsolationTests {

        @Test
        @DisplayName("should not allow user to access another user's note")
        void isolation_cannotAccessOtherUserNote() throws Exception {
            Long noteId = createNoteViaApi("Private Note", "Private content", "NORMAL");

            // Create a second user
            com.smartnotes.entity.User user2 = new com.smartnotes.entity.User();
            user2.setUsername("otheruser");
            user2.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("password123"));
            user2.setEmail("other@example.com");
            user2.setRole("USER");
            user2.setStatus("ACTIVE");
            user2.setTokenVersion(0);
            user2 = userRepository.save(user2);

            String user2Token = jwtUtil.generateAccessToken(user2, user2.getTokenVersion());

            // User2 should not be able to read User1's note
            mockMvc.perform(get("/api/notes/" + noteId)
                            .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(2001));

            // User2 should not be able to update User1's note
            Map<String, Object> body = new HashMap<>();
            body.put("title", "Hacked");
            mockMvc.perform(put("/api/notes/" + noteId)
                            .header("Authorization", "Bearer " + user2Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(2001));

            // User2 should not be able to delete User1's note
            mockMvc.perform(delete("/api/notes/" + noteId)
                            .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isInternalServerError());

            // User1's note should still be accessible
            mockMvc.perform(get("/api/notes/" + noteId)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("Private Note"));
        }

        @Test
        @DisplayName("should not list other user's notes")
        void isolation_listNotesDoesNotShowOtherUsers() throws Exception {
            createNoteViaApi("User1 Note", "Content", "NORMAL");

            // Create a second user
            com.smartnotes.entity.User user2 = new com.smartnotes.entity.User();
            user2.setUsername("user2isolation");
            user2.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("password123"));
            user2.setRole("USER");
            user2.setStatus("ACTIVE");
            user2.setTokenVersion(0);
            user2 = userRepository.save(user2);

            String user2Token = jwtUtil.generateAccessToken(user2, user2.getTokenVersion());

            mockMvc.perform(get("/api/notes")
                            .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isEmpty());
        }
    }

    // ==================== Toggle Complete Tests ====================

    @Nested
    @DisplayName("PATCH /api/notes/{id}/complete")
    class ToggleCompleteTests {

        @Test
        @DisplayName("should toggle complete from false to true")
        void toggleComplete_on() throws Exception {
            Long noteId = createNoteViaApi("Task", "Content", "CHECKLIST");

            mockMvc.perform(patch("/api/notes/" + noteId + "/complete")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isCompleted").value(true));

            // Toggle back
            mockMvc.perform(patch("/api/notes/" + noteId + "/complete")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isCompleted").value(false));
        }

        @Test
        @DisplayName("should return error for non-existent note")
        void toggleComplete_notFound() throws Exception {
            mockMvc.perform(patch("/api/notes/99999/complete")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isInternalServerError());
        }
    }
}
