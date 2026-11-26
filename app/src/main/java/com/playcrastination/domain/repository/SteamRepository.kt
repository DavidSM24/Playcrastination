package com.playcrastination.domain.repository

import com.playcrastination.domain.model.SteamConnection
import com.playcrastination.domain.model.SteamLibrary
import kotlinx.coroutines.flow.Flow

interface SteamRepository {
    val connection: Flow<SteamConnection>
    suspend fun saveConnection(steamId: String, apiKey: String)
    suspend fun disconnect()
    suspend fun validate(): Int
    suspend fun setAutomatic(enabled: Boolean)
    suspend fun setInterval(minutes: Int)
    suspend fun preview(recentOnly: Boolean): SteamLibrary
    suspend fun import(library: SteamLibrary, appIds: Set<Long>, progress: (Int, Int) -> Unit): Int
    suspend fun sync(ifStale: Boolean = false)
    suspend fun setHidden(gameId: Long, hidden: Boolean)
    suspend fun refreshMetadata(gameId: Long)
    suspend fun refreshAchievements(gameId: Long)
}
