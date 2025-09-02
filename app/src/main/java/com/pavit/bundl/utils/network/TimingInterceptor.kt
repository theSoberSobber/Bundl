package com.pavit.bundl.utils.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class TimingInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.nanoTime()
        
        Log.d("BundlAPI", "╔═══════ Request Started ═══════")
        Log.d("BundlAPI", "║ ${request.method} ${request.url}")
        
        val response = chain.proceed(request)
        val endTime = System.nanoTime()
        
        val duration = (endTime - startTime) / 1_000_000 // Convert to milliseconds
        Log.d("BundlAPI", "╔═══════ Request Completed ═══════")
        Log.d("BundlAPI", "║ Duration: ${duration}ms")
        Log.d("BundlAPI", "║ Status: ${response.code}")
        Log.d("BundlAPI", "╚════════════════════════════════")
        
        return response
    }
} 