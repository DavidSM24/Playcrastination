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

    @Insert
    suspend fun insertGame(game: GameEntity): Long

    @Update
    suspend fun updateGame(game: GameEntity)

    @Upsert
    suspend fun writeProgress(progress: GameProgressEntity)

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
}

@Database(
    entities = [GameEntity::class, GameProgressEntity::class, GameDurationEntity::class,
        BacklogEntryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class PlayDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}
