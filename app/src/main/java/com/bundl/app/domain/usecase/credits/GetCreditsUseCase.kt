package com.bundl.app.domain.usecase.credits

import com.bundl.app.domain.repository.CreditsRepository
import com.bundl.app.domain.usecase.base.UseCase
import javax.inject.Inject

class GetCreditsUseCase @Inject constructor(
    private val creditsRepository: CreditsRepository
) : UseCase<com.bundl.app.domain.repository.CreditsInfo> {
    
    override suspend fun invoke(): Result<com.bundl.app.domain.repository.CreditsInfo> {
        return creditsRepository.getCredits()
    }
}
