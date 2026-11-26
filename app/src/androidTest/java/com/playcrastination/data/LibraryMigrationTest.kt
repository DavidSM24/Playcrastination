package com.playcrastination.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.playcrastination.core.database.Migration1To2
import com.playcrastination.core.database.Migration2To3
import com.playcrastination.core.database.PlayDatabase
import com.playcrastination.data.repository.RoomGameRepository
import com.playcrastination.domain.model.CompletionGoal
import com.playcrastination.domain.model.Durations
import com.playcrastination.domain.model.GameStatus
import com.playcrastination.domain.model.Priority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class LibraryMigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(instrumentation, PlayDatabase::class.java)

    @Test
    fun movesProgressIntoCurrentPlaythroughWithoutLosingAnyPersonalFields() = runBlocking {
        val filename = "migration-test-${UUID.randomUUID()}.db"
        val context = instrumentation.targetContext
        try {
            helper.createDatabase(filename, 1).use { legacy ->
                legacy.execSQL(
                    """
                    INSERT INTO games (id, title, releaseYear, genre, platform, coverUrl, bannerUrl,
                        description, notes, status, priority, goal, favorite, createdAt, updatedAt)
                    VALUES (7, 'Migrated game', 2025, 'RPG; Action', 'PC', '', '', 'Summary',
                        'Keep my notes', 'PLAYING', 'HIGH', 'MAIN_AND_EXTRAS', 1, 123, 456),
                       (8, 'No progress row', NULL, '', '', '', '', '',
                        '', 'NOT_STARTED', 'NORMAL', 'MAIN_STORY', 0, 321, 654)
                    """.trimIndent()
                )
                legacy.execSQL("INSERT INTO game_progress VALUES (7, 1080, 42)")
                legacy.execSQL("INSERT INTO game_durations VALUES (7, 1800, 2700, 3900)")
                legacy.execSQL("INSERT INTO backlog_entries VALUES (7, 5)")
            }

            helper.runMigrationsAndValidate(filename, 3, true, Migration1To2, Migration2To3).close()
            val migrated = Room.databaseBuilder(context, PlayDatabase::class.java, filename)
                .addMigrations(Migration1To2, Migration2To3)
                .build()
            try {
                val repository = RoomGameRepository(migrated)
                val game = checkNotNull(repository.observeGame(7).first())
                assertEquals("Migrated game", game.title)
                assertEquals(2025, game.releaseYear)
                assertEquals(listOf("RPG", "Action"), game.genres)
                assertEquals("PC", game.platform)
                assertEquals("Keep my notes", game.notes)
                assertEquals(GameStatus.PLAYING, game.status)
                assertEquals(Priority.HIGH, game.priority)
                assertEquals(CompletionGoal.MAIN_AND_EXTRAS, game.goal)
                assertTrue(game.favorite)
                assertEquals(5L, game.backlogPosition)
                assertEquals(1080, game.currentPlaythrough.playedMinutes)
                assertEquals(42, game.currentPlaythrough.narrativePercent)
                assertEquals(Durations(1800, 2700, 3900), game.durations)
                assertNull(game.rating)
                assertNull(game.steam)
                assertTrue(game.previousPlaythroughs.isEmpty())

                val noProgress = checkNotNull(repository.observeGame(8).first())
                assertTrue(noProgress.currentPlaythrough.id > 0)
                assertNull(noProgress.currentPlaythrough.playedMinutes)
            } finally {
                migrated.close()
            }
        } finally {
            context.deleteDatabase(filename)
        }
    }
}
