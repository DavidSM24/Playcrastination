package com.playcrastination.data.steam

import com.playcrastination.domain.model.SteamAchievements
import com.playcrastination.domain.model.SteamFailure
import com.playcrastination.domain.model.SteamGame
import com.playcrastination.domain.model.SteamProblem
import com.playcrastination.domain.model.SteamProfile
import java.net.URI
import java.net.URISyntaxException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

interface SteamGateway {
    suspend fun owned(credentials: SteamCredentials): List<SteamGame>
    suspend fun recent(credentials: SteamCredentials): List<SteamGame>
    suspend fun profile(credentials: SteamCredentials): SteamProfile
    suspend fun achievements(credentials: SteamCredentials, appId: Long): SteamAchievements
}

internal interface SteamApi {
    @GET("IPlayerService/GetOwnedGames/v1/")
    suspend fun owned(@Header("x-webapi-key") key: String, @Query("input_json") input: String): Response<SteamGamesEnvelope>

    @GET("IPlayerService/GetRecentlyPlayedGames/v1/")
    suspend fun recent(@Header("x-webapi-key") key: String, @Query("input_json") input: String): Response<SteamGamesEnvelope>

    @GET("ISteamUser/GetPlayerSummaries/v2/")
    suspend fun profile(
        @Header("x-webapi-key") key: String,
        @Query("steamids") steamId: String
    ): Response<SteamProfileEnvelope>

    @GET("ISteamUserStats/GetPlayerAchievements/v1/")
    suspend fun achievements(
        @Header("x-webapi-key") key: String,
        @Query("steamid") steamId: String,
        @Query("appid") appId: Long
    ): Response<SteamAchievementEnvelope>
}

@Singleton
internal class SteamRemoteDataSource @Inject constructor(private val api: SteamApi) : SteamGateway {
    private val http = SteamHttp()

    override suspend fun owned(credentials: SteamCredentials): List<SteamGame> {
        val input = buildJsonObject {
            put("steamid", credentials.steamId.toLong())
            put("include_appinfo", true)
            put("include_played_free_games", true)
        }
        return http.request { api.owned(credentials.apiKey, input.toString()) }.response.toGames()
    }

    override suspend fun recent(credentials: SteamCredentials): List<SteamGame> {
        val input = buildJsonObject { put("steamid", credentials.steamId.toLong()) }
        return http.request { api.recent(credentials.apiKey, input.toString()) }.response.toGames()
    }

    override suspend fun profile(credentials: SteamCredentials): SteamProfile {
        val player = http.request { api.profile(credentials.apiKey, credentials.steamId) }
            .response?.players?.firstOrNull { it.steamid == credentials.steamId }
            ?: throw SteamFailure(SteamProblem.PROFILE_MISSING)
        return SteamProfile(player.steamid, player.personaname.orEmpty(),
            player.avatarfull.httpsUrl(), player.gameextrainfo?.takeIf(String::isNotBlank))
    }

    override suspend fun achievements(credentials: SteamCredentials, appId: Long): SteamAchievements {
        val result = http.request { api.achievements(credentials.apiKey, credentials.steamId, appId) }.playerstats
        val entries = result?.achievements?.takeIf { result.success == true }
            ?: throw SteamFailure(SteamProblem.UNAVAILABLE)
        return SteamAchievements(entries.count { it.achieved == 1 }, entries.size)
    }
}

@Serializable
internal data class SteamGamesEnvelope(val response: SteamGamesDto? = null)

@Serializable
internal data class SteamGamesDto(
    @SerialName("game_count") val gameCount: Int? = null,
    @SerialName("total_count") val totalCount: Int? = null,
    val games: List<SteamGameDto>? = null
)

@Serializable
internal data class SteamGameDto(
    val appid: Long,
    val name: String? = null,
    @SerialName("playtime_forever") val playedMinutes: Int? = null,
    @SerialName("playtime_2weeks") val recentMinutes: Int? = null,
    @SerialName("rtime_last_played") val lastPlayed: Long? = null,
    @SerialName("img_icon_url") val icon: String? = null
)

@Serializable
internal data class SteamProfileEnvelope(val response: SteamPlayersDto? = null)

@Serializable
internal data class SteamPlayersDto(val players: List<SteamPlayerDto> = emptyList())

@Serializable
internal data class SteamPlayerDto(
    val steamid: String,
    val personaname: String? = null,
    val avatarfull: String? = null,
    val gameextrainfo: String? = null
)

@Serializable
internal data class SteamAchievementEnvelope(val playerstats: SteamAchievementsDto? = null)

@Serializable
internal data class SteamAchievementsDto(val success: Boolean? = null, val achievements: List<SteamAchievementDto>? = null)

@Serializable
internal data class SteamAchievementDto(val achieved: Int? = null)

private fun SteamGamesDto?.toGames(): List<SteamGame> {
    val available = this?.games ?: when {
        this?.gameCount == 0 || this?.totalCount == 0 -> emptyList()
        else -> throw SteamFailure(SteamProblem.LIBRARY_PRIVATE)
    }
    return available.filter { it.appid in 1L..4_294_967_295L }.distinctBy { it.appid }.map {
        val icon = it.icon?.takeIf(SteamIconHash::matches)
        SteamGame(
            appId = it.appid,
            name = it.name?.trim()?.takeIf(String::isNotEmpty) ?: "Steam #${it.appid}",
            playedMinutes = it.playedMinutes?.takeIf { time -> time >= 0 },
            recentMinutes = it.recentMinutes?.takeIf { time -> time >= 0 },
            lastPlayedSeconds = it.lastPlayed?.takeIf { time -> time in 1L..253_402_300_799L },
            iconUrl = icon?.let { hash ->
                "https://cdn.cloudflare.steamstatic.com/steamcommunity/public/images/apps/${it.appid}/$hash.jpg"
            }
        )
    }
}

private val SteamIconHash = Regex("[a-fA-F0-9]{40}")

internal fun String?.httpsUrl(): String? {
    val value = this?.trim()?.takeIf(String::isNotEmpty) ?: return null
    return try {
        val uri = URI(value)
        value.takeIf { uri.scheme.equals("https", ignoreCase = true) && uri.host != null && uri.userInfo == null }
    } catch (_: URISyntaxException) {
        null
    }
}
