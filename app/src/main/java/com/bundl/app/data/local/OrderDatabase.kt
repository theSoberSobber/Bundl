package com.bundl.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE active_orders ADD COLUMN phoneNumberMap TEXT")
        database.execSQL("ALTER TABLE active_orders ADD COLUMN note TEXT")
    }
}

@Database(
    entities = [OrderEntity::class],
    version = 2
)
@TypeConverters(Converters::class)
abstract class OrderDatabase : RoomDatabase() {
    abstract val orderDao: OrderDao
} 