package com.pavit.bundl.domain.usecase.auth

import com.pavit.bundl.domain.repository.AuthRepository
import com.pavit.bundl.domain.usecase.base.VoidUseCase
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
