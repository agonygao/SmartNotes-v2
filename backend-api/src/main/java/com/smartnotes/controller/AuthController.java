package com.smartnotes.controller;

import com.smartnotes.dto.ApiResponse;
import com.smartnotes.dto.LoginRequest;
import com.smartnotes.dto.LoginResponse;
import com.smartnotes.dto.RegisterRequest;
import com.smartnotes.dto.UserDTO;
import com.smartnotes.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
        UserDTO userDTO = authService.register(request);
        return ApiResponse.success(userDTO);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = authService.login(request);
        return ApiResponse.success(loginResponse);
    }

    @GetMapping("/refresh")
    public ApiResponse<LoginResponse> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "refreshToken", required = false) String refreshTokenParam
    ) {
        String refreshToken = resolveRefreshToken(authHeader, refreshTokenParam);
        LoginResponse loginResponse = authService.refreshToken(refreshToken);
        return ApiResponse.success(loginResponse);
    }

    @GetMapping("/me")
    public ApiResponse<UserDTO> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getPrincipal();
        UserDTO userDTO = authService.getCurrentUser(userId);
        return ApiResponse.success(userDTO);
    }

    private String resolveRefreshToken(String authHeader, String refreshTokenParam) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (StringUtils.hasText(refreshTokenParam)) {
            return refreshTokenParam;
        }
        throw new com.smartnotes.exception.BusinessException(
                com.smartnotes.dto.ErrorCode.BAD_REQUEST,
                "refreshToken is required"
        );
    }
}
