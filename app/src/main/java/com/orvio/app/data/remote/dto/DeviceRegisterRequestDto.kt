package com.orvio.app.data.remote.dto

data class DeviceRegisterRequestDto(
    val fcmToken: String,
    val phoneNumber: String
) 