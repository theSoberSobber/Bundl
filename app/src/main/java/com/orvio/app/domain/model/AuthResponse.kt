package com.orvio.app.domain.model

data class AuthResponse(
    val accessToken: String?,
    val refreshToken: String?
) {
    // Helper method to check if the response contains valid tokens
    fun hasValidTokens(): Boolean {
        return !accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty()
    }
} 