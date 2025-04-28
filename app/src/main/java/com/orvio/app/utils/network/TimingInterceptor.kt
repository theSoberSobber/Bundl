package com.orvio.app.utils.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class TimingInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.nanoTime()
        
        Log.d("OrvioAPI", "╔═══════ Request Started ═══════")
        Log.d("OrvioAPI", "║ ${request.method} ${request.url}")
        
        val response = chain.proceed(request)
        val endTime = System.nanoTime()
        
        val duration = (endTime - startTime) / 1_000_000 // Convert to milliseconds
        Log.d("OrvioAPI", "╔═══════ Request Completed ═══════")
        Log.d("OrvioAPI", "║ Duration: ${duration}ms")
        Log.d("OrvioAPI", "║ Status: ${response.code}")
        Log.d("OrvioAPI", "╚════════════════════════════════")
        
        return response
    }
} 