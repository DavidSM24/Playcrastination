package com.playcrastination.data.repository

import androidx.room.withTransaction
import com.playcrastination.core.database.BacklogEntryEntity
import com.playcrastination.core.database.GameDurationEntity
import com.playcrastination.core.database.GameProgressEntity
import com.playcrastination.core.database.PlayDatabase
import com.playcrastination.data.mapper.toDomain
import com.playcrastination.data.mapper.toEntity
import com.playcrastination.domain.model.Game
import com.playcrastination.domain.repository.GameRepository
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomGameRepository @Inject constructor(private val database: PlayDatabase) : GameRepository {
    private val dao = database.gameDao()

    override fun observeLibrary() = dao.observeLibrary().map { records ->
        records.map { it.toDomain() }
    }

    override fun observeGame(id: Long) = dao.observeGame(id).map { it?.toDomain() }

    override suspend fun save(game: Game): Long = database.withTransaction {
        require(game.title.isNotBlank())
        val now = System.currentTimeMillis()
        val id = if (game.id == 0L) {
            dao.insertGame(game.toEntity(now, now))
        } else {
            val createdAt = checkNotNull(dao.creationTime(game.id))
            dao.updateGame(game.toEntity(createdAt, now))
            game.id
        }
        dao.writeProgress(GameProgressEntity(id, game.playedMinutes, game.narrativePercent))
        val durations = game.durations
        dao.writeDurations(GameDurationEntity(id, durations.mainMinutes, durations.extrasMinutes, durations.completionistMinutes))
        id
    }

    override suspend fun delete(id: Long) = dao.deleteGame(id)

    override suspend fun setBacklog(id: Long, inBacklog: Boolean) {
        database.withTransaction {
            if (inBacklog && dao.creationTime(id) != null) {
                dao.insertBacklog(BacklogEntryEntity(id, dao.nextPosition()))
            } else if (!inBacklog) {
                dao.removeBacklog(id)
            }
        }
    }
}
