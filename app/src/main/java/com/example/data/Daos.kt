package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloaderDao {
    // --- Download Logs ---
    @Query("SELECT * FROM download_logs ORDER BY timestamp DESC")
    fun getAllDownloadLogs(): Flow<List<DownloadLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadLog(log: DownloadLog) : Long

    @Query("DELETE FROM download_logs")
    suspend fun clearAllDownloadLogs()

    // --- Telegram Users ---
    @Query("SELECT * FROM telegram_users ORDER BY joinDate DESC")
    fun getAllTelegramUsers(): Flow<List<TelegramUser>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelegramUser(user: TelegramUser)

    @Update
    suspend fun updateTelegramUser(user: TelegramUser)

    @Query("UPDATE telegram_users SET isBanned = :isBanned WHERE id = :userId")
    suspend fun updateBanStatus(userId: Long, isBanned: Boolean)

    @Query("DELETE FROM telegram_users")
    suspend fun clearAllTelegramUsers()

    // --- System Proxies ---
    @Query("SELECT * FROM system_proxies ORDER BY country ASC")
    fun getAllProxies(): Flow<List<ProxyStatus>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProxy(proxy: ProxyStatus)

    @Query("UPDATE system_proxies SET isOnline = :isOnline, speedMs = :latencyMs WHERE id = :proxyId")
    suspend fun updateProxyStatus(proxyId: Int, isOnline: Boolean, latencyMs: Int)

    // --- System Settings ---
    @Query("SELECT * FROM system_settings")
    fun getAllSettings(): Flow<List<SystemSetting>>

    @Query("SELECT * FROM system_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingByKey(key: String): SystemSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SystemSetting)
}
