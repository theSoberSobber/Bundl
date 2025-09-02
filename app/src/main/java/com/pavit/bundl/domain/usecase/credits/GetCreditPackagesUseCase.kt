package com.pavit.bundl.domain.usecase.credits

import com.pavit.bundl.domain.model.CreditPackage
import com.pavit.bundl.domain.repository.CreditsRepository
import com.pavit.bundl.domain.usecase.base.UseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetCreditPackagesUseCase @Inject constructor(
    private val creditsRepository: CreditsRepository
) : UseCase<List<CreditPackage>> {
    
    override suspend fun invoke(): Result<List<CreditPackage>> {
        return creditsRepository.getCreditPackages()
    }
}
