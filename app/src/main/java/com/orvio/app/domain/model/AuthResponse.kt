package com.orvio.app.domain.model

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String
) 