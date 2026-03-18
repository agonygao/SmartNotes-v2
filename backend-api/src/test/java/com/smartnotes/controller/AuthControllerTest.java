package com.smartnotes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartnotes.dto.LoginRequest;
import com.smartnotes.dto.LoginResponse;
import com.smartnotes.dto.NoteRequest;
import com.smartnotes.dto.RegisterRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.smartnotes.SmartNotesApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private String authToken;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        RegisterRequest register = RegisterRequest.builder()
                .username("testuser")
                .password("password123")
                .email("test@example.com")
                .build();

        User user = new User();
        user.setUsername("testuser");
        user.setPasswordHash(org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.class
                .cast(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder())
                .encode("password123"));
        user.setEmail("test@example.com");
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setTokenVersion(0);
        user = userRepository.save(user);
        testUserId = user.getId();

        authToken = jwtUtil.generateAccessToken(user, user.getTokenVersion());
    }

    private String generateToken() {
        User user = userRepository.findById(testUserId).orElseThrow();
        return jwtUtil.generateAccessToken(user, user.getTokenVersion());
    }

    // ==================== Register Tests ====================

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("should register a new user successfully")
        void register_success() throws Exception {
            Map<String, String> body = new HashMap<>();
            body.put("username", "newuser");
            body.put("password", "password123456");
            body.put("email", "new@example.com");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.username").value("newuser"))
                    .andExpect(jsonPath("$.data.email").value("new@example.com"))
                    .andExpect(jsonPath("$.data.role").value("USER"))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("should register without email")
        void register_noEmail() throws Exception {
            Map<String, String> body = new HashMap<>();
            body.put("username", "noemailuser");
            body.put("password", "password123456");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.username").value("noemailuser"));
        }

        @Test
        @DisplayName("should fail when username is already taken")
        void register_duplicateUsername() throws Exception {
            Map<String, String> body = new HashMap<>();
            body.put("username", "testuser");
            body.put("password", "password123456");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(1001));
        }

        @Test
        @DisplayName("should fail when username is too short")
        void register_shortUsername() throws Exception {
            Map<String, String> body = new HashMap<>();
            body.put("username", "ab");
            body.put("password", "password123456");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should fail when password is too short")
        void register_shortPassword() throws Exception {
            Map<String, String> body = new HashMap<>();
            body.put("username", "validuser123");
            body.put("password", "short");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should fail when username is missing")
        void register_missingUsername() throws Exception {
            Map<String, String> body = new HashMap<>();
            body.put("password", "password123456");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should fail with empty body")
        void register_emptyBody() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should fail when password is missing")
        void register_missingPassword() throws Exception {
            Map<String, String> body = new HashMap<>();
            body.put("username", "validuser456");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== Login Tests ====================

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("should login successfully and return tokens")
        void login_success() throws Exception {
            Map<String, String> body = new HashMap<>();
            body.put("username", "testuser");
            body.put("password", "password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.accessToken").isString())
                    .andExpect(jsonPath("$.data.refreshToken").isString())
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.expiresIn").isNumber());
        }

        @Test
        @DisplayName("should fail with wrong password")
        void login_wrongPassword() throws Exception {
            Map<String, String> body = new HashMap<>();
            body.put("username", "testuser");
            body.put("password", "wrongpassword");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(1002));
        }

        @Test
        @DisplayName("should fail with non-existent username")
        void login_userNotFound() throws Exception {
            Map<String, String> body = new HashMap<>();
            body.put("username", "nonexistent");
            body.put("password", "password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(1002));
        }

        @Test
        @DisplayName("should fail for disabled user")
        void login_disabledUser() throws Exception {
            // Create a disabled user
            com.smartnotes.entity.User disabledUser = new com.smartnotes.entity.User();
            disabledUser.setUsername("disableduser");
            disabledUser.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("password123"));
            disabledUser.setEmail("disabled@example.com");
            disabledUser.setRole("USER");
            disabledUser.setStatus("DISABLED");
            disabledUser.setTokenVersion(0);
            userRepository.save(disabledUser);

            Map<String, String> body = new HashMap<>();
            body.put("username", "disableduser");
            body.put("password", "password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(1005));
        }

        @Test
        @DisplayName("should fail when username is missing")
        void login_missingUsername() throws Exception {
            Map<String, String> body = new HashMap<>();
            body.put("password", "password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should fail when password is missing")
        void login_missingPassword() throws Exception {
            Map<String, String> body = new HashMap<>();
            body.put("username", "testuser");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== Refresh Token Tests ====================

    @Nested
    @DisplayName("GET /api/auth/refresh")
    class RefreshTokenTests {

        @Test
        @DisplayName("should refresh token via Authorization header")
        void refreshToken_viaHeader() throws Exception {
            User user = userRepository.findById(testUserId).orElseThrow();
            String refreshToken = jwtUtil.generateRefreshToken(user, user.getTokenVersion());

            mockMvc.perform(get("/api/auth/refresh")
                            .header("Authorization", "Bearer " + refreshToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.accessToken").isString())
                    .andExpect(jsonPath("$.data.refreshToken").isString());
        }

        @Test
        @DisplayName("should refresh token via query parameter")
        void refreshToken_viaParam() throws Exception {
            User user = userRepository.findById(testUserId).orElseThrow();
            String refreshToken = jwtUtil.generateRefreshToken(user, user.getTokenVersion());

            mockMvc.perform(get("/api/auth/refresh")
                            .param("refreshToken", refreshToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.accessToken").isString());
        }

        @Test
        @DisplayName("should fail when no refresh token provided")
        void refreshToken_missing() throws Exception {
            mockMvc.perform(get("/api/auth/refresh"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("should reject reused refresh token (token version mismatch)")
        void refreshToken_reuseDetected() throws Exception {
            User user = userRepository.findById(testUserId).orElseThrow();
            String refreshToken = jwtUtil.generateRefreshToken(user, user.getTokenVersion());

            // First refresh succeeds
            mockMvc.perform(get("/api/auth/refresh")
                            .header("Authorization", "Bearer " + refreshToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isString());

            // Second use of the same token should fail (version mismatch)
            mockMvc.perform(get("/api/auth/refresh")
                            .header("Authorization", "Bearer " + refreshToken))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(1004));
        }

        @Test
        @DisplayName("should fail with invalid refresh token")
        void refreshToken_invalid() throws Exception {
            mockMvc.perform(get("/api/auth/refresh")
                            .header("Authorization", "Bearer invalid-token"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(1004));
        }
    }

    // ==================== Get Current User Tests ====================

    @Nested
    @DisplayName("GET /api/auth/me")
    class GetCurrentUserTests {

        @Test
        @DisplayName("should return current user info with valid token")
        void me_success() throws Exception {
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer " + generateToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.id").value(testUserId))
                    .andExpect(jsonPath("$.data.username").value("testuser"))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.role").value("USER"));
        }

        @Test
        @DisplayName("should fail without authentication")
        void me_unauthenticated() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should fail with invalid token")
        void me_invalidToken() throws Exception {
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer invalid-token"))
                    .andExpect(status().isForbidden());
        }
    }
}
