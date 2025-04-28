package com.orvio.app.domain.model

data class User(
    val phoneNumber: String,
    val accessToken: String,
    val refreshToken: String
) 