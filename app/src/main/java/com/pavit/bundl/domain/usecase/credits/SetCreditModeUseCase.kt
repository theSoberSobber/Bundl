package com.pavit.bundl.domain.usecase.credits

import com.pavit.bundl.domain.repository.CreditsRepository
import com.pavit.bundl.domain.usecase.base.VoidParameterizedUseCase
import javax.inject.Inject

data class SetCreditModeParams(
    val mode: String
)

class SetCreditModeUseCase @Inject constructor(
    private val creditsRepository: CreditsRepository
) : VoidParameterizedUseCase<SetCreditModeParams> {
    
    override suspend fun invoke(parameters: SetCreditModeParams): Result<Unit> {
        return creditsRepository.setCreditMode(parameters.mode)
    }
}
