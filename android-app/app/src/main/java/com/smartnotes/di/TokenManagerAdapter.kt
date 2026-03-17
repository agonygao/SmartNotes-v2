package com.smartnotes.di

import com.smartnotes.data.local.TokenManager as DataTokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManagerAdapter @Inject constructor(
    private val tokenManager: DataTokenManager
) : com.smartnotes.ui.viewmodel.TokenManager {

    override fun saveTokens(accessToken: String, refreshToken: String) {
        kotlinx.coroutines.runBlocking {
            tokenManager.saveTokens(accessToken, refreshToken)
        }
    }

    override fun getAccessToken(): String? {
        return tokenManager.getAccessTokenSync()
    }

    override fun getRefreshToken(): String? {
        return kotlinx.coroutines.runBlocking {
            tokenManager.getRefreshToken()
        }
    }

    override fun clearTokens() {
        tokenManager.clearTokens()
    }

    override fun isTokenValid(): Boolean {
        return tokenManager.isTokenAvailable()
    }
}
