package com.pavit.bundl.domain.usecase.auth

import com.pavit.bundl.domain.usecase.base.UseCase
import com.pavit.bundl.domain.repository.DeviceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetFcmTokenUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) : UseCase<String> {
    
    override suspend fun invoke(): Result<String> {
        return deviceRepository.getFcmToken()
    }
}
