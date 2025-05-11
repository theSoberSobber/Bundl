package com.bundl.app.domain.model

import com.google.gson.annotations.SerializedName

data class UserStats(
    val provider: ProviderStats,
    val consumer: ConsumerStats,
    val credits: CreditStats
)

data class ProviderStats(
    val currentDevice: CurrentDeviceStats?,
    val allDevices: AllDevicesStats
)

data class CurrentDeviceStats(
    val deviceId: String,
    val failedToSendAck: Int,
    val sentAckNotVerified: Int,
    val sentAckVerified: Int,
    val active: Boolean
)

data class AllDevicesStats(
    val failedToSendAck: Int,
    val sentAckNotVerified: Int,
    val sentAckVerified: Int,
    val totalMessagesSent: Int,
    val messageSentSuccessfully: Int,
    val messageTried: Int,
    val totalDevices: Int,
    val activeDevices: Int
)

data class ConsumerStats(
    val aggregate: AggregateKeyStats,
    val keys: List<KeyStats>
)

data class AggregateKeyStats(
    val totalKeys: Int,
    val activeKeys: Int,
    val oldestKey: Long,
    val newestKey: Long,
    val lastUsedKey: Long
)

data class KeyStats(
    val name: String,
    val createdAt: String,
    val lastUsed: String?,
    val refreshToken: String
)

data class CreditStats(
    val balance: Int,
    val mode: String,
    val cashbackPoints: Int = 0
) 