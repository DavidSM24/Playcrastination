package com.playcrastination.data.steam

import com.playcrastination.domain.model.SteamFailure
import com.playcrastination.domain.model.SteamProblem
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class SteamRemoteDataSourceTest {
    private val credentials = SteamCredentials("76561198000000001", "test-placeholder")
    private val jsonFormat = Json { ignoreUnknownKeys = true }

    @Test
    fun readsOwnedGamesAndSendsTheKeyInTheHeaderOnly() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(json("""{"response":{"game_count":2,"games":[{"appid":20,"name":"Alpha","playtime_forever":0},{"appid":21,"name":"Beta"}]}}"""))
            val result = remote(server).owned(credentials)
            assertEquals(listOf(20L, 21L), result.map { it.appId })
            assertEquals(0, result[0].playedMinutes)
            assertNull(result[1].playedMinutes)
            val request = server.takeRequest()
            assertEquals("/IPlayerService/GetOwnedGames/v1/", request.requestUrl?.encodedPath)
            assertEquals(credentials.apiKey, request.getHeader("x-webapi-key"))
            assertFalse(request.path.orEmpty().contains(credentials.apiKey))
            assertTrue(request.requestUrl?.queryParameter("input_json").orEmpty().contains(credentials.steamId))
        }
    }

    @Test
    fun distinguishesPrivacyFromAVisibleEmptyLibrary() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(json("""{"response":{}}"""))
            server.enqueue(json("""{"response":{"game_count":0}}"""))
            val remote = remote(server)
            assertEquals(SteamProblem.LIBRARY_PRIVATE, failure { remote.owned(credentials) })
            assertTrue(remote.owned(credentials).isEmpty())
        }
    }

    @Test
    fun retriesServerErrorsAndDoesNotRetryDeniedAccess() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(503))
            server.enqueue(json("""{"response":{"total_count":0}}"""))
            server.enqueue(MockResponse().setResponseCode(403))
            val remote = remote(server)
            assertTrue(remote.recent(credentials).isEmpty())
            assertEquals(SteamProblem.KEY_REJECTED, failure { remote.profile(credentials) })
            assertEquals(3, server.requestCount)
        }
    }

    @Test
    fun holdsFurtherCallsAfterRateLimiting() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "30"))
            val remote = remote(server)
            assertEquals(SteamProblem.RATE_LIMITED, failure { remote.owned(credentials) })
            assertEquals(SteamProblem.RATE_LIMITED, failure { remote.recent(credentials) })
            assertEquals(1, server.requestCount)
        }
    }

    @Test
    fun mapsAnAuthorizedProfileRecentPlayAndAchievements() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(json("""{"response":{"players":[{"steamid":"76561198000000001","personaname":"Player","avatarfull":"https://cdn.example.test/avatar.png","gameextrainfo":"Sample"}]}}"""))
            server.enqueue(json("""{"response":{"total_count":1,"games":[{"appid":20,"name":"Sample","playtime_forever":80,"playtime_2weeks":12,"rtime_last_played":1795680000}]}}"""))
            server.enqueue(json("""{"playerstats":{"success":true,"achievements":[{"achieved":1},{"achieved":0}]}}"""))
            val remote = remote(server)
            assertEquals("Sample", remote.profile(credentials).currentGame)
            val recent = remote.recent(credentials).single()
            assertEquals(12, recent.recentMinutes)
            assertEquals(1_795_680_000L, recent.lastPlayedSeconds)
            val achievements = remote.achievements(credentials, 20)
            assertEquals(1, achievements.unlocked)
            assertEquals(2, achievements.total)
        }
    }

    @Test
    fun mapsPublicStoreMetadataAndTreatsForbiddenStoreAsUnavailable() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(json("""{"20":{"success":true,"data":{"developers":["Developer"],"publishers":["Publisher"],"genres":[{"description":"Adventure"}],"platforms":{"windows":true,"linux":true},"release_date":{"coming_soon":false,"date":"24 Apr, 2025"},"header_image":"https://cdn.example.test/header.jpg"}}}"""))
            server.enqueue(MockResponse().setResponseCode(403))
            val store = SteamStoreMetadata(retrofit(server).create(SteamStoreApi::class.java))
            val metadata = store.fetch(20)
            assertEquals("Developer", metadata.developer)
            assertEquals("Windows, Linux", metadata.platforms)
            assertEquals("2025-04-24", metadata.releaseDate.toString())
            assertNull(server.takeRequest().getHeader("x-webapi-key"))
            assertEquals(SteamProblem.UNAVAILABLE, failure { store.fetch(20) })
        }
    }

    private fun remote(server: MockWebServer): SteamRemoteDataSource =
        SteamRemoteDataSource(retrofit(server).create(SteamApi::class.java))

    private fun retrofit(server: MockWebServer): Retrofit =
        Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(jsonFormat.asConverterFactory("application/json".toMediaType()))
            .build()

    private fun json(body: String) = MockResponse().setHeader("Content-Type", "application/json").setBody(body)

    private suspend fun failure(call: suspend () -> Any): SteamProblem {
        return try {
            call()
            error("Expected a Steam failure")
        } catch (failure: SteamFailure) {
            failure.problem
        }
    }
}
