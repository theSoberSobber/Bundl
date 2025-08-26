package com.bundl.app.data.remote.api

import com.bundl.app.data.remote.dto.CreditModeResponse
import com.bundl.app.data.remote.dto.CreditPackageResponse
import com.bundl.app.data.remote.dto.CreditsResponse
import com.bundl.app.domain.payment.OrderResponse
import com.bundl.app.domain.payment.VerifyResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface ApiKeyService {
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