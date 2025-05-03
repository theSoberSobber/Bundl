package com.orvio.app.data.remote.api

import com.orvio.app.data.remote.dto.DeviceRegisterRequestDto
import com.orvio.app.data.remote.dto.OtpSendRequestDto
import com.orvio.app.data.remote.dto.OtpVerifyRequestDto
import com.orvio.app.data.remote.dto.RefreshTokenRequestDto
import com.orvio.app.domain.model.AuthResponse
import com.orvio.app.domain.model.OtpSendResponse
import com.orvio.app.domain.model.UserStats
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
    
    @POST("/auth/register")
    suspend fun registerDevice(@Body request: DeviceRegisterRequestDto): Any
    
    @GET("/auth/stats")
    suspend fun getUserStats(): UserStats
} 