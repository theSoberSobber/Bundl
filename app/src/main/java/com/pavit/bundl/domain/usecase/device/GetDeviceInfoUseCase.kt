package com.pavit.bundl.domain.usecase.device

import com.pavit.bundl.domain.repository.DeviceRepository
import com.pavit.bundl.domain.usecase.base.UseCase
import javax.inject.Inject

class GetDeviceInfoUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) : UseCase<com.pavit.bundl.domain.repository.DeviceInfo> {
    
    override suspend fun invoke(): Result<com.pavit.bundl.domain.repository.DeviceInfo> {
        return deviceRepository.getDeviceInfo()
    }
}
