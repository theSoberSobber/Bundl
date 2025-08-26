package com.bundl.app.domain.usecase.auth

import com.bundl.app.domain.model.UserStats
import com.bundl.app.domain.repository.AuthRepository
import com.bundl.app.domain.usecase.base.UseCase
import javax.inject.Inject

class GetUserStatsUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : UseCase<UserStats> {
    
    override suspend fun invoke(): Result<UserStats> {
        return authRepository.getUserStats()
    }
}
