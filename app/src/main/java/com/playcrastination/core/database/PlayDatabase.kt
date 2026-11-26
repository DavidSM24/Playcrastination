package com.playcrastination.core.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Transaction
    @Query("SELECT * FROM games ORDER BY title COLLATE NOCASE, id")
    fun observeLibrary(): Flow<List<GameRecord>>

    @Transaction
    @Query("SELECT * FROM games WHERE id = :id")
    fun observeGame(id: Long): Flow<GameRecord?>

    @Query("SELECT createdAt FROM games WHERE id = :id")
    suspend fun creationTime(id: Long): Long?

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun findGame(id: Long): GameEntity?

    @Insert
    suspend fun insertGame(game: GameEntity): Long

    @Update
    suspend fun updateGame(game: GameEntity)

    @Query("SELECT * FROM playthroughs WHERE gameId = :gameId ORDER BY id DESC LIMIT 1")
    suspend fun currentPlaythrough(gameId: Long): PlaythroughEntity?

    @Query("SELECT * FROM playthroughs WHERE id = :id AND gameId = :gameId")
    suspend fun findPlaythrough(gameId: Long, id: Long): PlaythroughEntity?

    @Insert
    suspend fun insertPlaythrough(playthrough: PlaythroughEntity)

    @Update
    suspend fun updatePlaythrough(playthrough: PlaythroughEntity)

    @Upsert
    suspend fun writeDurations(durations: GameDurationEntity)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteGame(id: Long)

    @Query("SELECT COALESCE(MAX(position), 0) + 1 FROM backlog_entries")
    suspend fun nextPosition(): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBacklog(entry: BacklogEntryEntity)

    @Query("DELETE FROM backlog_entries WHERE gameId = :id")
    suspend fun removeBacklog(id: Long)

    @Query("SELECT gameId FROM backlog_entries ORDER BY position")
    suspend fun backlogOrder(): List<Long>

    @Query("SELECT EXISTS(SELECT 1 FROM backlog_entries WHERE gameId = :id)")
    suspend fun isQueued(id: Long): Boolean

    @Query("DELETE FROM backlog_entries")
    suspend fun clearBacklog()

    @Insert
    suspend fun insertBacklogOrder(entries: List<BacklogEntryEntity>)

    @Query("SELECT status FROM games WHERE id = :id")
    suspend fun gameStatus(id: Long): String?

    @Query("UPDATE games SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, updatedAt: Long)

    @Query("UPDATE games SET priority = :priority, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePriority(id: Long, priority: String, updatedAt: Long): Int

    @Query("UPDATE games SET goal = :goal, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateGoal(id: Long, goal: String, updatedAt: Long): Int
}

@Database(
    entities = [GameEntity::class, PlaythroughEntity::class, GameDurationEntity::class,
        BacklogEntryEntity::class, SteamLinkEntity::class, SteamProfileEntity::class,
        SteamPlaytimeSnapshot::class],
    version = 3,
    exportSchema = true
)
abstract class PlayDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun steamDao(): SteamDao
}
