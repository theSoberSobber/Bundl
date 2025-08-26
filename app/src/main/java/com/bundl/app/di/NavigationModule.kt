package com.bundl.app.di

import com.bundl.app.data.repository.NavigationRepositoryImpl
import com.bundl.app.domain.repository.NavigationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NavigationModule {
    
    @Binds
    @Singleton
    abstract fun bindNavigationRepository(
        navigationRepositoryImpl: NavigationRepositoryImpl
    ): NavigationRepository
}
