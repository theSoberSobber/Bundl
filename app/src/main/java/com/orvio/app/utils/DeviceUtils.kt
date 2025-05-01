package com.orvio.app.utils

import android.content.Context
import android.provider.Settings
import android.telephony.TelephonyManager
import android.Manifest
import android.content.pm.PackageManager
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
    
    fun getPhoneNumber(): String? {
        // Check if we have the required permissions
        if (!hasRequiredPermissions()) {
            return null
        }
        
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return try {
            // Try to get the phone number
            val phoneNumber = telephonyManager.line1Number
            // Remove country code if present and clean up the number
            phoneNumber?.let { cleanPhoneNumber(it) }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun cleanPhoneNumber(number: String): String {
        // Remove all non-digit characters
        val digitsOnly = number.filter { it.isDigit() }
        
        // Remove country code if present
        return when {
            digitsOnly.startsWith("91") && digitsOnly.length > 10 -> digitsOnly.substring(2)
            digitsOnly.startsWith("+91") && digitsOnly.length > 10 -> digitsOnly.substring(3)
            else -> digitsOnly
        }
    }
    
    private fun hasRequiredPermissions(): Boolean {
        return context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
               context.checkSelfPermission(Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun generateSHA256Hash(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
} 