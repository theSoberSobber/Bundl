package com.pavit.bundl.data.remote.api

import com.pavit.bundl.data.remote.dto.CreditModeResponse
import com.pavit.bundl.data.remote.dto.CreditPackageResponse
import com.pavit.bundl.data.remote.dto.CreditsResponse
import com.pavit.bundl.domain.payment.OrderResponse
import com.pavit.bundl.domain.payment.VerifyResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface CreditsService {
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