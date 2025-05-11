package com.bundl.app.domain.model

data class User(
    val phoneNumber: String,
    val accessToken: String,
    val refreshToken: String,
    val credits: Int = 0,
    val cashbackPoints: Int = 0,
    val creditMode: String = "moderate"
) 