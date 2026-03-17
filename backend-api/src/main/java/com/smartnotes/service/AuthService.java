package com.smartnotes.service;

import com.smartnotes.dto.LoginRequest;
import com.smartnotes.dto.LoginResponse;
import com.smartnotes.dto.RegisterRequest;
import com.smartnotes.dto.UserDTO;
import com.smartnotes.entity.Note;
import com.smartnotes.entity.User;
import com.smartnotes.entity.Word;
import com.smartnotes.exception.BusinessException;
import com.smartnotes.repository.NoteRepository;
import com.smartnotes.repository.UserRepository;
import com.smartnotes.repository.WordBookRepository;
import com.smartnotes.repository.WordRepository;
import com.smartnotes.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final NoteRepository noteRepository;
    private final WordBookRepository wordBookRepository;
    private final WordRepository wordRepository;

    @Transactional
    public UserDTO register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(
                    com.smartnotes.dto.ErrorCode.USER_ALREADY_EXISTS,
                    "username=" + request.getUsername()
            );
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRole("USER");
        user.setStatus("ACTIVE");

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getUsername());

        return toUserDTO(user);
    }

    /**
     * Register a new user and migrate guest data from a temporary guest user account.
     *
     * @param request    the registration request
     * @param guestUserId the ID of the guest user whose data should be migrated
     * @return the registered user DTO
     */
    @Transactional
    public UserDTO registerFromGuest(RegisterRequest request, Long guestUserId) {
        UserDTO registeredUser = register(request);
        migrateGuestData(guestUserId, registeredUser.getId());
        return registeredUser;
    }

    /**
     * Migrate all non-deleted notes and word progress records from a guest user
     * to a target user account.
     *
     * @param guestUserId  the guest user's ID
     * @param targetUserId the target (registered) user's ID
     */
    @Transactional
    public void migrateGuestData(Long guestUserId, Long targetUserId) {
        if (guestUserId == null || targetUserId == null) {
            log.warn("Skipping guest data migration: guestUserId or targetUserId is null");
            return;
        }

        if (guestUserId.equals(targetUserId)) {
            log.warn("Skipping guest data migration: guestUserId and targetUserId are the same");
            return;
        }

        // Migrate notes
        List<Note> guestNotes = noteRepository.findByUserIdAndDeletedFalse(guestUserId,
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        int noteCount = 0;
        for (Note note : guestNotes) {
            note.setUserId(targetUserId);
            noteRepository.save(note);
            noteCount++;
        }

        // Migrate word books
        List<com.smartnotes.entity.WordBook> guestWordBooks = wordBookRepository.findByUserIdAndDeletedFalse(guestUserId);
        int wordBookCount = 0;
        for (com.smartnotes.entity.WordBook wordBook : guestWordBooks) {
            wordBook.setUserId(targetUserId);
            wordBookRepository.save(wordBook);
            wordBookCount++;
        }

        log.info("Guest data migration completed: guestUserId={}, targetUserId={}, notes={}, wordBooks={}",
                guestUserId, targetUserId, noteCount, wordBookCount);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(
                        com.smartnotes.dto.ErrorCode.INVALID_CREDENTIALS
                ));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(com.smartnotes.dto.ErrorCode.INVALID_CREDENTIALS);
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(com.smartnotes.dto.ErrorCode.ACCOUNT_DISABLED);
        }

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        log.info("User logged in successfully: {}", user.getUsername());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .build();
    }

    @Transactional(readOnly = true)
    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(com.smartnotes.dto.ErrorCode.TOKEN_INVALID);
        }

        String username = jwtUtil.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(com.smartnotes.dto.ErrorCode.TOKEN_INVALID));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(com.smartnotes.dto.ErrorCode.ACCOUNT_DISABLED);
        }

        String newAccessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);

        log.info("Token refreshed for user: {}", user.getUsername());

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .build();
    }

    @Transactional(readOnly = true)
    public UserDTO getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(com.smartnotes.dto.ErrorCode.NOT_FOUND));
        return toUserDTO(user);
    }

    private UserDTO toUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
