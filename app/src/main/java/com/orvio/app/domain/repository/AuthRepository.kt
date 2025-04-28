package com.orvio.app.domain.repository

import com.orvio.app.domain.model.AuthResponse
import com.orvio.app.domain.model.OtpSendResponse
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun sendOtp(phoneNumber: String): Result<OtpSendResponse>
    suspend fun verifyOtp(transactionId: String, otp: String): Result<AuthResponse>
    suspend fun refreshToken(refreshToken: String): Result<AuthResponse>
    
    suspend fun saveAuthTokens(accessToken: String, refreshToken: String)
    suspend fun clearAuthTokens()
    
    fun getAccessToken(): Flow<String?>
    fun getRefreshToken(): Flow<String?>
    fun isLoggedIn(): Flow<Boolean>
} 