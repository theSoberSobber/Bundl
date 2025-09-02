package com.pavit.bundl.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.text.SimpleDateFormat
import java.util.*

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE active_orders ADD COLUMN phoneNumberMap TEXT")
        database.execSQL("ALTER TABLE active_orders ADD COLUMN note TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val defaultDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        database.execSQL("ALTER TABLE active_orders ADD COLUMN createdAt TEXT NOT NULL DEFAULT '$defaultDate'")
    }
}

@Database(
    entities = [OrderEntity::class],
    version = 3
)
@TypeConverters(Converters::class)
abstract class OrderDatabase : RoomDatabase() {
    abstract val orderDao: OrderDao
} 