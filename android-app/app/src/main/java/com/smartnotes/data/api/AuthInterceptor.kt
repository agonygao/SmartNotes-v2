package com.smartnotes.data.api

import com.smartnotes.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val accessToken = runBlocking { tokenManager.getAccessToken() }

        val newRequest = if (!accessToken.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header(HEADER_AUTHORIZATION, BEARER_PREFIX + accessToken)
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(newRequest)
    }
}
