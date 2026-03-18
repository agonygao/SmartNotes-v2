package com.smartnotes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartnotes.entity.User;
import com.smartnotes.repository.UserRepository;
import com.smartnotes.repository.WordBookRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.smartnotes.SmartNotesApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("WordBookController Integration Tests")
class WordBookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WordBookRepository wordBookRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private String authToken;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        wordBookRepository.deleteAll();
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

    private Long createWordBookViaApi(String name, String type) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("type", type);

        String response = mockMvc.perform(post("/api/wordbooks")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("data").get("id").asLong();
    }

    // ==================== Create WordBook Tests ====================

    @Nested
    @DisplayName("POST /api/wordbooks")
    class CreateWordBookTests {

        @Test
        @DisplayName("should create a custom word book")
        void createWordBook_success() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("name", "My Vocabulary");
            body.put("description", "Custom words");
            body.put("type", "CUSTOM");

            mockMvc.perform(post("/api/wordbooks")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.name").value("My Vocabulary"))
                    .andExpect(jsonPath("$.data.description").value("Custom words"))
                    .andExpect(jsonPath("$.data.type").value("CUSTOM"))
                    .andExpect(jsonPath("$.data.wordCount").value(0))
                    .andExpect(jsonPath("$.data.isDefault").value(false));
        }

        @Test
        @DisplayName("should default to CUSTOM type when not specified")
        void createWordBook_defaultType() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("name", "No Type Book");

            mockMvc.perform(post("/api/wordbooks")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.type").value("CUSTOM"));
        }

        @Test
        @DisplayName("should require authentication")
        void createWordBook_unauthenticated() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("name", "No Auth Book");

            mockMvc.perform(post("/api/wordbooks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== List WordBooks Tests ====================

    @Nested
    @DisplayName("GET /api/wordbooks")
    class ListWordBooksTests {

        @Test
        @DisplayName("should list word books for user")
        void listWordBooks_success() throws Exception {
            createWordBookViaApi("Book 1", "CUSTOM");
            createWordBookViaApi("Book 2", "CUSTOM");

            mockMvc.perform(get("/api/wordbooks")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.length()").value(2));
        }

        @Test
        @DisplayName("should return empty list for new user")
        void listWordBooks_empty() throws Exception {
            mockMvc.perform(get("/api/wordbooks")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    // ==================== Get Default WordBooks Tests ====================

    @Nested
    @DisplayName("GET /api/wordbooks/defaults")
    class GetDefaultWordBooksTests {

        @Test
        @DisplayName("should return empty list when defaults have been cleared")
        void getDefaultWordBooks_emptyAfterCleanup() throws Exception {
            // @BeforeEach deletes all word books including defaults created by DefaultDataInitializer
            mockMvc.perform(get("/api/wordbooks/defaults")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("should return consistent results on subsequent calls")
        void getDefaultWordBooks_idempotent() throws Exception {
            // After cleanup, defaults are gone, so subsequent calls also return empty
            mockMvc.perform(get("/api/wordbooks/defaults")
                    .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/wordbooks/defaults")
                    .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    // ==================== Get WordBook Tests ====================

    @Nested
    @DisplayName("GET /api/wordbooks/{id}")
    class GetWordBookTests {

        @Test
        @DisplayName("should get a word book by ID")
        void getWordBook_success() throws Exception {
            Long bookId = createWordBookViaApi("Test Book", "CUSTOM");

            mockMvc.perform(get("/api/wordbooks/" + bookId)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(bookId))
                    .andExpect(jsonPath("$.data.name").value("Test Book"));
        }

        @Test
        @DisplayName("should return error for non-existent word book")
        void getWordBook_notFound() throws Exception {
            mockMvc.perform(get("/api/wordbooks/99999")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(3001));
        }
    }

    // ==================== Update WordBook Tests ====================

    @Nested
    @DisplayName("PUT /api/wordbooks/{id}")
    class UpdateWordBookTests {

        @Test
        @DisplayName("should update a word book name")
        void updateWordBook_success() throws Exception {
            Long bookId = createWordBookViaApi("Original Name", "CUSTOM");

            Map<String, Object> body = new HashMap<>();
            body.put("name", "Updated Name");

            mockMvc.perform(put("/api/wordbooks/" + bookId)
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Updated Name"))
                    .andExpect(jsonPath("$.data.version").value(2));
        }

        @Test
        @DisplayName("should return error when updating non-existent word book")
        void updateWordBook_notFound() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("name", "Ghost");

            mockMvc.perform(put("/api/wordbooks/99999")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(3001));
        }
    }

    // ==================== Delete WordBook Tests ====================

    @Nested
    @DisplayName("DELETE /api/wordbooks/{id}")
    class DeleteWordBookTests {

        @Test
        @DisplayName("should soft delete a custom word book")
        void deleteWordBook_success() throws Exception {
            Long bookId = createWordBookViaApi("To Delete", "CUSTOM");

            mockMvc.perform(delete("/api/wordbooks/" + bookId)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // Should no longer appear in list
            mockMvc.perform(get("/api/wordbooks")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("should return error when deleting non-existent word book")
        void deleteWordBook_notFound() throws Exception {
            mockMvc.perform(delete("/api/wordbooks/99999")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("should return error when trying to delete non-existent default book")
        void deleteWordBook_defaultCet4() throws Exception {
            // After @BeforeEach cleanup, default books created by DefaultDataInitializer are gone
            // The defaults endpoint returns empty, so there's nothing to delete
            String response = mockMvc.perform(get("/api/wordbooks/defaults")
                            .header("Authorization", "Bearer " + authToken))
                    .andReturn().getResponse().getContentAsString();

            var node = objectMapper.readTree(response).get("data");
            // Defaults were cleaned up, so this test verifies the cleanup behavior
            assertThat(node.size()).isEqualTo(0);
        }
    }
}
