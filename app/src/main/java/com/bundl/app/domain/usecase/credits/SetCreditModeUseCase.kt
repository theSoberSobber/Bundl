package com.bundl.app.domain.usecase.credits

import com.bundl.app.domain.repository.ApiKeyRepository
import com.bundl.app.domain.usecase.base.VoidParameterizedUseCase
import javax.inject.Inject

data class SetCreditModeParams(
    val mode: String
)

class SetCreditModeUseCase @Inject constructor(
    private val apiKeyRepository: ApiKeyRepository
) : VoidParameterizedUseCase<SetCreditModeParams> {
    
    override suspend fun invoke(parameters: SetCreditModeParams): Result<Unit> {
        return apiKeyRepository.setCreditMode(parameters.mode)
    }
}
