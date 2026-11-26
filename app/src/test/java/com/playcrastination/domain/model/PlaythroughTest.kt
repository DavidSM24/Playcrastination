package com.playcrastination.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PlaythroughTest {
    @Test
    fun rejectsInvalidDatesAndTimeBreakdowns() {
        val start = LocalDate.of(2025, 4, 24)
        assertFalse(Playthrough(startedOn = start, finishedOn = start.minusDays(1)).isValid)
        assertFalse(Playthrough(startedOn = start, lastPlayedOn = start.minusDays(1)).isValid)
        assertFalse(Playthrough(playedMinutes = Int.MAX_VALUE, storyMinutes = Int.MAX_VALUE, otherMinutes = 1).isValid)
        assertFalse(Playthrough(narrativePercent = 101).isValid)
        assertTrue(Playthrough(playedMinutes = 120, storyMinutes = 80, extrasMinutes = 40).isValid)
        assertTrue(Playthrough(storyMinutes = 10).isValid)
    }

    @Test
    fun separatesLifetimePlaytimeFromSelectedGoalAndKeepsUnknowns() {
        val archived = Playthrough(playedMinutes = 1800, narrativePercent = 100)
        val game = Game(previousPlaythroughs = listOf(archived), durations = Durations(1500))
        assertEquals(1800L, game.recordedMinutes)
        assertNull(game.timeProgress().percent)
        assertNull(game.currentPlaythrough.narrativePercent)
        assertEquals(1460, game.copy(currentPlaythrough = Playthrough(playedMinutes = 40)).timeProgress().remainingMinutes)
        assertNull(Game(currentPlaythrough = Playthrough(storyMinutes = 10)).recordedMinutes)
        assertEquals(0L, Game(currentPlaythrough = Playthrough(playedMinutes = 0)).recordedMinutes)
    }
}
