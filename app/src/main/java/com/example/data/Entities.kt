package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_logs")
data class DownloadLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val platform: String,
    val resolution: String,
    val format: String,
    val sizeMb: Double,
    val status: String, // "Completed", "Failed", "Processing"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "telegram_users")
data class TelegramUser(
    @PrimaryKey val id: Long, // Telegram User ID
    val username: String,
    val joinDate: String,
    val downloadedCount: Int,
    val isBanned: Boolean = false
)

@Entity(tableName = "system_proxies")
data class ProxyStatus(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val address: String,
    val country: String,
    val speedMs: Int,
    val isOnline: Boolean = true
)

@Entity(tableName = "system_settings")
data class SystemSetting(
    @PrimaryKey val key: String,
    val value: String
)
