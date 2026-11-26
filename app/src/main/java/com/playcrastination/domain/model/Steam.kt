package com.playcrastination.domain.model

import java.time.LocalDate

data class SteamGame(
    val appId: Long,
    val name: String,
    val playedMinutes: Int?,
    val recentMinutes: Int? = null,
    val lastPlayedSeconds: Long? = null,
    val iconUrl: String? = null
)

data class SteamLink(
    val steamId: String,
    val appId: Long,
    val playedMinutes: Int?,
    val recentMinutes: Int?,
    val lastPlayedSeconds: Long?,
    val hidden: Boolean,
    val achievements: SteamAchievements?
) {
    val storeUrl: String get() = "https://store.steampowered.com/app/$appId/"
}

data class SteamAchievements(val unlocked: Int, val total: Int)

data class SteamProfile(
    val steamId: String,
    val name: String,
    val avatarUrl: String?,
    val currentGame: String?,
    val lastSyncMillis: Long? = null
)

data class SteamSettings(
    val steamId: String? = null,
    val hasKey: Boolean = false,
    val automatic: Boolean = true,
    val intervalMinutes: Int = 30
) {
    val configured: Boolean get() = steamId != null && hasKey
}

data class SteamConnection(val settings: SteamSettings = SteamSettings(), val profile: SteamProfile? = null)

data class SteamLibrary(val steamId: String, val games: List<SteamGame>)

data class SteamMetadata(
    val description: String?,
    val developer: String?,
    val publisher: String?,
    val genre: String?,
    val platforms: String?,
    val releaseDate: LocalDate?,
    val headerUrl: String?
)

enum class SteamProblem(val retryable: Boolean = false) {
    NOT_CONFIGURED, INVALID_INPUT, KEY_REJECTED, LIBRARY_PRIVATE, PROFILE_MISSING,
    UNAVAILABLE, STORAGE, OFFLINE(true), RATE_LIMITED(true), TEMPORARY(true)
}

class SteamFailure(val problem: SteamProblem) : Exception(problem.name)
