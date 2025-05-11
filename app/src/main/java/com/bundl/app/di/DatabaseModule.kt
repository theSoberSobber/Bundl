package com.bundl.app.di

import android.content.Context
import androidx.room.Room
import com.bundl.app.data.local.OrderDatabase
import com.bundl.app.data.local.OrderDao
import com.bundl.app.data.local.MIGRATION_1_2
import com.bundl.app.data.local.MIGRATION_2_3
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideOrderDatabase(
        @ApplicationContext context: Context
    ): OrderDatabase {
        return Room.databaseBuilder(
            context,
            OrderDatabase::class.java,
            "orders.db"
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()
    }

    @Provides
    @Singleton
    fun provideOrderDao(database: OrderDatabase): OrderDao {
        return database.orderDao
    }
} 