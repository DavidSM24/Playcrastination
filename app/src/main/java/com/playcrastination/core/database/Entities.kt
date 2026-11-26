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
    val updatedAt: Long,
    val originalTitle: String?,
    val releaseDay: Long?,
    val developer: String?,
    val publisher: String?,
    val availablePlatforms: String?,
    val backgroundUrl: String?,
    val rating: Int?
)

@Entity(
    tableName = "playthroughs",
    foreignKeys = [ForeignKey(
        entity = GameEntity::class, parentColumns = ["id"], childColumns = ["gameId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["gameId", "id"])]
)
data class PlaythroughEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val playedMinutes: Int?,
    val narrativePercent: Int?,
    val startedDay: Long?,
    val finishedDay: Long?,
    val lastPlayedDay: Long?,
    val storyMinutes: Int?,
    val extrasMinutes: Int?,
    val otherMinutes: Int?
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
    @Relation(parentColumn = "id", entityColumn = "gameId") val playthroughs: List<PlaythroughEntity>,
    @Relation(parentColumn = "id", entityColumn = "gameId") val durations: GameDurationEntity?,
    @Relation(parentColumn = "id", entityColumn = "gameId") val backlog: BacklogEntryEntity?,
    @Relation(parentColumn = "id", entityColumn = "gameId") val steam: SteamLinkEntity?
)
