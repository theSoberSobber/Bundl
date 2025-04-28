package com.orvio.app.data.remote.api

import com.orvio.app.data.remote.dto.ApiKeyDto
import com.orvio.app.data.remote.dto.OtpSendRequestDto
import com.orvio.app.data.remote.dto.OtpVerifyRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiKeyService {
    @GET("/auth/apiKey/getAll")
    suspend fun getApiKeys(): List<ApiKeyDto>
    
    @POST("/auth/apiKey/createNew")
    suspend fun createApiKey(@Body request: Map<String, String>): String
    
    @POST("/auth/apiKey/revoke")
    suspend fun revokeApiKey(@Body request: Map<String, String>): Any
    
    @POST("/service/sendOtp")
    suspend fun serviceSendOtp(@Body request: OtpSendRequestDto): Any
    
    @POST("/service/verifyOtp")
    suspend fun serviceVerifyOtp(@Body request: OtpVerifyRequestDto): Any
    
    @POST("/service/ack")
    suspend fun serviceAck(@Body request: Map<String, String>): Any
} 