package com.playcrastination.domain.repository

import com.playcrastination.domain.model.Game
import com.playcrastination.domain.model.BacklogPlacement
import com.playcrastination.domain.model.CompletionGoal
import com.playcrastination.domain.model.Playthrough
import com.playcrastination.domain.model.Priority
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    fun observeLibrary(): Flow<List<Game>>
    fun observeGame(id: Long): Flow<Game?>
    suspend fun save(game: Game): Long
    suspend fun delete(id: Long)
    suspend fun setBacklog(id: Long, inBacklog: Boolean)
    suspend fun moveInBacklog(gameId: Long, anchorId: Long, placement: BacklogPlacement)
    suspend fun startQueuedGame(id: Long)
    suspend fun pauseAndQueue(id: Long)
    suspend fun setPriority(id: Long, priority: Priority)
    suspend fun setGoal(id: Long, goal: CompletionGoal)
    suspend fun startNewPlaythrough(gameId: Long, currentId: Long)
    suspend fun updateArchivedPlaythrough(gameId: Long, playthrough: Playthrough)
}
