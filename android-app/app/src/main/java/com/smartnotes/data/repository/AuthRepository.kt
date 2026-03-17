package com.smartnotes.data.repository

import com.smartnotes.data.api.ApiResponse
import com.smartnotes.data.api.ApiService
import com.smartnotes.data.api.LoginRequest
import com.smartnotes.data.api.LoginResponse
import com.smartnotes.data.api.RegisterRequest
import com.smartnotes.data.api.UserDTO
import com.smartnotes.data.local.TokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {

    suspend fun register(
        username: String,
        password: String,
        email: String? = null
    ): Result<UserDTO> {
        return try {
            val response = apiService.register(
                RegisterRequest(
                    username = username,
                    password = password,
                    email = email
                )
            )
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(
        username: String,
        password: String
    ): Result<LoginResponse> {
        return try {
            val response = apiService.login(
                LoginRequest(
                    username = username,
                    password = password
                )
            )
            if (response.isSuccess && response.data != null) {
                tokenManager.saveTokens(
                    accessToken = response.data.accessToken,
                    refreshToken = response.data.refreshToken
                )
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshToken(): Result<LoginResponse> {
        return try {
            val refreshTokenValue = tokenManager.getRefreshToken()
            if (refreshTokenValue.isNullOrBlank()) {
                return Result.failure(Exception("No refresh token available"))
            }
            val response = apiService.refreshToken(
                authHeader = "Bearer $refreshTokenValue",
                refreshToken = refreshTokenValue
            )
            if (response.isSuccess && response.data != null) {
                tokenManager.saveTokens(
                    accessToken = response.data.accessToken,
                    refreshToken = response.data.refreshToken
                )
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            tokenManager.clearTokens()
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): Result<UserDTO> {
        return try {
            val response = apiService.getCurrentUser()
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        tokenManager.clearTokens()
    }

    fun isLoggedIn(): Boolean {
        return tokenManager.getAccessTokenSync().isNullOrBlank().not()
    }
}
