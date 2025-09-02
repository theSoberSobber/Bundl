package com.pavit.bundl.domain.usecase.auth

import com.pavit.bundl.domain.model.UserStats
import com.pavit.bundl.domain.repository.AuthRepository
import com.pavit.bundl.domain.usecase.base.UseCase
import javax.inject.Inject

class GetUserStatsUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : UseCase<UserStats> {
    
    override suspend fun invoke(): Result<UserStats> {
        return authRepository.getUserStats()
    }
}
