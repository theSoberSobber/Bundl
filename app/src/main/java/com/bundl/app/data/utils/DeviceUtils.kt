package com.bundl.app.data.utils

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest
import javax.inject.Inject

class DeviceUtils @Inject constructor(
    private val context: Context
) {
    fun getDeviceHash(): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        
        return generateSHA256Hash(androidId)
    }
    
    private fun generateSHA256Hash(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
} 