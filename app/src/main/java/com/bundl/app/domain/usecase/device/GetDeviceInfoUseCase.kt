package com.bundl.app.domain.usecase.device

import com.bundl.app.domain.repository.DeviceRepository
import com.bundl.app.domain.usecase.base.UseCase
import javax.inject.Inject

class GetDeviceInfoUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) : UseCase<com.bundl.app.domain.repository.DeviceInfo> {
    
    override suspend fun invoke(): Result<com.bundl.app.domain.repository.DeviceInfo> {
        return deviceRepository.getDeviceInfo()
    }
}
