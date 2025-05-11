package com.bundl.app.data.remote.dto

data class OtpVerifyRequestDto(
    val tid: String,
    val otp: String,
    val fcmToken: String
) 