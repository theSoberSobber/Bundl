package com.bundl.app.domain.usecase.auth

import com.bundl.app.domain.repository.AuthRepository
import com.bundl.app.domain.repository.DeviceRepository
import com.bundl.app.domain.usecase.base.ParameterizedUseCase
import javax.inject.Inject

data class VerifyOtpParams(
    val tid: String,
    val otp: String
)

class VerifyOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository
) : ParameterizedUseCase<VerifyOtpParams, Boolean> {
    
    override suspend fun invoke(params: VerifyOtpParams): Result<Boolean> {
        return deviceRepository.getFcmToken().fold(
            onSuccess = { fcmToken ->
                authRepository.verifyOtp(params.tid, params.otp, fcmToken).map { 
                    true // Return success indicator
                }
            },
            onFailure = { error ->
                Result.failure(Exception("Failed to get device token: ${error.message}", error))
            }
        )
    }
}
