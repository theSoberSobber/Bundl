package com.pavit.bundl.data.remote.api

import com.pavit.bundl.data.remote.dto.OtpSendRequestDto
import com.pavit.bundl.data.remote.dto.OtpVerifyRequestDto
import com.pavit.bundl.data.remote.dto.RefreshTokenRequestDto
import com.pavit.bundl.domain.model.AuthResponse
import com.pavit.bundl.domain.model.OtpSendResponse
import com.pavit.bundl.domain.model.UserStats
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {
    @POST("/auth/sendOtp")
    suspend fun sendOtp(@Body request: OtpSendRequestDto): OtpSendResponse
    
    @POST("/auth/verifyOtp")
    suspend fun verifyOtp(@Body request: OtpVerifyRequestDto): AuthResponse
    
    @POST("/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequestDto): AuthResponse
    
    @POST("/auth/updateFcmToken")
    suspend fun updateFcmToken(@Body request: Map<String, String>): Map<String, Any>
    
    @GET("/auth/stats")
    suspend fun getUserStats(): UserStats
} 