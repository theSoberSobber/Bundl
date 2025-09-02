package com.pavit.bundl.utils.network

import android.util.Log
import com.pavit.bundl.data.local.TokenManager
import com.pavit.bundl.data.remote.api.AuthApiService
import com.pavit.bundl.data.remote.dto.RefreshTokenRequestDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

@Singleton
class AuthAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApiService: AuthApiService
) : Authenticator {
    
    companion object {
        private const val TAG = "AuthAuthenticator"
        // Mutex to prevent multiple simultaneous refresh attempts
        private val refreshMutex = Mutex()
    }
    
    override fun authenticate(route: Route?, response: Response): Request? {
        Log.d(TAG, "Attempting to authenticate failed request: ${response.request.method} ${response.request.url}")
        
        // Don't attempt to refresh if the failed request is the refresh endpoint itself
        val requestUrl = response.request.url.toString()
        if (requestUrl.contains("/auth/refresh")) {
            Log.w(TAG, "The failing request is a refresh token request - not attempting to refresh again")
            return null
        }
        
        // Use mutex to ensure only one refresh happens at a time
        return runBlocking {
            refreshMutex.withLock {
                // Check if another thread already refreshed the token
                val currentToken = tokenManager.getAccessToken().first()
                val authHeader = response.request.header("Authorization")
                val requestToken = authHeader?.substringAfter("Bearer ")
                
                if (currentToken != null && requestToken != null && currentToken != requestToken) {
                    // Token has already been refreshed by another request
                    Log.d(TAG, "Token was already refreshed by another request. Using the new token.")
                    return@withLock response.request.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                }
                
                // Proceed with normal refresh
                val refreshToken = tokenManager.getRefreshToken().first()
                if (refreshToken == null) {
                    Log.e(TAG, "No refresh token available")
                    return@withLock null
                }
                
                Log.d(TAG, "Refresh token available, attempting to refresh access token")
                
                try {
                    val refreshResponse = authApiService.refreshToken(RefreshTokenRequestDto(refreshToken))
                    
                    // Handle partial token refresh (server sometimes returns only access token)
                    if (refreshResponse.accessToken.isNullOrEmpty() && refreshResponse.refreshToken.isNullOrEmpty()) {
                        // Both tokens are missing - this is a genuine error
                        Log.e(TAG, "Received completely invalid tokens in refresh response - both tokens missing")
                        return@withLock null
                    } else if (!refreshResponse.accessToken.isNullOrEmpty() && refreshResponse.refreshToken.isNullOrEmpty()) {
                        // Only access token provided - use it with the existing refresh token
                        Log.w(TAG, "Received partial token refresh: New access token but no refresh token, keeping old refresh token")
                        try {
                            tokenManager.saveTokens(
                                accessToken = refreshResponse.accessToken,
                                refreshToken = refreshToken  // Keep the old refresh token
                            )
                            
                            // Retry the original request with new token
                            Log.d(TAG, "Retrying original request with new access token (kept old refresh token): ${response.request.method} ${response.request.url}")
                            return@withLock response.request.newBuilder()
                                .header("Authorization", "Bearer ${refreshResponse.accessToken}")
                                .build()
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to save partial refreshed tokens", e)
                            return@withLock null
                        }
                    }
                    
                    // Normal case: Both tokens were provided
                    Log.d(TAG, "Successfully refreshed both tokens")
                    
                    try {
                        tokenManager.saveTokens(
                            accessToken = refreshResponse.accessToken,
                            refreshToken = refreshResponse.refreshToken
                        )
                        
                        // Retry the original request with new token
                        Log.d(TAG, "Retrying original request with new token: ${response.request.method} ${response.request.url}")
                        return@withLock response.request.newBuilder()
                            .header("Authorization", "Bearer ${refreshResponse.accessToken}")
                            .build()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to save refreshed tokens", e)
                        return@withLock null
                    }
                        
                } catch (e: HttpException) {
                    // No need to handle 403 here as the AuthInterceptor will handle it
                    // when the retry is attempted
                    Log.e(TAG, "Failed to refresh token. Status: ${e.code()}")
                    Log.e(TAG, "Error message: ${e.message()}")
                    null
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during token refresh", e)
                    null
                }
            }
        }
    }
} 