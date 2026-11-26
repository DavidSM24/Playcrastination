package com.playcrastination.domain.repository

import com.playcrastination.domain.model.Game
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    fun observeLibrary(): Flow<List<Game>>
    fun observeGame(id: Long): Flow<Game?>
    suspend fun save(game: Game): Long
    suspend fun delete(id: Long)
    suspend fun setBacklog(id: Long, inBacklog: Boolean)
}
