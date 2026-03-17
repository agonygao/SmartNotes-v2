package com.smartnotes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---------------------------------------------------------------------------
// Data models for auth
// ---------------------------------------------------------------------------
data class LoginRequest(
    val username: String,
    val password: String,
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
)

data class AuthResponse(
    val token: String,
    val refreshToken: String,
    val username: String,
    val userId: Long,
    val role: String,
)

data class UserInfo(
    val id: Long,
    val username: String,
    val email: String?,
    val role: String,
)

// ---------------------------------------------------------------------------
// Auth UI state
// ---------------------------------------------------------------------------
sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Success(val user: UserInfo) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

// ---------------------------------------------------------------------------
// TokenManager - responsible for persisting JWT tokens
// ---------------------------------------------------------------------------
interface TokenManager {
    fun saveTokens(accessToken: String, refreshToken: String)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun clearTokens()
    fun isTokenValid(): Boolean
}

// ---------------------------------------------------------------------------
// AuthRepository interface
// ---------------------------------------------------------------------------
interface AuthRepository {
    suspend fun login(request: LoginRequest): Result<AuthResponse>
    suspend fun register(request: RegisterRequest): Result<AuthResponse>
    suspend fun refreshToken(refreshToken: String): Result<AuthResponse>
    suspend fun getCurrentUser(token: String): Result<UserInfo>
}

// ---------------------------------------------------------------------------
// AuthViewModel
// ---------------------------------------------------------------------------
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
) : ViewModel() {

    private val _loginState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val loginState: StateFlow<AuthUiState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val registerState: StateFlow<AuthUiState> = _registerState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    val currentUser: StateFlow<UserInfo?> = _currentUser.asStateFlow()

    init {
        checkAutoLogin()
    }

    /**
     * Checks if the user has valid stored tokens and attempts auto-login.
     */
    private fun checkAutoLogin() {
        viewModelScope.launch {
            val accessToken = tokenManager.getAccessToken()
            if (accessToken != null && tokenManager.isTokenValid()) {
                val result = authRepository.getCurrentUser(accessToken)
                if (result.isSuccess) {
                    _currentUser.value = result.getOrNull()
                    _isLoggedIn.value = true
                } else {
                    // Token might be expired, try refreshing
                    attemptTokenRefresh()
                }
            }
        }
    }

    /**
     * Attempts to refresh the access token using the stored refresh token.
     */
    private suspend fun attemptTokenRefresh() {
        val refreshToken = tokenManager.getRefreshToken() ?: return
        val result = authRepository.refreshToken(refreshToken)
        if (result.isSuccess) {
            val response = result.getOrNull()!!
            tokenManager.saveTokens(response.token, response.refreshToken)
            _isLoggedIn.value = true
            _currentUser.value = UserInfo(
                id = response.userId,
                username = response.username,
                email = null,
                role = response.role,
            )
        } else {
            // Refresh failed, clear tokens
            tokenManager.clearTokens()
            _isLoggedIn.value = false
        }
    }

    /**
     * Login with username and password.
     */
    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _loginState.value = AuthUiState.Error("Username and password must not be empty")
            return
        }

        viewModelScope.launch {
            _loginState.value = AuthUiState.Loading
            val result = authRepository.login(LoginRequest(username, password))
            if (result.isSuccess) {
                val response = result.getOrNull()!!
                tokenManager.saveTokens(response.token, response.refreshToken)
                _currentUser.value = UserInfo(
                    id = response.userId,
                    username = response.username,
                    email = null,
                    role = response.role,
                )
                _isLoggedIn.value = true
                _loginState.value = AuthUiState.Success(_currentUser.value!!)
            } else {
                _loginState.value = AuthUiState.Error(
                    result.exceptionOrNull()?.message ?: "Login failed"
                )
            }
        }
    }

    /**
     * Register a new account.
     */
    fun register(username: String, email: String, password: String) {
        if (username.isBlank()) {
            _registerState.value = AuthUiState.Error("Username must not be empty")
            return
        }
        if (email.isBlank() || !email.contains("@")) {
            _registerState.value = AuthUiState.Error("Please enter a valid email address")
            return
        }
        if (password.length < 6) {
            _registerState.value = AuthUiState.Error("Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            _registerState.value = AuthUiState.Loading
            val result = authRepository.register(RegisterRequest(username, email, password))
            if (result.isSuccess) {
                val response = result.getOrNull()!!
                tokenManager.saveTokens(response.token, response.refreshToken)
                _currentUser.value = UserInfo(
                    id = response.userId,
                    username = response.username,
                    email = email,
                    role = response.role,
                )
                _isLoggedIn.value = true
                _registerState.value = AuthUiState.Success(_currentUser.value!!)
            } else {
                _registerState.value = AuthUiState.Error(
                    result.exceptionOrNull()?.message ?: "Registration failed"
                )
            }
        }
    }

    /**
     * Logout and clear all stored credentials.
     */
    fun logout() {
        tokenManager.clearTokens()
        _isLoggedIn.value = false
        _currentUser.value = null
        _loginState.value = AuthUiState.Idle
        _registerState.value = AuthUiState.Idle
    }

    /**
     * Reset the login state back to Idle (e.g., after navigating away from error).
     */
    fun resetLoginState() {
        _loginState.value = AuthUiState.Idle
    }

    /**
     * Reset the register state back to Idle.
     */
    fun resetRegisterState() {
        _registerState.value = AuthUiState.Idle
    }

    /**
     * Refresh the current user's access token.
     */
    fun refresh() {
        viewModelScope.launch {
            attemptTokenRefresh()
        }
    }
}
