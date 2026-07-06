package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        ServiceRequestEntity::class,
        PostEntity::class,
        OfferEntity::class,
        MessageEntity::class,
        NotificationEntity::class,
        JobEntity::class,
        ChannelMessageEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class SouqDatabase : RoomDatabase() {
    abstract fun souqDao(): SouqDao

    companion object {
        @Volatile
        private var INSTANCE: SouqDatabase? = null

        fun getDatabase(context: Context): SouqDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SouqDatabase::class.java,
                    "souq_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
