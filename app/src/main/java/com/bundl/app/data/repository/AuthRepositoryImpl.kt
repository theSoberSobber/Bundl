package com.bundl.app.data.repository

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.bundl.app.data.local.TokenManager
import com.bundl.app.data.remote.api.AuthApiService
import com.bundl.app.data.remote.dto.OtpSendRequestDto
import com.bundl.app.data.remote.dto.OtpVerifyRequestDto
import com.bundl.app.data.remote.dto.RefreshTokenRequestDto
import com.bundl.app.domain.model.AuthResponse
import com.bundl.app.domain.model.OtpSendResponse
import com.bundl.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) : AuthRepository {
    
    companion object {
        private const val TAG = "AuthRepository"
    }
    
    override suspend fun sendOtp(phoneNumber: String): Result<OtpSendResponse> {
        return try {
            val response = authApiService.sendOtp(OtpSendRequestDto(phoneNumber))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun verifyOtp(tid: String, otp: String, fcmToken: String): Result<AuthResponse> {
        return try {
            val response = authApiService.verifyOtp(OtpVerifyRequestDto(tid, otp, fcmToken))
            
            // Check if we got valid tokens
            if (response.hasValidTokens()) {
                // Save tokens on successful verification
                saveAuthTokens(response.accessToken, response.refreshToken)
                Result.success(response)
            } else {
                Log.e(TAG, "Verify OTP didn't return valid tokens: access=${response.accessToken != null}, refresh=${response.refreshToken != null}")
                Result.failure(Exception("Invalid tokens received"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun refreshToken(refreshToken: String): Result<AuthResponse> {
        return try {
            val response = authApiService.refreshToken(RefreshTokenRequestDto(refreshToken))
            
            // Handle different token refresh scenarios
            if (response.accessToken.isNullOrEmpty() && response.refreshToken.isNullOrEmpty()) {
                // Both tokens missing - genuine error
                Log.e(TAG, "Refresh token didn't return valid tokens - both tokens missing")
                Result.failure(Exception("Invalid tokens received"))
            } else if (!response.accessToken.isNullOrEmpty() && response.refreshToken.isNullOrEmpty()) {
                // Only access token returned - use it with existing refresh token
                Log.w(TAG, "Partial token refresh: New access token but no refresh token, keeping old refresh token")
                saveAuthTokens(response.accessToken, refreshToken)
                
                // Return modified response with both tokens
                val updatedResponse = AuthResponse(
                    accessToken = response.accessToken,
                    refreshToken = refreshToken
                )
                Result.success(updatedResponse)
            } else {
                // Normal case: Both tokens provided
                Log.d(TAG, "Successfully refreshed both tokens")
                saveAuthTokens(response.accessToken, response.refreshToken)
                Result.success(response)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun saveAuthTokens(accessToken: String?, refreshToken: String?) {
        tokenManager.saveTokens(accessToken, refreshToken)
    }
    
    override suspend fun clearAuthTokens() {
        tokenManager.clearTokens()
    }
    
    override suspend fun logout() {
        try {
            // Delete FCM token first
            FirebaseMessaging.getInstance().deleteToken().await()
            Log.d(TAG, "FCM token deleted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete FCM token", e)
        }
        
        // Then clear auth tokens
        clearAuthTokens()
    }
    
    override fun getAccessToken(): Flow<String?> {
        return tokenManager.getAccessToken()
    }
    
    override fun getRefreshToken(): Flow<String?> {
        return tokenManager.getRefreshToken()
    }
    
    override fun isLoggedIn(): Flow<Boolean> {
        return tokenManager.isLoggedIn()
    }
} 