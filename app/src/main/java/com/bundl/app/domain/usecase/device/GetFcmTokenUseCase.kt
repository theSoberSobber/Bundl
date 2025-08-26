package com.bundl.app.domain.usecase.device

import com.bundl.app.domain.repository.DeviceRepository
import com.bundl.app.domain.usecase.base.UseCase
import javax.inject.Inject

class GetFcmTokenUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) : UseCase<String> {
    
    override suspend fun invoke(): Result<String> {
        return deviceRepository.getFcmToken()
    }
}
