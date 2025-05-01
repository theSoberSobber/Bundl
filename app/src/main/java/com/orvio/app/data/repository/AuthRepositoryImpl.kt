package com.orvio.app.data.repository

import android.util.Log
import com.orvio.app.data.local.TokenManager
import com.orvio.app.data.remote.api.AuthApiService
import com.orvio.app.data.remote.dto.OtpSendRequestDto
import com.orvio.app.data.remote.dto.OtpVerifyRequestDto
import com.orvio.app.data.remote.dto.RefreshTokenRequestDto
import com.orvio.app.domain.model.AuthResponse
import com.orvio.app.domain.model.OtpSendResponse
import com.orvio.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    
    override suspend fun verifyOtp(transactionId: String, otp: String): Result<AuthResponse> {
        return try {
            val response = authApiService.verifyOtp(OtpVerifyRequestDto(transactionId, otp))
            
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
        if (accessToken != null && refreshToken != null) {
            tokenManager.saveTokens(accessToken, refreshToken)
        } else {
            Log.e(TAG, "Attempted to save null tokens: access=${accessToken != null}, refresh=${refreshToken != null}")
        }
    }
    
    override suspend fun clearAuthTokens() {
        tokenManager.clearTokens()
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