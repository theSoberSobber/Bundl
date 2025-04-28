package com.orvio.app.data.remote.dto

import com.orvio.app.domain.model.ApiKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ApiKeyDto(
    val id: String,
    val name: String,
    val createdAt: String,
    val lastUsed: String?,
    val session: SessionDto
) {
    fun toApiKey(): ApiKey {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        return ApiKey(
            id = id,
            name = name,
            key = session.refreshToken,
            createdAt = dateFormat.parse(createdAt) ?: Date(),
            lastUsed = lastUsed?.let { dateFormat.parse(it) }
        )
    }
}

data class SessionDto(
    val id: String,
    val refreshToken: String
) 