package com.bundl.app.domain.repository

import com.bundl.app.domain.model.AuthResponse
import com.bundl.app.domain.model.OtpSendResponse
import com.bundl.app.domain.model.UserStats
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun sendOtp(phoneNumber: String): Result<OtpSendResponse>
    suspend fun verifyOtp(tid: String, otp: String, fcmToken: String): Result<AuthResponse>
    suspend fun refreshToken(refreshToken: String): Result<AuthResponse>
    suspend fun getUserStats(): Result<UserStats>
    
    suspend fun saveAuthTokens(accessToken: String?, refreshToken: String?)
    suspend fun clearAuthTokens()
    suspend fun logout()
    
    fun getAccessToken(): Flow<String?>
    fun getRefreshToken(): Flow<String?>
    fun isLoggedIn(): Flow<Boolean>
} 