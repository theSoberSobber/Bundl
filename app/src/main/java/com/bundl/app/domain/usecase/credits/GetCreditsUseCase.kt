package com.bundl.app.domain.usecase.credits

import com.bundl.app.domain.repository.ApiKeyRepository
import com.bundl.app.domain.usecase.base.UseCase
import javax.inject.Inject

class GetCreditsUseCase @Inject constructor(
    private val apiKeyRepository: ApiKeyRepository
) : UseCase<com.bundl.app.domain.repository.CreditsInfo> {
    
    override suspend fun invoke(): Result<com.bundl.app.domain.repository.CreditsInfo> {
        return apiKeyRepository.getCredits()
    }
}
