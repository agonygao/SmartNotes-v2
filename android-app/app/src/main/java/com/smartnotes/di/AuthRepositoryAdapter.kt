package com.smartnotes.di

import com.smartnotes.data.repository.AuthRepository as DataAuthRepository
import com.smartnotes.ui.viewmodel.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryAdapter @Inject constructor(
    private val authRepository: DataAuthRepository,
    private val tokenManagerAdapter: TokenManagerAdapter
) : com.smartnotes.ui.viewmodel.AuthRepository {

    override suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return authRepository.login(request.username, request.password).map { response ->
            AuthResponse(
                token = response.accessToken,
                refreshToken = response.refreshToken,
                username = "",
                userId = 0,
                role = "user",
            )
        }
    }

    override suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        // Register returns UserDTO, then login to get tokens
        return authRepository.register(
            username = request.username,
            password = request.password,
            email = request.email,
        ).map { userDTO ->
            // Auto-login after registration
            val loginResult = authRepository.login(request.username, request.password)
            if (loginResult.isSuccess) {
                val lr = loginResult.getOrNull()!!
                AuthResponse(
                    token = lr.accessToken,
                    refreshToken = lr.refreshToken,
                    username = userDTO.username,
                    userId = userDTO.id,
                    role = userDTO.role,
                )
            } else {
                AuthResponse(
                    token = "",
                    refreshToken = "",
                    username = userDTO.username,
                    userId = userDTO.id,
                    role = userDTO.role,
                )
            }
        }
    }

    override suspend fun refreshToken(refreshToken: String): Result<AuthResponse> {
        return authRepository.refreshToken().map { response ->
            AuthResponse(
                token = response.accessToken,
                refreshToken = response.refreshToken,
                username = "",
                userId = 0,
                role = "user",
            )
        }
    }

    override suspend fun getCurrentUser(token: String): Result<UserInfo> {
        return authRepository.getCurrentUser().map { userDTO ->
            UserInfo(
                id = userDTO.id,
                username = userDTO.username,
                email = userDTO.email,
                role = userDTO.role,
            )
        }
    }
}
