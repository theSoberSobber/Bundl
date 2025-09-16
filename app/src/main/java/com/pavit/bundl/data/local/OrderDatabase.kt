package com.pavit.bundl.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pavit.bundl.data.local.entity.ChatMessageEntity
import com.pavit.bundl.data.local.entity.ChatRoomEntity
import com.pavit.bundl.data.local.entity.ChatMessageConverters
import com.pavit.bundl.data.local.entity.ChatRoomConverters
import com.pavit.bundl.data.local.dao.ChatMessageDao
import com.pavit.bundl.data.local.dao.ChatRoomDao
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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create chat_rooms table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS chat_rooms (
                id TEXT NOT NULL PRIMARY KEY,
                orderId TEXT NOT NULL,
                participants TEXT NOT NULL,
                lastMessage TEXT,
                lastMessageTime INTEGER,
                unreadCount INTEGER NOT NULL DEFAULT 0,
                isActive INTEGER NOT NULL DEFAULT 1,
                lastUpdated INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}
            )
        """)
        
        // Create chat_messages table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS chat_messages (
                id TEXT NOT NULL PRIMARY KEY,
                orderId TEXT NOT NULL,
                senderId TEXT NOT NULL,
                senderName TEXT,
                content TEXT NOT NULL,
                messageType TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                deliveryStatus TEXT NOT NULL,
                isSystemMessage INTEGER NOT NULL DEFAULT 0,
                localTimestamp INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}
            )
        """)
        
        // Create indices for better performance
        database.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_orderId ON chat_messages(orderId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_timestamp ON chat_messages(timestamp)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_chat_rooms_orderId ON chat_rooms(orderId)")
    }
}

@Database(
    entities = [OrderEntity::class, ChatMessageEntity::class, ChatRoomEntity::class],
    version = 4
)
@TypeConverters(Converters::class, ChatMessageConverters::class, ChatRoomConverters::class)
abstract class OrderDatabase : RoomDatabase() {
    abstract val orderDao: OrderDao
    abstract val chatMessageDao: ChatMessageDao
    abstract val chatRoomDao: ChatRoomDao
} 