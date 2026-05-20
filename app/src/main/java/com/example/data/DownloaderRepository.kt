package com.example.data

import kotlinx.coroutines.flow.Flow

class DownloaderRepository(private val dao: DownloaderDao) {

    val downloadLogs: Flow<List<DownloadLog>> = dao.getAllDownloadLogs()
    
    val telegramUsers: Flow<List<TelegramUser>> = dao.getAllTelegramUsers()
    
    val systemProxies: Flow<List<ProxyStatus>> = dao.getAllProxies()
    
    val systemSettings: Flow<List<SystemSetting>> = dao.getAllSettings()

    suspend fun addDownloadLog(log: DownloadLog): Long {
        return dao.insertDownloadLog(log)
    }

    suspend fun clearDownloads() {
        dao.clearAllDownloadLogs()
    }

    suspend fun addTelegramUser(user: TelegramUser) {
        dao.insertTelegramUser(user)
    }

    suspend fun updateBanStatus(userId: Long, isBanned: Boolean) {
        dao.updateBanStatus(userId, isBanned)
    }

    suspend fun clearUsers() {
        dao.clearAllTelegramUsers()
    }

    suspend fun addProxy(proxy: ProxyStatus) {
        dao.insertProxy(proxy)
    }

    suspend fun updateProxy(proxyId: Int, isOnline: Boolean, speedMs: Int) {
        dao.updateProxyStatus(proxyId, isOnline, speedMs)
    }

    suspend fun getSetting(key: String, defaultValue: String): String {
        return dao.getSettingByKey(key)?.value ?: defaultValue
    }

    suspend fun saveSetting(key: String, value: String) {
        dao.insertSetting(SystemSetting(key, value))
    }
}
