package com.bundl.app.data.remote.api

import com.bundl.app.data.remote.dto.ApiKeyDto
import com.bundl.app.data.remote.dto.CreditModeResponse
import com.bundl.app.data.remote.dto.CreditPackageResponse
import com.bundl.app.data.remote.dto.CreditsResponse
import com.bundl.app.data.remote.dto.OtpSendRequestDto
import com.bundl.app.data.remote.dto.OtpVerifyRequestDto
import com.bundl.app.domain.payment.OrderResponse
import com.bundl.app.domain.payment.VerifyResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
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
    
    @GET("/credits/balance")
    suspend fun getCredits(): CreditsResponse
    
    @GET("/service/creditMode")
    suspend fun getCreditMode(): CreditModeResponse
    
    @PATCH("/service/creditMode")
    suspend fun setCreditMode(@Body request: Map<String, String>): Map<String, Boolean>
    
    @GET("/credits/packages")
    suspend fun getCreditPackages(): CreditPackageResponse
    
    @POST("/credits/purchase")
    suspend fun purchaseCredits(@Body request: Map<String, String>): CreditsResponse
    
    @POST("/credits/order")
    suspend fun createCreditOrder(@Body request: Map<String, String>): OrderResponse
    
    @POST("/credits/verify")
    suspend fun verifyCreditOrder(@Body request: Map<String, String>): VerifyResponse
} 