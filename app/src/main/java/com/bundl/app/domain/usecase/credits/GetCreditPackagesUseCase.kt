package com.bundl.app.domain.usecase.credits

import com.bundl.app.domain.model.CreditPackage
import com.bundl.app.domain.repository.CreditsRepository
import com.bundl.app.domain.usecase.base.UseCase
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
