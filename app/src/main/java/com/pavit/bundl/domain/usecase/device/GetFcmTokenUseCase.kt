package com.pavit.bundl.domain.usecase.device

import com.pavit.bundl.domain.repository.DeviceRepository
import com.pavit.bundl.domain.usecase.base.UseCase
import javax.inject.Inject

class GetFcmTokenUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) : UseCase<String> {
    
    override suspend fun invoke(): Result<String> {
        return deviceRepository.getFcmToken()
    }
}
