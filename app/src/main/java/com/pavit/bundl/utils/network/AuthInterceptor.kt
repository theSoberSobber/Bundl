package com.pavit.bundl.utils.network

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.pavit.bundl.data.local.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    
    companion object {
        private const val TAG = "AuthInterceptor"
        private var requestCounter = 0
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestId = ++requestCounter
        
        Log.d(TAG, "[$requestId] Intercepting request: ${request.method} ${request.url}")
        
        // Skip token for authentication endpoints
        val skipAuth = request.url.toString().contains("/auth/sendOtp") || 
                       request.url.toString().contains("/auth/verifyOtp")
        
        if (skipAuth) {
            Log.d(TAG, "[$requestId] Skipping auth for login endpoint")
            return chain.proceed(request)
        }
        
        val token = runBlocking { tokenManager.getAccessToken().first() }
        val isLoggedIn = runBlocking { tokenManager.isLoggedIn().first() }
        
        Log.d(TAG, "[$requestId] Auth status: logged in = $isLoggedIn, token ${if (token != null) "present" else "missing"}")
        
        if (token != null) {
            Log.d(TAG, "[$requestId] Using token: ${token.take(10)}...")
        }
        
        val newRequest = request.newBuilder()
        
        token?.let {
            newRequest.addHeader("Authorization", "Bearer $it")
            Log.d(TAG, "[$requestId] Added Authorization header with token")
        } ?: run {
            Log.w(TAG, "[$requestId] No access token available for request")
        }
        
        val response = chain.proceed(newRequest.build())
        
        Log.d(TAG, "[$requestId] Received response: ${response.code} for ${request.method} ${request.url}")
        
        if (response.code == 401) {
            Log.e(TAG, "[$requestId] Received 401 Unauthorized for request: ${request.method} ${request.url}")
            Log.e(TAG, "[$requestId] Request headers: ${request.headers}")
            Log.e(TAG, "[$requestId] Response headers: ${response.headers}")
            
            // Mark all 401 responses as requiring authentication
            // This will trigger our AuthAuthenticator for this response
            return response.newBuilder()
                .code(401)
                .message("Unauthorized - Will trigger token refresh")
                .build()
        }
        
        // Handle 403 Forbidden - similar to unauthorized but typically means token is invalid or expired beyond refresh
        if (response.code == 403) {
            Log.e(TAG, "[$requestId] Received 403 Forbidden for request: ${request.method} ${request.url}")
            
            // Implement logout directly to avoid dependency cycle
            runBlocking {
                // Check if tokens exist before attempting to sign out
                val refreshToken = tokenManager.getRefreshToken().first()
                
                if (refreshToken != null) {
                    Log.d(TAG, "[$requestId] Logging out due to 403 error")
                    
                    try {
                        // Delete FCM token first
                        FirebaseMessaging.getInstance().deleteToken().await()
                        Log.d(TAG, "[$requestId] FCM token deleted successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "[$requestId] Failed to delete FCM token", e)
                    }
                    
                    // Then clear auth tokens
                    tokenManager.clearTokens()
                    Log.d(TAG, "[$requestId] Auth tokens cleared")
                } else {
                    Log.d(TAG, "[$requestId] Skipping logout - tokens already cleared")
                }
            }
        }
        
        return response
    }
} 