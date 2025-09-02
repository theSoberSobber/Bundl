package com.pavit.bundl.domain.usecase.auth

import com.pavit.bundl.domain.repository.AuthRepository
import com.pavit.bundl.domain.usecase.base.ParameterizedUseCase
import javax.inject.Inject

data class SendOtpParams(
    val phoneNumber: String
)

class SendOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : ParameterizedUseCase<SendOtpParams, String> {
    
    override suspend fun invoke(params: SendOtpParams): Result<String> {
        return authRepository.sendOtp(params.phoneNumber).map { response ->
            response.tid
        }
    }
}
