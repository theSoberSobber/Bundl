package com.bundl.app.domain.usecase.auth

import com.bundl.app.domain.repository.AuthRepository
import com.bundl.app.domain.usecase.base.UseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CheckLoginStatusUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : UseCase<Boolean> {
    
    override suspend fun invoke(): Result<Boolean> {
        return try {
            val isLoggedIn = authRepository.isLoggedIn().first()
            Result.success(isLoggedIn)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun asFlow(): Flow<Boolean> = authRepository.isLoggedIn()
}
