package com.playcrastination.data.repository

import androidx.room.withTransaction
import com.playcrastination.core.database.PlayDatabase
import com.playcrastination.core.database.SteamLinkEntity
import com.playcrastination.core.database.SteamPlaytimeSnapshot
import com.playcrastination.core.database.SteamProfileEntity
import com.playcrastination.data.mapper.toEntity
import com.playcrastination.data.steam.SteamConfiguration
import com.playcrastination.data.steam.SteamCredentials
import com.playcrastination.data.steam.SteamGateway
import com.playcrastination.data.steam.SteamMetadataSource
import com.playcrastination.domain.model.Game
import com.playcrastination.domain.model.Playthrough
import com.playcrastination.domain.model.SteamConnection
import com.playcrastination.domain.model.SteamFailure
import com.playcrastination.domain.model.SteamGame
import com.playcrastination.domain.model.SteamLibrary
import com.playcrastination.domain.model.SteamProblem
import com.playcrastination.domain.model.SteamProfile
import com.playcrastination.domain.repository.SteamRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun interface SteamNetwork {
    fun isAvailable(): Boolean
}

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class LocalSteamRepository @Inject constructor(
    private val database: PlayDatabase,
    private val configuration: SteamConfiguration,
    private val remote: SteamGateway,
    private val metadata: SteamMetadataSource,
    private val network: SteamNetwork
) : SteamRepository {
    private val operations = Mutex()
    private val games = database.gameDao()
    private val steam = database.steamDao()

    override val connection = configuration.settings.flatMapLatest { settings ->
        val steamId = settings.steamId
        if (steamId == null || !settings.configured) {
            flowOf(SteamConnection(settings))
        } else {
            steam.observeProfile(steamId).map { SteamConnection(settings, it?.toDomain()) }
        }
    }

    override suspend fun saveConnection(steamId: String, apiKey: String) {
        val id = steamId.trim()
        val secret = apiKey.trim()
        if (!id.matches(Regex("[1-9][0-9]{16}")) || !secret.matches(Regex("[A-Za-z0-9]{8,128}"))) {
            throw SteamFailure(SteamProblem.INVALID_INPUT)
        }
        operations.withLock { configuration.save(SteamCredentials(id, secret)) }
    }

    override suspend fun disconnect() = operations.withLock { configuration.clear() }

    override suspend fun setAutomatic(enabled: Boolean) = configuration.setAutomatic(enabled)

    override suspend fun setInterval(minutes: Int) {
        require(minutes in 15..1440)
        configuration.setInterval(minutes)
    }

    override suspend fun validate(): Int = operations.withLock {
        val credentials = connectedCredentials()
        val profile = remote.profile(credentials)
        val games = remote.owned(credentials)
        persistProfile(profile, steam.profile(credentials.steamId)?.lastSyncMillis)
        games.size
    }

    override suspend fun preview(recentOnly: Boolean): SteamLibrary = operations.withLock {
        val credentials = connectedCredentials()
        val games = if (recentOnly) remote.recent(credentials) else remote.owned(credentials)
        SteamLibrary(credentials.steamId, games.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }))
    }

    override suspend fun import(library: SteamLibrary, appIds: Set<Long>, progress: (Int, Int) -> Unit): Int =
        operations.withLock {
            val settings = configuration.settings.first()
            val configuredId = settings.steamId
            if (!settings.configured || configuredId == null || library.steamId != configuredId) {
                throw SteamFailure(SteamProblem.NOT_CONFIGURED)
            }
            val selected = library.games.filter { it.appId in appIds }.distinctBy { it.appId }
            var inserted = 0
            var completed = 0
            progress(0, selected.size)
            for (batch in selected.chunked(25)) {
                val now = System.currentTimeMillis()
                inserted += database.withTransaction {
                    var count = 0
                    for (candidate in batch) {
                        val old = steam.link(configuredId, candidate.appId)
                        if (old == null) {
                            val game = Game(title = candidate.name, platform = "Steam", coverUrl = candidate.iconUrl.orEmpty())
                            val id = games.insertGame(game.toEntity(now, now))
                            games.insertPlaythrough(Playthrough().toEntity(id))
                            val link = SteamLinkEntity(id, configuredId, candidate.appId, null, null, null, now)
                            writeObservation(link, candidate, now)
                            count++
                        } else {
                            writeObservation(old, candidate, now)
                        }
                    }
                    count
                }
                completed += batch.size
                progress(completed, selected.size)
            }
            inserted
        }

    override suspend fun sync(ifStale: Boolean) = operations.withLock {
        val settings = configuration.settings.first()
        if (ifStale) {
            if (!settings.automatic || !settings.configured) return@withLock
            val last = settings.steamId?.let { steam.profile(it)?.lastSyncMillis }
            if (last != null && System.currentTimeMillis() - last < settings.intervalMinutes * 60_000L) return@withLock
        }
        val credentials = connectedCredentials()
        val owned = remote.owned(credentials).associateBy(SteamGame::appId)
        val profile = optionalSteam { remote.profile(credentials) }
        val recent = optionalSteam { remote.recent(credentials).associateBy(SteamGame::appId) }
        val now = System.currentTimeMillis()

        database.withTransaction {
            for (link in steam.links(credentials.steamId)) {
                val observation = owned[link.appId] ?: recent?.get(link.appId) ?: continue
                val recentGame = recent?.get(link.appId)
                writeObservation(
                    link,
                    observation.copy(
                        recentMinutes = recentGame?.recentMinutes ?: observation.recentMinutes
                            ?: if (recent != null && recentGame == null) 0 else null,
                        lastPlayedSeconds = maxOf(observation.lastPlayedSeconds ?: 0L, recentGame?.lastPlayedSeconds ?: 0L)
                            .takeIf { it > 0L }
                    ),
                    now
                )
            }
            if (profile != null) {
                persistProfile(profile, now)
            } else {
                val previous = steam.profile(credentials.steamId)
                if (previous != null) steam.writeProfile(previous.copy(lastSyncMillis = now))
                else steam.writeProfile(SteamProfileEntity(credentials.steamId, "", null, null, now))
            }
        }
    }

    override suspend fun setHidden(gameId: Long, hidden: Boolean) = steam.setHidden(gameId, hidden)

    override suspend fun refreshMetadata(gameId: Long) = operations.withLock {
        val link = steam.linkForGame(gameId) ?: throw SteamFailure(SteamProblem.UNAVAILABLE)
        val now = System.currentTimeMillis()
        if (link.metadataAtMillis?.let { now - it < 86_400_000L } == true) return@withLock
        requireNetwork()
        val remote = metadata.fetch(link.appId)

        database.withTransaction {
            val stored = games.findGame(gameId) ?: return@withTransaction
            val latestLink = steam.linkForGame(gameId) ?: return@withTransaction
            val release = remote.releaseDate?.takeIf { stored.releaseYear == null && stored.releaseDay == null }
            games.updateGame(stored.copy(
                description = stored.description.ifBlank { remote.description.orEmpty() },
                developer = stored.developer?.takeUnless(String::isBlank) ?: remote.developer,
                publisher = stored.publisher?.takeUnless(String::isBlank) ?: remote.publisher,
                genre = stored.genre.ifBlank { remote.genre.orEmpty() },
                availablePlatforms = stored.availablePlatforms?.takeUnless(String::isBlank) ?: remote.platforms,
                bannerUrl = stored.bannerUrl.ifBlank { remote.headerUrl.orEmpty() },
                releaseYear = stored.releaseYear ?: release?.year,
                releaseDay = stored.releaseDay ?: release?.toEpochDay()
            ))
            steam.writeLink(latestLink.copy(metadataAtMillis = now))
        }
    }

    override suspend fun refreshAchievements(gameId: Long) = operations.withLock {
        val link = steam.linkForGame(gameId) ?: throw SteamFailure(SteamProblem.UNAVAILABLE)
        val credentials = connectedCredentials()
        if (link.steamId != credentials.steamId) throw SteamFailure(SteamProblem.NOT_CONFIGURED)
        val achievements = remote.achievements(credentials, link.appId)
        database.withTransaction {
            val latest = steam.linkForGame(gameId) ?: return@withTransaction
            steam.writeLink(latest.copy(unlocked = achievements.unlocked, achievementCount = achievements.total))
        }
    }

    private suspend fun connectedCredentials(): SteamCredentials {
        val credentials = configuration.credentials() ?: throw SteamFailure(SteamProblem.NOT_CONFIGURED)
        requireNetwork()
        return credentials
    }

    private fun requireNetwork() {
        if (!network.isAvailable()) throw SteamFailure(SteamProblem.OFFLINE)
    }

    private suspend fun persistProfile(profile: SteamProfile, syncedAt: Long?) {
        val old = steam.profile(profile.steamId)
        steam.writeProfile(SteamProfileEntity(
            profile.steamId,
            profile.name.ifBlank { old?.name.orEmpty() },
            profile.avatarUrl ?: old?.avatarUrl,
            profile.currentGame,
            syncedAt
        ))
    }

    private suspend fun writeObservation(old: SteamLinkEntity, observation: SteamGame, now: Long) {
        val observedTime = observation.playedMinutes
        steam.writeLink(old.copy(
            playedMinutes = observedTime ?: old.playedMinutes,
            recentMinutes = observation.recentMinutes ?: old.recentMinutes,
            lastPlayedSeconds = maxOf(old.lastPlayedSeconds ?: 0, observation.lastPlayedSeconds ?: 0)
                .takeIf { it > 0 },
            syncedAtMillis = now
        ))
        if (observedTime != null && observedTime != old.playedMinutes) {
            steam.insertSnapshot(SteamPlaytimeSnapshot(gameId = old.gameId, observedAtMillis = now,
                cumulativeMinutes = observedTime))
        }
    }
}

private fun SteamProfileEntity.toDomain() = SteamProfile(steamId, name, avatarUrl, currentGame, lastSyncMillis)

private suspend fun <T> optionalSteam(operation: suspend () -> T): T? = try {
    operation()
} catch (_: SteamFailure) {
    null
}
