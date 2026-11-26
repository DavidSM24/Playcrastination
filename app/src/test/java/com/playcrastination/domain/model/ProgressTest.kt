package com.playcrastination.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProgressTest {
    @Test
    fun calculatesSelectedGoalInMinutes() {
        val game = Game(
            playedMinutes = 18 * 60,
            goal = CompletionGoal.MAIN_STORY,
            durations = Durations(30 * 60, 45 * 60, 65 * 60)
        )

        assertEquals(12 * 60, game.timeProgress().remainingMinutes)
        assertEquals(60, game.timeProgress().percent)
        assertEquals(27 * 60, game.timeProgress(CompletionGoal.MAIN_AND_EXTRAS).remainingMinutes)
        assertEquals(47 * 60, game.timeProgress(CompletionGoal.COMPLETIONIST).remainingMinutes)
    }

    @Test
    fun boundTimeProgressWithoutSettingCompletionOrNarrativeProgress() {
        val game = Game(status = GameStatus.PLAYING, playedMinutes = 151, durations = Durations(100))
        val progress = game.timeProgress()

        assertEquals(100, progress.percent)
        assertEquals(0, progress.remainingMinutes)
        assertTrue(progress.aboveEstimate)
        assertFalse(game.status.isCompleted)
        assertNull(game.narrativePercent)
    }

    @Test
    fun distinguishesUnknownInputsFromZeroMinutesPlayed() {
        assertNull(TimeProgress.from(null, 120).remainingMinutes)
        assertNull(TimeProgress.from(30, null).percent)
        assertNull(TimeProgress.from(30, 0).remainingMinutes)
        assertEquals(120, TimeProgress.from(0, 120).remainingMinutes)
        assertEquals(0, TimeProgress.from(0, 120).percent)
        assertEquals(99, TimeProgress.from(1799, 1800).percent)
    }

    @Test
    fun backlogCountsUnknownGamesSeparatelyAndWithholdsIncompleteForecast() {
        val unknown = Game(durations = Durations(180))
        val known = Game(playedMinutes = 60, durations = Durations(180))
        val finished = Game(status = GameStatus.MAIN_STORY_COMPLETED)
        val today = LocalDate.of(2026, 11, 26)

        val mixed = listOf(unknown, known, finished).remainingTime()
        assertEquals(DurationTotal(120L, 1), mixed)
        assertNull(mixed.finishDate(today, 1))
        assertEquals(today.plusDays(1), listOf(known, finished).remainingTime().finishDate(today, 1))
        assertEquals(today, listOf(known).remainingTime().finishDate(today, 2))
        assertEquals(DurationTotal(null, 1), listOf(unknown).remainingTime())
        assertEquals(DurationTotal(null, 0), emptyList<Game>().remainingTime())
    }

    @Test
    fun completedStoryDoesNotClearACompletionistBacklogGoal() {
        val game = Game(
            status = GameStatus.MAIN_STORY_COMPLETED,
            goal = CompletionGoal.COMPLETIONIST,
            playedMinutes = 1800,
            durations = Durations(1800, 2700, 3900)
        )

        assertEquals(DurationTotal(2100L, 0), listOf(game).remainingTime())
        assertEquals(DurationTotal(0L, 0), listOf(game.copy(status = GameStatus.COMPLETED_100_PERCENT)).remainingTime())
    }
}
