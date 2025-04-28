package com.orvio.app.utils.network

import com.orvio.app.data.local.TokenManager
import com.orvio.app.data.remote.api.AuthApiService
import com.orvio.app.data.remote.dto.RefreshTokenRequestDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

class AuthAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApiService: AuthApiService
) : Authenticator {
    
    override fun authenticate(route: Route?, response: Response): Request? {
        return runBlocking {
            // Get the refresh token
            val refreshToken = tokenManager.getRefreshToken().first()
            
            if (refreshToken == null) {
                tokenManager.clearTokens()
                return@runBlocking null
            }
            
            try {
                // Try to refresh the token
                val authResponse = authApiService.refreshToken(RefreshTokenRequestDto(refreshToken))
                
                // Save the new tokens
                tokenManager.saveTokens(authResponse.accessToken, authResponse.refreshToken)
                
                // Create a new request with the new token
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${authResponse.accessToken}")
                    .build()
            } catch (e: Exception) {
                // If refresh failed, clear tokens and return null
                tokenManager.clearTokens()
                null
            }
        }
    }
} 