package com.bundl.app.domain.usecase.auth

import com.bundl.app.domain.repository.AuthRepository
import com.bundl.app.domain.usecase.base.VoidUseCase
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : VoidUseCase {
    
    override suspend fun invoke(): Result<Unit> {
        return try {
            authRepository.logout()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
