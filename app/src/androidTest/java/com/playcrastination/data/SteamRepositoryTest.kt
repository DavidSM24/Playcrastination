package com.playcrastination.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.playcrastination.core.database.PlayDatabase
import com.playcrastination.data.repository.LocalSteamRepository
import com.playcrastination.data.repository.RoomGameRepository
import com.playcrastination.data.repository.SteamNetwork
import com.playcrastination.data.steam.SteamConfiguration
import com.playcrastination.data.steam.SteamCredentials
import com.playcrastination.data.steam.SteamGateway
import com.playcrastination.data.steam.SteamMetadataSource
import com.playcrastination.domain.model.CompletionGoal
import com.playcrastination.domain.model.GameStatus
import com.playcrastination.domain.model.SteamAchievements
import com.playcrastination.domain.model.SteamFailure
import com.playcrastination.domain.model.SteamGame
import com.playcrastination.domain.model.SteamLibrary
import com.playcrastination.domain.model.SteamMetadata
import com.playcrastination.domain.model.SteamProblem
import com.playcrastination.domain.model.SteamProfile
import com.playcrastination.domain.model.SteamSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class SteamRepositoryTest {
    private val account = "76561198000000001"
    private lateinit var database: PlayDatabase
    private lateinit var library: RoomGameRepository
    private lateinit var steam: LocalSteamRepository
    private lateinit var gateway: FakeSteamGateway
    private var connected = true

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, PlayDatabase::class.java).build()
        library = RoomGameRepository(database)
        gateway = FakeSteamGateway()
        steam = LocalSteamRepository(database, FakeConfiguration(SteamCredentials(account, "placeholder")),
            gateway, object : SteamMetadataSource {
                override suspend fun fetch(appId: Long) = SteamMetadata("Store text", "Developer", "Publisher", "RPG",
                    "Windows", LocalDate.of(2025, 4, 24), null)
            }, SteamNetwork { connected })
    }

    @After
    fun close() = database.close()

    @Test
    fun importsOnlySelectedGamesAndKeepsObservedPlaytimeSeparate() = runBlocking {
        val preview = SteamLibrary(account, listOf(game(20, 100), game(21, null)))
        assertEquals(1, steam.import(preview, setOf(20)) { _, _ -> })
        assertEquals(0, steam.import(preview, setOf(20)) { _, _ -> })
        val original = library.observeLibrary().first().single()
        assertNull(original.currentPlaythrough.playedMinutes)
        assertNull(original.backlogPosition)

        library.save(original.copy(
            notes = "Keep my decision", goal = CompletionGoal.COMPLETIONIST, favorite = true,
            status = GameStatus.ON_HOLD, currentPlaythrough = original.currentPlaythrough.copy(playedMinutes = 30)
        ))
        library.setBacklog(original.id, true)
        steam.setHidden(original.id, true)

        gateway.games = listOf(game(20, 120), game(21, 3))
        steam.sync()
        steam.sync()
        val updated = library.observeLibrary().first().single()
        assertEquals("Keep my decision", updated.notes)
        assertEquals(CompletionGoal.COMPLETIONIST, updated.goal)
        assertEquals(GameStatus.ON_HOLD, updated.status)
        assertEquals(1L, updated.backlogPosition)
        assertTrue(updated.favorite)
        assertTrue(checkNotNull(updated.steam).hidden)
        assertEquals(30, updated.currentPlaythrough.playedMinutes)
        assertEquals(120, updated.steam?.playedMinutes)
        assertEquals(listOf(100, 120), database.steamDao().snapshots(updated.id).map { it.cumulativeMinutes })
    }

    @Test
    fun commitsCompleteBatchesAndCanResumeAnImportAfterCancellation() = runBlocking {
        val preview = SteamLibrary(account, (1L..28L).map { game(it, 0) })
        val selection = preview.games.mapTo(mutableSetOf()) { it.appId }
        try {
            steam.import(preview, selection) { completed, _ ->
                if (completed == 25) throw CancellationException("stop test import")
            }
            error("Expected cancellation")
        } catch (_: CancellationException) {
            assertEquals(25, library.observeLibrary().first().size)
        }
        assertEquals(3, steam.import(preview, selection) { _, _ -> })
        assertEquals(28, library.observeLibrary().first().size)
    }

    @Test
    fun preservesManualMetadataAndKeepsCachedValuesAfterAnOfflineFailure() = runBlocking {
        steam.import(SteamLibrary(account, listOf(game(20, 100))), setOf(20)) { _, _ -> }
        val original = library.observeLibrary().first().single()
        library.save(original.copy(description = "Manual description", releaseYear = 2024))
        steam.refreshMetadata(original.id)
        steam.refreshAchievements(original.id)
        connected = false
        try {
            steam.sync()
            error("Expected offline error")
        } catch (failure: SteamFailure) {
            assertEquals(SteamProblem.OFFLINE, failure.problem)
        }

        val after = library.observeLibrary().first().single()
        assertEquals("Manual description", after.description)
        assertEquals(2024, after.releaseYear)
        assertNull(after.releaseDate)
        assertEquals("Developer", after.developer)
        assertEquals(100, after.steam?.playedMinutes)
        assertEquals(SteamAchievements(1, 2), after.steam?.achievements)
        assertFalse(checkNotNull(after.steam).hidden)
    }

    private fun game(id: Long, minutes: Int?) = SteamGame(id, "Game $id", minutes)

    private class FakeConfiguration(initial: SteamCredentials) : SteamConfiguration {
        private var credentials: SteamCredentials? = initial
        override val settings = MutableStateFlow(SteamSettings(initial.steamId, true))
        override suspend fun credentials() = credentials
        override suspend fun save(credentials: SteamCredentials) {
            this.credentials = credentials
            settings.value = settings.value.copy(steamId = credentials.steamId, hasKey = true)
        }
        override suspend fun clear() {
            credentials = null
            settings.value = settings.value.copy(steamId = null, hasKey = false)
        }
        override suspend fun setAutomatic(enabled: Boolean) { settings.value = settings.value.copy(automatic = enabled) }
        override suspend fun setInterval(minutes: Int) { settings.value = settings.value.copy(intervalMinutes = minutes) }
    }

    private class FakeSteamGateway : SteamGateway {
        var games = emptyList<SteamGame>()
        override suspend fun owned(credentials: SteamCredentials) = games
        override suspend fun recent(credentials: SteamCredentials) = emptyList<SteamGame>()
        override suspend fun profile(credentials: SteamCredentials) = SteamProfile(credentials.steamId, "Player", null, null)
        override suspend fun achievements(credentials: SteamCredentials, appId: Long) = SteamAchievements(1, 2)
    }
}
