package com.bundl.app.domain.usecase.auth

import com.bundl.app.domain.usecase.base.UseCase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetFcmTokenUseCase @Inject constructor() : UseCase<String> {
    
    override suspend fun invoke(): Result<String> {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            Result.success(token ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
