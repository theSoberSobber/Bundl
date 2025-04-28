package com.orvio.app.data.remote.dto

data class DeviceRegisterRequestDto(
    val deviceHash: String,
    val fcmToken: String
) 