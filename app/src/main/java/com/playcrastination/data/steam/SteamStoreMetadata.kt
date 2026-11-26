package com.playcrastination.data.steam

import androidx.core.text.HtmlCompat
import com.playcrastination.domain.model.SteamFailure
import com.playcrastination.domain.model.SteamMetadata
import com.playcrastination.domain.model.SteamProblem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SteamMetadataSource {
    suspend fun fetch(appId: Long): SteamMetadata
}

internal interface SteamStoreApi {
    // Public Store endpoint, separate from the documented Steam Web API.
    @GET("api/appdetails")
    suspend fun details(@Query("appids") appId: Long, @Query("l") language: String = "english"):
        Response<Map<String, SteamStoreEnvelope>>
}

@Singleton
internal class SteamStoreMetadata @Inject constructor(private val api: SteamStoreApi) : SteamMetadataSource {
    private val http = SteamHttp()

    override suspend fun fetch(appId: Long): SteamMetadata {
        val item = try {
            http.request { api.details(appId) }[appId.toString()]
        } catch (failure: SteamFailure) {
            if (failure.problem != SteamProblem.KEY_REJECTED) throw failure
            throw SteamFailure(SteamProblem.UNAVAILABLE)
        }
        val data = item?.data?.takeIf { item.success }
            ?: throw SteamFailure(SteamProblem.UNAVAILABLE)
        return SteamMetadata(
            description = data.description?.let {
                HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim().takeIf(String::isNotEmpty)
            },
            developer = data.developers.asLabel(),
            publisher = data.publishers.asLabel(),
            genre = data.genres.map { it.description }.asLabel(),
            platforms = buildList {
                if (data.platforms?.windows == true) add("Windows")
                if (data.platforms?.mac == true) add("macOS")
                if (data.platforms?.linux == true) add("Linux")
            }.asLabel(),
            releaseDate = data.release?.takeUnless { it.comingSoon }?.date?.parseStoreDate(),
            headerUrl = data.header.httpsUrl()
        )
    }
}

@Serializable
internal data class SteamStoreEnvelope(val success: Boolean = false, val data: SteamStoreDto? = null)

@Serializable
internal data class SteamStoreDto(
    @SerialName("short_description") val description: String? = null,
    @SerialName("header_image") val header: String? = null,
    val developers: List<String> = emptyList(),
    val publishers: List<String> = emptyList(),
    val genres: List<SteamStoreGenre> = emptyList(),
    val platforms: SteamStorePlatforms? = null,
    @SerialName("release_date") val release: SteamStoreRelease? = null
)

@Serializable
internal data class SteamStoreGenre(val description: String = "")

@Serializable
internal data class SteamStorePlatforms(val windows: Boolean = false, val mac: Boolean = false, val linux: Boolean = false)

@Serializable
internal data class SteamStoreRelease(@SerialName("coming_soon") val comingSoon: Boolean = false, val date: String? = null)

private val dateFormats = listOf("d MMM, uuuu", "MMM d, uuuu", "d MMM uuuu")
    .map { DateTimeFormatter.ofPattern(it, Locale.ENGLISH) }

private fun String.parseStoreDate(): LocalDate? = dateFormats.firstNotNullOfOrNull { format ->
    try {
        LocalDate.parse(trim(), format)
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun List<String>.asLabel(): String? =
    map(String::trim).filter(String::isNotEmpty).distinct().joinToString().takeIf(String::isNotEmpty)
