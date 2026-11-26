package com.playcrastination.data.repository

import androidx.room.withTransaction
import com.playcrastination.core.database.BacklogEntryEntity
import com.playcrastination.core.database.GameDurationEntity
import com.playcrastination.core.database.PlayDatabase
import com.playcrastination.data.mapper.toDomain
import com.playcrastination.data.mapper.toEntity
import com.playcrastination.domain.model.BacklogPlacement
import com.playcrastination.domain.model.Game
import com.playcrastination.domain.model.CompletionGoal
import com.playcrastination.domain.model.GameStatus
import com.playcrastination.domain.model.Playthrough
import com.playcrastination.domain.model.Priority
import com.playcrastination.domain.repository.GameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomGameRepository @Inject constructor(private val database: PlayDatabase) : GameRepository {
    private val dao = database.gameDao()

    override fun observeLibrary() = dao.observeLibrary().map { records ->
        records.map { it.toDomain() }
    }.flowOn(Dispatchers.Default)

    override fun observeGame(id: Long) = dao.observeGame(id).map { it?.toDomain() }

    override suspend fun save(game: Game): Long = database.withTransaction {
        require(game.title.isNotBlank())
        require(game.releaseYear == null || game.releaseYear in 1..9999)
        require(game.releaseDate == null || game.releaseDate.year in 1..9999)
        require(game.rating == null || game.rating in 1..10)
        require(game.currentPlaythrough.isValid)
        require(CompletionGoal.entries.all { goal -> game.durations[goal]?.let { it > 0 } ?: true })

        val now = System.currentTimeMillis()
        val id = if (game.id == 0L) {
            val gameId = dao.insertGame(game.toEntity(now, now))
            dao.insertPlaythrough(game.currentPlaythrough.copy(id = 0).toEntity(gameId))
            gameId
        } else {
            val createdAt = checkNotNull(dao.creationTime(game.id))
            val current = checkNotNull(dao.currentPlaythrough(game.id))
            check(current.id == game.currentPlaythrough.id)
            dao.updateGame(game.toEntity(createdAt, now))
            dao.updatePlaythrough(game.currentPlaythrough.toEntity(game.id))
            game.id
        }
        val durations = game.durations
        dao.writeDurations(GameDurationEntity(id, durations.mainMinutes, durations.extrasMinutes, durations.completionistMinutes))
        id
    }

    override suspend fun delete(id: Long) = dao.deleteGame(id)

    override suspend fun startNewPlaythrough(gameId: Long, currentId: Long) {
        database.withTransaction {
            val current = checkNotNull(dao.currentPlaythrough(gameId)).toDomain()
            check(current.id == currentId && current.hasDetails)
            dao.insertPlaythrough(Playthrough().toEntity(gameId))
        }
    }

    override suspend fun updateArchivedPlaythrough(gameId: Long, playthrough: Playthrough) {
        database.withTransaction {
            require(playthrough.isValid)
            val current = checkNotNull(dao.currentPlaythrough(gameId))
            check(current.id != playthrough.id && dao.findPlaythrough(gameId, playthrough.id) != null)
            dao.updatePlaythrough(playthrough.toEntity(gameId))
        }
    }

    override suspend fun setBacklog(id: Long, inBacklog: Boolean) {
        database.withTransaction {
            if (inBacklog && dao.creationTime(id) != null) {
                dao.insertBacklog(BacklogEntryEntity(id, dao.nextPosition()))
            } else if (!inBacklog) {
                dao.removeBacklog(id)
            }
        }
    }

    override suspend fun moveInBacklog(gameId: Long, anchorId: Long, placement: BacklogPlacement) {
        if (gameId == anchorId) return
        database.withTransaction {
            val current = dao.backlogOrder()
            val reordered = current.toMutableList()
            check(reordered.remove(gameId))
            val anchor = reordered.indexOf(anchorId)
            check(anchor >= 0)
            val destination = if (placement == BacklogPlacement.AFTER) anchor + 1 else anchor
            reordered.add(destination, gameId)
            if (reordered != current) {
                // Replace the positions together so the unique index never sees a collision.
                dao.clearBacklog()
                dao.insertBacklogOrder(reordered.mapIndexed { index, id ->
                    BacklogEntryEntity(id, index + 1L)
                })
            }
        }
    }

    override suspend fun startQueuedGame(id: Long) {
        database.withTransaction {
            val status = dao.gameStatus(id)
            if (!dao.isQueued(id)) {
                check(status == GameStatus.PLAYING.name)
                return@withTransaction
            }
            if (status != GameStatus.PLAYING.name) {
                dao.updateStatus(id, GameStatus.PLAYING.name, System.currentTimeMillis())
            }
            dao.removeBacklog(id)
        }
    }

    override suspend fun pauseAndQueue(id: Long) {
        database.withTransaction {
            when (dao.gameStatus(id)) {
                GameStatus.PLAYING.name -> dao.updateStatus(id, GameStatus.ON_HOLD.name, System.currentTimeMillis())
                GameStatus.ON_HOLD.name -> Unit
                else -> error("Game is no longer playing")
            }
            if (!dao.isQueued(id)) dao.insertBacklog(BacklogEntryEntity(id, dao.nextPosition()))
        }
    }

    override suspend fun setPriority(id: Long, priority: Priority) {
        check(dao.updatePriority(id, priority.name, System.currentTimeMillis()) == 1)
    }

    override suspend fun setGoal(id: Long, goal: CompletionGoal) {
        check(dao.updateGoal(id, goal.name, System.currentTimeMillis()) == 1)
    }
}
