package com.pavit.bundl.di

import android.content.Context
import com.pavit.bundl.domain.payment.RevenueCatManager
import com.pavit.bundl.domain.payment.RevenueCatService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PaymentModule {

    @Provides
    @Singleton
    fun provideRevenueCatService(
        @ApplicationContext context: Context
    ): RevenueCatService {
        return RevenueCatService(context)
    }

    @Provides
    @Singleton
    fun provideRevenueCatManager(
        revenueCatService: RevenueCatService
    ): RevenueCatManager {
        return RevenueCatManager(revenueCatService)
    }
}
