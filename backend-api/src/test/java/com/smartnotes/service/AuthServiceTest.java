package com.smartnotes.service;

import com.smartnotes.dto.*;
import com.smartnotes.entity.Note;
import com.smartnotes.entity.User;
import com.smartnotes.entity.WordBook;
import com.smartnotes.exception.BusinessException;
import com.smartnotes.repository.NoteRepository;
import com.smartnotes.repository.UserRepository;
import com.smartnotes.repository.WordBookRepository;
import com.smartnotes.repository.WordRepository;
import com.smartnotes.security.JwtUtil;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private WordBookRepository wordBookRepository;

    @Mock
    private WordRepository wordRepository;

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
        testUser.setTokenVersion(0);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    // ==================== Register Tests ====================

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("should successfully register a new user")
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

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getRole()).isEqualTo("USER");
            assertThat(result.getStatus()).isEqualTo("ACTIVE");

            verify(userRepository).existsByUsername("testuser");
            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(argThat(user ->
                    user.getUsername().equals("testuser") &&
                    user.getPasswordHash().equals("encodedPassword") &&
                    "ACTIVE".equals(user.getStatus()) &&
                    "USER".equals(user.getRole())
            ));
        }

        @Test
        @DisplayName("should throw USER_ALREADY_EXISTS when username is taken")
        void register_duplicateUsername() {
            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.USER_ALREADY_EXISTS);

            verify(userRepository).existsByUsername("testuser");
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("should save user with null email when email is not provided")
        void register_nullEmail() {
            RegisterRequest req = RegisterRequest.builder()
                    .username("user2")
                    .password("pass123456")
                    .build();

            when(userRepository.existsByUsername("user2")).thenReturn(false);
            when(passwordEncoder.encode("pass123456")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(2L);
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                return user;
            });

            UserDTO result = authService.register(req);

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("user2");
            verify(userRepository).save(argThat(user -> user.getEmail() == null));
        }
    }

    // ==================== Login Tests ====================

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("should successfully authenticate and return tokens")
        void login_success() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
            when(jwtUtil.generateAccessToken(testUser, 0)).thenReturn("accessToken");
            when(jwtUtil.generateRefreshToken(testUser, 0)).thenReturn("refreshToken");

            LoginResponse result = authService.login(loginRequest);

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("accessToken");
            assertThat(result.getRefreshToken()).isEqualTo("refreshToken");
            assertThat(result.getTokenType()).isEqualTo("Bearer");
            assertThat(result.getExpiresIn()).isEqualTo(86400000L);

            verify(userRepository).findByUsername("testuser");
            verify(passwordEncoder).matches("password123", "encodedPassword");
            verify(jwtUtil).generateAccessToken(testUser, 0);
            verify(jwtUtil).generateRefreshToken(testUser, 0);
        }

        @Test
        @DisplayName("should throw INVALID_CREDENTIALS for wrong password")
        void login_wrongPassword() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

            verify(jwtUtil, never()).generateAccessToken(any(User.class), anyInt());
        }

        @Test
        @DisplayName("should throw INVALID_CREDENTIALS when user not found")
        void login_userNotFound() {
            LoginRequest request = LoginRequest.builder()
                    .username("nonexistent")
                    .password("password123")
                    .build();

            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtUtil, never()).generateAccessToken(any(User.class), anyInt());
        }

        @Test
        @DisplayName("should throw ACCOUNT_DISABLED for disabled user")
        void login_disabledUser() {
            testUser.setStatus("DISABLED");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.ACCOUNT_DISABLED);

            verify(jwtUtil, never()).generateAccessToken(any(User.class), anyInt());
        }
    }

    // ==================== RefreshToken Tests ====================

    @Nested
    @DisplayName("refreshToken()")
    class RefreshTokenTests {

        @Test
        @DisplayName("should return new tokens for valid refresh token")
        void refreshToken_success() {
            when(jwtUtil.validateToken("validRefreshToken")).thenReturn(true);
            when(jwtUtil.extractUsername("validRefreshToken")).thenReturn("testuser");
            when(jwtUtil.extractTokenVersion("validRefreshToken")).thenReturn(0);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtUtil.generateAccessToken(testUser, 1)).thenReturn("newAccessToken");
            when(jwtUtil.generateRefreshToken(testUser, 1)).thenReturn("newRefreshToken");

            LoginResponse result = authService.refreshToken("validRefreshToken");

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("newAccessToken");
            assertThat(result.getRefreshToken()).isEqualTo("newRefreshToken");
            assertThat(result.getTokenType()).isEqualTo("Bearer");

            verify(jwtUtil).validateToken("validRefreshToken");
            verify(jwtUtil).extractUsername("validRefreshToken");
            verify(jwtUtil).extractTokenVersion("validRefreshToken");
            verify(userRepository).findByUsername("testuser");
            verify(jwtUtil).generateAccessToken(testUser, 1);
            verify(jwtUtil).generateRefreshToken(testUser, 1);
        }

        @Test
        @DisplayName("should throw TOKEN_INVALID for invalid token")
        void refreshToken_invalidToken() {
            when(jwtUtil.validateToken("invalidToken")).thenReturn(false);

            assertThatThrownBy(() -> authService.refreshToken("invalidToken"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.TOKEN_INVALID);

            verify(jwtUtil).validateToken("invalidToken");
            verify(jwtUtil, never()).extractUsername(anyString());
            verify(userRepository, never()).findByUsername(anyString());
        }

        @Test
        @DisplayName("should throw TOKEN_INVALID when token version mismatch (already-used token)")
        void refreshToken_alreadyUsedToken() {
            // User has tokenVersion=2 (meaning tokens at version 0 and 1 are revoked)
            testUser.setTokenVersion(2);

            when(jwtUtil.validateToken("oldRefreshToken")).thenReturn(true);
            when(jwtUtil.extractUsername("oldRefreshToken")).thenReturn("testuser");
            when(jwtUtil.extractTokenVersion("oldRefreshToken")).thenReturn(0); // old version
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.refreshToken("oldRefreshToken"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.TOKEN_INVALID);

            verify(userRepository, never()).save(any(User.class));
            verify(jwtUtil, never()).generateAccessToken(any(User.class), anyInt());
        }

        @Test
        @DisplayName("should throw ACCOUNT_DISABLED for disabled user on refresh")
        void refreshToken_disabledUser() {
            testUser.setStatus("DISABLED");

            when(jwtUtil.validateToken("validRefreshToken")).thenReturn(true);
            when(jwtUtil.extractUsername("validRefreshToken")).thenReturn("testuser");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.refreshToken("validRefreshToken"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.ACCOUNT_DISABLED);

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("should throw TOKEN_INVALID when user not found for token")
        void refreshToken_userNotFound() {
            when(jwtUtil.validateToken("validRefreshToken")).thenReturn(true);
            when(jwtUtil.extractUsername("validRefreshToken")).thenReturn("deleteduser");
            when(userRepository.findByUsername("deleteduser")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken("validRefreshToken"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.TOKEN_INVALID);
        }
    }

    // ==================== Login Edge Cases ====================

    @Nested
    @DisplayName("login() - edge cases")
    class LoginEdgeCases {

        @Test
        @DisplayName("should throw INVALID_CREDENTIALS for user with null status")
        void login_nullStatusUser() {
            testUser.setStatus(null);

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.ACCOUNT_DISABLED);

            verify(jwtUtil, never()).generateAccessToken(any(User.class), anyInt());
        }

        @Test
        @DisplayName("should throw INVALID_CREDENTIALS for user with LOCKED status")
        void login_lockedUser() {
            testUser.setStatus("LOCKED");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.ACCOUNT_DISABLED);

            verify(jwtUtil, never()).generateAccessToken(any(User.class), anyInt());
        }

        @Test
        @DisplayName("should throw INVALID_CREDENTIALS for user with BANNED status")
        void login_bannedUser() {
            testUser.setStatus("BANNED");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.ACCOUNT_DISABLED);
        }
    }

    // ==================== ChangePassword Tests ====================

    @Nested
    @DisplayName("changePassword()")
    class ChangePasswordTests {

        @Test
        @DisplayName("should change password and increment tokenVersion")
        void changePassword_success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
            when(passwordEncoder.encode("newPassword456")).thenReturn("newEncodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            authService.changePassword(1L, "password123", "newPassword456");

            verify(userRepository).save(argThat(user ->
                    user.getPasswordHash().equals("newEncodedPassword") &&
                    user.getTokenVersion() == 1
            ));
        }

        @Test
        @DisplayName("should throw NOT_FOUND when user does not exist")
        void changePassword_userNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.changePassword(999L, "old", "new"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.NOT_FOUND);

            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("should throw INVALID_CREDENTIALS for wrong old password")
        void changePassword_wrongOldPassword() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongOldPass", "encodedPassword")).thenReturn(false);

            assertThatThrownBy(() -> authService.changePassword(1L, "wrongOldPass", "newPass"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any(User.class));
        }
    }

    // ==================== RefreshToken Edge Cases ====================

    @Nested
    @DisplayName("refreshToken() - edge cases")
    class RefreshTokenEdgeCases {

        @Test
        @DisplayName("should increment tokenVersion after successful refresh")
        void refreshToken_incrementsVersion() {
            when(jwtUtil.validateToken("validRefreshToken")).thenReturn(true);
            when(jwtUtil.extractUsername("validRefreshToken")).thenReturn("testuser");
            when(jwtUtil.extractTokenVersion("validRefreshToken")).thenReturn(0);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtUtil.generateAccessToken(any(User.class), anyInt())).thenReturn("newAccessToken");
            when(jwtUtil.generateRefreshToken(any(User.class), anyInt())).thenReturn("newRefreshToken");

            authService.refreshToken("validRefreshToken");

            verify(userRepository).save(argThat(user -> user.getTokenVersion() == 1));
        }
    }

    // ==================== GetCurrentUser Tests ====================

    @Nested
    @DisplayName("getCurrentUser()")
    class GetCurrentUserTests {

        @Test
        @DisplayName("should return user DTO for valid user ID")
        void getCurrentUser_success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            UserDTO result = authService.getCurrentUser(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getRole()).isEqualTo("USER");
            assertThat(result.getStatus()).isEqualTo("ACTIVE");
            assertThat(result.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw NOT_FOUND when user does not exist")
        void getCurrentUser_notFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getCurrentUser(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }

    // ==================== RegisterFromGuest Tests ====================

    @Nested
    @DisplayName("registerFromGuest()")
    class RegisterFromGuestTests {

        @Test
        @DisplayName("should register user and migrate guest data")
        void registerFromGuest_success() {
            Note guestNote = new Note();
            guestNote.setId(100L);
            guestNote.setUserId(999L);
            guestNote.setTitle("Guest Note");
            guestNote.setDeleted(false);

            WordBook guestWordBook = new WordBook();
            guestWordBook.setId(200L);
            guestWordBook.setUserId(999L);
            guestWordBook.setDeleted(false);

            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                return user;
            });
            when(noteRepository.findByUserIdAndDeletedFalse(eq(999L), any()))
                    .thenReturn(new PageImpl<>(List.of(guestNote)));
            when(wordBookRepository.findByUserIdAndDeletedFalse(999L))
                    .thenReturn(List.of(guestWordBook));

            UserDTO result = authService.registerFromGuest(registerRequest, 999L);

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");

            verify(noteRepository).save(argThat(note -> note.getUserId().equals(1L)));
            verify(wordBookRepository).save(argThat(wb -> wb.getUserId().equals(1L)));
        }

        @Test
        @DisplayName("should skip migration when guestUserId is null")
        void registerFromGuest_nullGuestId() {
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                return user;
            });

            UserDTO result = authService.registerFromGuest(registerRequest, null);

            assertThat(result).isNotNull();
            verify(noteRepository, never()).findByUserIdAndDeletedFalse(anyLong(), any());
            verify(wordBookRepository, never()).findByUserIdAndDeletedFalse(anyLong());
        }

        @Test
        @DisplayName("should skip migration when guestUserId equals targetUserId")
        void registerFromGuest_sameUserIds() {
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                return user;
            });

            authService.registerFromGuest(registerRequest, 1L);

            verify(noteRepository, never()).findByUserIdAndDeletedFalse(anyLong(), any());
            verify(wordBookRepository, never()).findByUserIdAndDeletedFalse(anyLong());
        }

        @Test
        @DisplayName("should handle guest with no notes or word books")
        void registerFromGuest_noGuestData() {
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                return user;
            });
            when(noteRepository.findByUserIdAndDeletedFalse(eq(999L), any()))
                    .thenReturn(new PageImpl<>(List.of()));
            when(wordBookRepository.findByUserIdAndDeletedFalse(999L))
                    .thenReturn(List.of());

            UserDTO result = authService.registerFromGuest(registerRequest, 999L);

            assertThat(result).isNotNull();
            verify(noteRepository, never()).save(any(Note.class));
            verify(wordBookRepository, never()).save(any(WordBook.class));
        }

        @Test
        @DisplayName("should propagate exception when registration fails during guest migration")
        void registerFromGuest_registrationFails() {
            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            assertThatThrownBy(() -> authService.registerFromGuest(registerRequest, 999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.USER_ALREADY_EXISTS);

            verify(noteRepository, never()).findByUserIdAndDeletedFalse(anyLong(), any());
        }
    }

    // ==================== MigrateGuestData Tests ====================

    @Nested
    @DisplayName("migrateGuestData()")
    class MigrateGuestDataTests {

        @Test
        @DisplayName("should migrate multiple notes")
        void migrateGuestData_multipleNotes() {
            Note note1 = new Note();
            note1.setId(100L);
            note1.setUserId(999L);
            note1.setDeleted(false);

            Note note2 = new Note();
            note2.setId(101L);
            note2.setUserId(999L);
            note2.setDeleted(false);

            when(noteRepository.findByUserIdAndDeletedFalse(eq(999L), any()))
                    .thenReturn(new PageImpl<>(List.of(note1, note2)));
            when(wordBookRepository.findByUserIdAndDeletedFalse(999L))
                    .thenReturn(List.of());
            when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

            authService.migrateGuestData(999L, 1L);

            verify(noteRepository, times(2)).save(argThat(note -> note.getUserId().equals(1L)));
        }

        @Test
        @DisplayName("should migrate multiple word books")
        void migrateGuestData_multipleWordBooks() {
            WordBook wb1 = new WordBook();
            wb1.setId(100L);
            wb1.setUserId(999L);
            wb1.setDeleted(false);

            WordBook wb2 = new WordBook();
            wb2.setId(101L);
            wb2.setUserId(999L);
            wb2.setDeleted(false);

            when(noteRepository.findByUserIdAndDeletedFalse(eq(999L), any()))
                    .thenReturn(new PageImpl<>(List.of()));
            when(wordBookRepository.findByUserIdAndDeletedFalse(999L))
                    .thenReturn(List.of(wb1, wb2));
            when(wordBookRepository.save(any(WordBook.class))).thenAnswer(invocation -> invocation.getArgument(0));

            authService.migrateGuestData(999L, 1L);

            verify(wordBookRepository, times(2)).save(argThat(wb -> wb.getUserId().equals(1L)));
        }

        @Test
        @DisplayName("should not migrate deleted notes")
        void migrateGuestData_skipDeletedNotes() {
            Note deletedNote = new Note();
            deletedNote.setId(100L);
            deletedNote.setUserId(999L);
            deletedNote.setDeleted(true);

            // findByUserIdAndDeletedFalse filters out deleted notes, so this returns empty
            when(noteRepository.findByUserIdAndDeletedFalse(eq(999L), any()))
                    .thenReturn(new PageImpl<>(List.of()));
            when(wordBookRepository.findByUserIdAndDeletedFalse(999L))
                    .thenReturn(List.of());

            authService.migrateGuestData(999L, 1L);

            verify(noteRepository, never()).save(any(Note.class));
        }
    }
}
