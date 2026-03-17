package com.smartnotes.service;

import com.smartnotes.dto.*;
import com.smartnotes.entity.User;
import com.smartnotes.exception.BusinessException;
import com.smartnotes.repository.UserRepository;
import com.smartnotes.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .username("testuser")
                .password("password123")
                .email("test@example.com")
                .build();

        loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPasswordHash("encodedPassword");
        testUser.setEmail("test@example.com");
        testUser.setRole("USER");
        testUser.setStatus("ACTIVE");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("register - should successfully register a new user")
    void register_success() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        UserDTO result = authService.register(registerRequest);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("USER", result.getRole());
        assertEquals("ACTIVE", result.getStatus());

        verify(userRepository).existsByUsername("testuser");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register - should throw exception when username already exists")
    void register_duplicateUsername() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(registerRequest));

        assertEquals(ErrorCode.USER_ALREADY_EXISTS, exception.getCode());

        verify(userRepository).existsByUsername("testuser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("login - should successfully authenticate and return tokens")
    void login_success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateAccessToken(testUser)).thenReturn("accessToken");
        when(jwtUtil.generateRefreshToken(testUser)).thenReturn("refreshToken");

        LoginResponse result = authService.login(loginRequest);

        assertNotNull(result);
        assertEquals("accessToken", result.getAccessToken());
        assertEquals("refreshToken", result.getRefreshToken());
        assertEquals("Bearer", result.getTokenType());
        assertEquals(86400000L, result.getExpiresIn());

        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder).matches("password123", "encodedPassword");
        verify(jwtUtil).generateAccessToken(testUser);
        verify(jwtUtil).generateRefreshToken(testUser);
    }

    @Test
    @DisplayName("login - should throw exception for wrong password")
    void login_invalidCredentials_wrongPassword() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest));

        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getCode());

        verify(jwtUtil, never()).generateAccessToken(any(User.class));
    }

    @Test
    @DisplayName("login - should throw exception when user not found")
    void login_userNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        LoginRequest request = LoginRequest.builder()
                .username("nonexistent")
                .password("password123")
                .build();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login(request));

        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getCode());

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("login - should throw exception when account is disabled")
    void login_accountDisabled() {
        testUser.setStatus("DISABLED");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest));

        assertEquals(ErrorCode.ACCOUNT_DISABLED, exception.getCode());

        verify(jwtUtil, never()).generateAccessToken(any(User.class));
    }

    @Test
    @DisplayName("refreshToken - should return new tokens for valid refresh token")
    void refreshToken_success() {
        when(jwtUtil.validateToken("validRefreshToken")).thenReturn(true);
        when(jwtUtil.extractUsername("validRefreshToken")).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateAccessToken(testUser)).thenReturn("newAccessToken");
        when(jwtUtil.generateRefreshToken(testUser)).thenReturn("newRefreshToken");

        LoginResponse result = authService.refreshToken("validRefreshToken");

        assertNotNull(result);
        assertEquals("newAccessToken", result.getAccessToken());
        assertEquals("newRefreshToken", result.getRefreshToken());
        assertEquals("Bearer", result.getTokenType());

        verify(jwtUtil).validateToken("validRefreshToken");
        verify(jwtUtil).extractUsername("validRefreshToken");
        verify(userRepository).findByUsername("testuser");
        verify(jwtUtil).generateAccessToken(testUser);
        verify(jwtUtil).generateRefreshToken(testUser);
    }

    @Test
    @DisplayName("refreshToken - should throw exception for invalid token")
    void refreshToken_invalidToken() {
        when(jwtUtil.validateToken("invalidToken")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.refreshToken("invalidToken"));

        assertEquals(ErrorCode.TOKEN_INVALID, exception.getCode());

        verify(jwtUtil).validateToken("invalidToken");
        verify(jwtUtil, never()).extractUsername(anyString());
    }

    @Test
    @DisplayName("getCurrentUser - should return user DTO for valid user ID")
    void getCurrentUser_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserDTO result = authService.getCurrentUser(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("USER", result.getRole());
        assertEquals("ACTIVE", result.getStatus());
        assertNotNull(result.getCreatedAt());

        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("getCurrentUser - should throw exception when user not found")
    void getCurrentUser_notFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.getCurrentUser(999L));

        assertEquals(ErrorCode.NOT_FOUND, exception.getCode());

        verify(userRepository).findById(999L);
    }
}
