package com.orvio.app.data.remote.dto

data class OtpVerifyRequestDto(
    val transactionId: String,
    val userInputOtp: String
) 