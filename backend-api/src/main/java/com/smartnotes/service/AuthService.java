package com.smartnotes.service;

import com.smartnotes.dto.LoginRequest;
import com.smartnotes.dto.LoginResponse;
import com.smartnotes.dto.RegisterRequest;
import com.smartnotes.dto.UserDTO;
import com.smartnotes.entity.User;
import com.smartnotes.exception.BusinessException;
import com.smartnotes.repository.UserRepository;
import com.smartnotes.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

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
