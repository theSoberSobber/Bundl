package com.bundl.app.domain.repository

import kotlinx.coroutines.flow.Flow

data class CreditsInfo(
    val credits: Int
)

interface CreditsRepository {
    suspend fun getCredits(): Result<CreditsInfo>
    suspend fun setCreditMode(mode: String): Result<Unit>
} 