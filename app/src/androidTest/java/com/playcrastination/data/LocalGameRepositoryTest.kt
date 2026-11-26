package com.playcrastination.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.playcrastination.core.database.PlayDatabase
import com.playcrastination.data.repository.RoomGameRepository
import com.playcrastination.domain.model.BacklogPlacement
import com.playcrastination.domain.model.CompletionGoal
import com.playcrastination.domain.model.Durations
import com.playcrastination.domain.model.Game
import com.playcrastination.domain.model.GameStatus
import com.playcrastination.domain.model.Playthrough
import com.playcrastination.domain.model.Priority
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class LocalGameRepositoryTest {
    private lateinit var database: PlayDatabase
    private lateinit var repository: RoomGameRepository

    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, PlayDatabase::class.java).build()
        repository = RoomGameRepository(database)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun savesUpdatesAndDeletesRelatedRows() = runBlocking {
        val id = repository.save(Game(
            title = "  Example  ",
            notes = "Keep this",
            status = GameStatus.PLAYING,
            currentPlaythrough = Playthrough(playedMinutes = 1080, narrativePercent = 35),
            durations = Durations(1800, null, 3900)
        ))
        val stored = checkNotNull(repository.observeGame(id).first())
        assertEquals("Example", stored.title)
        assertEquals(720, stored.timeProgress().remainingMinutes)
        assertNull(stored.durations.extrasMinutes)

        repository.setBacklog(id, true)
        repository.save(stored.copy(title = "Edited", favorite = true))
        val edited = checkNotNull(repository.observeGame(id).first())
        assertEquals(1L, edited.backlogPosition)
        assertEquals("Keep this", edited.notes)
        assertEquals(35, edited.currentPlaythrough.narrativePercent)

        repository.delete(id)
        assertNull(repository.observeGame(id).first())
        assertEquals(1L, database.gameDao().nextPosition())
    }

    @Test
    fun enqueuingConcurrentlyIsIdempotentAndKeepsExistingPositions() = runBlocking {
        val first = repository.save(Game(title = "First"))
        val second = repository.save(Game(title = "Second"))
        repository.setBacklog(first, true)
        List(8) { async { repository.setBacklog(second, true) } }.awaitAll()

        val library = repository.observeLibrary().first()
        assertEquals(listOf(1L, 2L), library.map { it.backlogPosition })
        repository.setBacklog(first, false)
        assertEquals(listOf(null, 2L), repository.observeLibrary().first().map { it.backlogPosition })
    }

    @Test
    fun reopensSavedGameAndQueueFromDisk() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val filename = "repository-test-${UUID.randomUUID()}.db"
        try {
            val storedDatabase = Room.databaseBuilder(context, PlayDatabase::class.java, filename).build()
            val id = try {
                val writer = RoomGameRepository(storedDatabase)
                val gameId = writer.save(Game(title = "Saved game", currentPlaythrough = Playthrough(playedMinutes = 0),
                    durations = Durations(1800)))
                writer.setBacklog(gameId, true)
                gameId
            } finally {
                storedDatabase.close()
            }
            val reopened = Room.databaseBuilder(context, PlayDatabase::class.java, filename).build()
            try {
                val stored = checkNotNull(RoomGameRepository(reopened).observeGame(id).first())
                assertEquals("Saved game", stored.title)
                assertEquals(0, stored.currentPlaythrough.playedMinutes)
                assertEquals(1800, stored.timeProgress().remainingMinutes)
                assertEquals(1L, stored.backlogPosition)
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(filename)
        }
    }

    @Test
    fun keepsArchiveIndependentFromCurrentProgressAndRejectsStaleEdits() = runBlocking {
        val lastPlayed = LocalDate.of(2025, 4, 24)
        val id = repository.save(Game(
            title = "First playthrough",
            originalTitle = "Original title",
            availablePlatforms = "PC; PS5",
            rating = 9,
            durations = Durations(1200),
            currentPlaythrough = Playthrough(playedMinutes = 480, narrativePercent = 80,
                lastPlayedOn = lastPlayed, storyMinutes = 300)
        ))
        val first = checkNotNull(repository.observeGame(id).first())
        repository.startNewPlaythrough(id, first.currentPlaythrough.id)
        assertTrue(runCatching { repository.save(first.copy(title = "Stale editor")) }.isFailure)
        assertTrue(runCatching { repository.startNewPlaythrough(id, first.currentPlaythrough.id) }.isFailure)

        val afterNew = checkNotNull(repository.observeGame(id).first())
        assertEquals(first.title, afterNew.title)
        assertEquals(first.currentPlaythrough, afterNew.previousPlaythroughs.single())
        assertNull(afterNew.timeProgress().percent)
        assertEquals(480L, afterNew.recordedMinutes)
        assertEquals(lastPlayed, afterNew.lastPlayedOn)
        assertEquals(listOf("PC", "PS5"), afterNew.platforms)

        repository.updateArchivedPlaythrough(id, first.currentPlaythrough.copy(playedMinutes = 600))
        repository.save(afterNew.copy(currentPlaythrough = afterNew.currentPlaythrough.copy(playedMinutes = 120)))
        val stored = checkNotNull(repository.observeGame(id).first())
        assertEquals(9, stored.rating)
        assertEquals("Original title", stored.originalTitle)
        assertEquals(720L, stored.recordedMinutes)
        assertEquals(10, stored.timeProgress().percent)
        assertEquals(80, stored.previousPlaythroughs.single().narrativePercent)
    }

    @Test
    fun reordersByAnchorDuringAnEnqueueAndRejectsAMissingAnchor() = runBlocking {
        val ids = (1..4).map { repository.save(Game(title = "Game $it")) }
        val (first, second, third, fourth) = ids
        ids.take(3).forEach { repository.setBacklog(it, true) }
        listOf(
            async { repository.moveInBacklog(third, first, BacklogPlacement.BEFORE) },
            async { repository.setBacklog(fourth, true) }
        ).awaitAll()
        repository.moveInBacklog(third, first, BacklogPlacement.BEFORE)
        assertEquals(listOf(third, first, second, fourth), database.gameDao().backlogOrder())

        repository.setBacklog(second, false)
        assertTrue(runCatching { repository.moveInBacklog(third, second, BacklogPlacement.AFTER) }.isFailure)
        assertEquals(listOf(third, first, fourth), database.gameDao().backlogOrder())
        assertEquals(4, repository.observeLibrary().first().size)
    }

    @Test
    fun startsAndPausesAtomicallyAndEditsOnlySelectedFields() = runBlocking {
        val first = repository.save(Game(title = "Queue anchor"))
        val id = repository.save(Game(
            title = "From the queue",
            notes = "Preserve this",
            favorite = true,
            rating = 8,
            durations = Durations(90, 180, 360),
            currentPlaythrough = Playthrough(playedMinutes = 30, narrativePercent = 17)
        ))
        repository.setBacklog(id, true)
        repository.setBacklog(first, true)

        repository.startQueuedGame(id)
        repository.startQueuedGame(id)
        val playing = checkNotNull(repository.observeGame(id).first())
        assertEquals(GameStatus.PLAYING, playing.status)
        assertNull(playing.backlogPosition)
        assertEquals(listOf(first), database.gameDao().backlogOrder())

        listOf(async { repository.pauseAndQueue(id) }, async { repository.pauseAndQueue(id) }).awaitAll()
        repository.setPriority(id, Priority.HIGH)
        repository.setGoal(id, CompletionGoal.COMPLETIONIST)
        val paused = checkNotNull(repository.observeGame(id).first())
        assertEquals(listOf(first, id), database.gameDao().backlogOrder())
        assertEquals(GameStatus.ON_HOLD, paused.status)
        assertEquals(Priority.HIGH, paused.priority)
        assertEquals(CompletionGoal.COMPLETIONIST, paused.goal)
        assertEquals(330, paused.timeProgress().remainingMinutes)
        assertEquals("Preserve this", paused.notes)
        assertEquals(playing.currentPlaythrough, paused.currentPlaythrough)
        assertTrue(paused.favorite)
        assertEquals(8, paused.rating)
    }

    @Test
    fun reopensSavedManualOrderFromDisk() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val filename = "backlog-test-${UUID.randomUUID()}.db"
        try {
            val stored = Room.databaseBuilder(context, PlayDatabase::class.java, filename).build()
            val expected = try {
                val writer = RoomGameRepository(stored)
                val ids = (1..3).map { writer.save(Game(title = "Game $it")) }
                ids.forEach { writer.setBacklog(it, true) }
                writer.moveInBacklog(ids.first(), ids.last(), BacklogPlacement.AFTER)
                listOf(ids[1], ids[2], ids[0])
            } finally {
                stored.close()
            }
            val reopened = Room.databaseBuilder(context, PlayDatabase::class.java, filename).build()
            try {
                assertEquals(expected, reopened.gameDao().backlogOrder())
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(filename)
        }
    }
}
