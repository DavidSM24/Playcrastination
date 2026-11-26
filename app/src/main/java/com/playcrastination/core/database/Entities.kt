package com.playcrastination.core.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "games", indices = [Index("title")])
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val releaseYear: Int?,
    val genre: String,
    val platform: String,
    val coverUrl: String,
    val bannerUrl: String,
    val description: String,
    val notes: String,
    val status: String,
    val priority: String,
    val goal: String,
    val favorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "game_progress",
    foreignKeys = [ForeignKey(
        entity = GameEntity::class, parentColumns = ["id"], childColumns = ["gameId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class GameProgressEntity(
    @PrimaryKey val gameId: Long,
    val playedMinutes: Int?,
    val narrativePercent: Int?
)

@Entity(
    tableName = "game_durations",
    foreignKeys = [ForeignKey(
        entity = GameEntity::class, parentColumns = ["id"], childColumns = ["gameId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class GameDurationEntity(
    @PrimaryKey val gameId: Long,
    val mainMinutes: Int?,
    val extrasMinutes: Int?,
    val completionistMinutes: Int?
)

@Entity(
    tableName = "backlog_entries",
    foreignKeys = [ForeignKey(
        entity = GameEntity::class, parentColumns = ["id"], childColumns = ["gameId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["position"], unique = true)]
)
data class BacklogEntryEntity(@PrimaryKey val gameId: Long, val position: Long)

data class GameRecord(
    @Embedded val game: GameEntity,
    @Relation(parentColumn = "id", entityColumn = "gameId") val progress: GameProgressEntity?,
    @Relation(parentColumn = "id", entityColumn = "gameId") val durations: GameDurationEntity?,
    @Relation(parentColumn = "id", entityColumn = "gameId") val backlog: BacklogEntryEntity?
)
