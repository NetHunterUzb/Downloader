package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DownloadLog::class,
        TelegramUser::class,
        ProxyStatus::class,
        SystemSetting::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DownloaderDatabase : RoomDatabase() {
    abstract fun downloaderDao(): DownloaderDao

    companion object {
        @Volatile
        private var INSTANCE: DownloaderDatabase? = null

        fun getDatabase(context: Context): DownloaderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DownloaderDatabase::class.java,
                    "downloader_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
