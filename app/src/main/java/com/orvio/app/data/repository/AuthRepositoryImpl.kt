package com.orvio.app.data.repository

import com.orvio.app.data.local.TokenManager
import com.orvio.app.data.remote.api.AuthApiService
import com.orvio.app.data.remote.dto.OtpSendRequestDto
import com.orvio.app.data.remote.dto.OtpVerifyRequestDto
import com.orvio.app.data.remote.dto.RefreshTokenRequestDto
import com.orvio.app.domain.model.AuthResponse
import com.orvio.app.domain.model.OtpSendResponse
import com.orvio.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) : AuthRepository {
    
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
            // Save tokens on successful verification
            saveAuthTokens(response.accessToken, response.refreshToken)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun refreshToken(refreshToken: String): Result<AuthResponse> {
        return try {
            val response = authApiService.refreshToken(RefreshTokenRequestDto(refreshToken))
            // Save tokens on successful refresh
            saveAuthTokens(response.accessToken, response.refreshToken)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun saveAuthTokens(accessToken: String, refreshToken: String) {
        tokenManager.saveTokens(accessToken, refreshToken)
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