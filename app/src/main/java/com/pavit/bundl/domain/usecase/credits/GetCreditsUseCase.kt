package com.pavit.bundl.domain.usecase.credits

import com.pavit.bundl.domain.repository.CreditsRepository
import com.pavit.bundl.domain.usecase.base.UseCase
import javax.inject.Inject

class GetCreditsUseCase @Inject constructor(
    private val creditsRepository: CreditsRepository
) : UseCase<com.pavit.bundl.domain.repository.CreditsInfo> {
    
    override suspend fun invoke(): Result<com.pavit.bundl.domain.repository.CreditsInfo> {
        return creditsRepository.getCredits()
    }
}
