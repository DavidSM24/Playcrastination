package com.playcrastination.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.playcrastination.core.database.PlayDatabase
import com.playcrastination.data.repository.RoomGameRepository
import com.playcrastination.domain.model.Durations
import com.playcrastination.domain.model.Game
import com.playcrastination.domain.model.GameStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

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
            playedMinutes = 1080,
            narrativePercent = 35,
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
        assertEquals(35, edited.narrativePercent)

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
                val gameId = writer.save(Game(title = "Saved game", playedMinutes = 0, durations = Durations(1800)))
                writer.setBacklog(gameId, true)
                gameId
            } finally {
                storedDatabase.close()
            }
            val reopened = Room.databaseBuilder(context, PlayDatabase::class.java, filename).build()
            try {
                val stored = checkNotNull(RoomGameRepository(reopened).observeGame(id).first())
                assertEquals("Saved game", stored.title)
                assertEquals(0, stored.playedMinutes)
                assertEquals(1800, stored.timeProgress().remainingMinutes)
                assertEquals(1L, stored.backlogPosition)
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(filename)
        }
    }
}
