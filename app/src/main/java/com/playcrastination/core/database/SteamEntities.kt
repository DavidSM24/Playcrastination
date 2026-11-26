package com.playcrastination.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "steam_links",
    foreignKeys = [ForeignKey(
        entity = GameEntity::class, parentColumns = ["id"], childColumns = ["gameId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["steamId", "appId"], unique = true)]
)
data class SteamLinkEntity(
    @PrimaryKey val gameId: Long,
    val steamId: String,
    val appId: Long,
    val playedMinutes: Int?,
    val recentMinutes: Int?,
    val lastPlayedSeconds: Long?,
    val syncedAtMillis: Long,
    val hidden: Boolean = false,
    val metadataAtMillis: Long? = null,
    val unlocked: Int? = null,
    val achievementCount: Int? = null
)

@Entity(tableName = "steam_profiles")
data class SteamProfileEntity(
    @PrimaryKey val steamId: String,
    val name: String,
    val avatarUrl: String?,
    val currentGame: String?,
    val lastSyncMillis: Long?
)

@Entity(
    tableName = "steam_playtime_snapshots",
    foreignKeys = [ForeignKey(
        entity = SteamLinkEntity::class, parentColumns = ["gameId"], childColumns = ["gameId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["gameId", "observedAtMillis"])]
)
data class SteamPlaytimeSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val observedAtMillis: Long,
    val cumulativeMinutes: Int
)

@Dao
interface SteamDao {
    @Query("SELECT * FROM steam_profiles WHERE steamId = :steamId")
    fun observeProfile(steamId: String): Flow<SteamProfileEntity?>

    @Query("SELECT * FROM steam_profiles WHERE steamId = :steamId")
    suspend fun profile(steamId: String): SteamProfileEntity?

    @Upsert
    suspend fun writeProfile(profile: SteamProfileEntity)

    @Query("SELECT * FROM steam_links WHERE steamId = :steamId AND appId = :appId")
    suspend fun link(steamId: String, appId: Long): SteamLinkEntity?

    @Query("SELECT * FROM steam_links WHERE steamId = :steamId")
    suspend fun links(steamId: String): List<SteamLinkEntity>

    @Query("SELECT * FROM steam_links WHERE gameId = :gameId")
    suspend fun linkForGame(gameId: Long): SteamLinkEntity?

    @Upsert
    suspend fun writeLink(link: SteamLinkEntity)

    @Query("UPDATE steam_links SET hidden = :hidden WHERE gameId = :gameId")
    suspend fun setHidden(gameId: Long, hidden: Boolean)

    @Insert
    suspend fun insertSnapshot(snapshot: SteamPlaytimeSnapshot)

    @Query("SELECT * FROM steam_playtime_snapshots WHERE gameId = :gameId ORDER BY id")
    suspend fun snapshots(gameId: Long): List<SteamPlaytimeSnapshot>
}
