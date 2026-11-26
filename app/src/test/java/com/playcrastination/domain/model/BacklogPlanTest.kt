package com.playcrastination.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BacklogPlanTest {
    private val today = LocalDate.of(2026, 11, 26)

    @Test
    fun spendsEachDaysRemainingBudgetAfterActiveGamesAndCountsQueuedActiveOnlyOnce() {
        val active = game(1, 3, 90, status = GameStatus.PLAYING)
        val first = game(2, 1, 90)
        val second = game(3, 2, 60)
        val third = game(4, 4, 1)

        val plan = planBacklog(listOf(third, second, active, first), BacklogOrder.MANUAL, today, 2)

        assertEquals(listOf(2L, 3L, 1L, 4L), plan.queue.map { it.game.id })
        assertEquals(today, plan.playing.single().finishesOn)
        assertEquals(today, plan.queue[0].startsOn)
        assertEquals(today.plusDays(1), plan.queue[0].finishesOn)
        assertEquals(today.plusDays(1), plan.queue[1].finishesOn)
        assertEquals(today.plusDays(2), plan.queue[3].startsOn)
        assertEquals(today.plusDays(2), plan.finishDate)
        assertEquals(DurationTotal(241L, 0), plan.queueTime)
        assertEquals(DurationTotal(90L, 0), plan.playingTime)
    }

    @Test
    fun preservesPartialForecastsWhenOneRemainingTimeIsUnknown() {
        val first = game(1, 1, 80)
        val unknown = game(2, 2, null)
        val third = game(3, 3, 60)
        val plan = planBacklog(listOf(first, unknown, third), BacklogOrder.MANUAL, today, 1)

        assertEquals(DurationTotal(140L, 1), plan.queueTime)
        assertEquals(today.plusDays(1), plan.queue[0].finishesOn)
        assertEquals(today.plusDays(1), plan.queue[1].startsOn)
        assertNull(plan.queue[1].finishesOn)
        assertNull(plan.queue[2].startsOn)
        assertNull(plan.finishDate)
    }

    @Test
    fun unknownActiveTimeBlocksWaitingDatesButLeavesKnownQueueSubtotal() {
        val active = game(1, null, 120, played = null, status = GameStatus.PLAYING)
        val queue = game(2, 1, 120)
        val plan = planBacklog(listOf(queue, active), BacklogOrder.MANUAL, today, 1)

        assertEquals(DurationTotal(120L, 0), plan.queueTime)
        assertEquals(DurationTotal(null, 1), plan.playingTime)
        assertEquals(today, plan.playing.single().startsOn)
        assertNull(plan.queue.single().startsOn)
        assertNull(plan.finishDate)
    }

    @Test
    fun temporaryDurationViewsKeepUnknownsLastAndManualTiesStable() {
        val games = listOf(
            game(1, 1, 180), game(2, 2, null), game(3, 3, 30), game(4, 4, 180)
        )
        fun order(mode: BacklogOrder) =
            planBacklog(games, mode, today, 1).queue.map { it.game.id }

        assertEquals(listOf(1L, 2L, 3L, 4L), order(BacklogOrder.MANUAL))
        assertEquals(listOf(3L, 1L, 4L, 2L), order(BacklogOrder.SHORTEST_FIRST))
        assertEquals(listOf(1L, 4L, 3L, 2L), order(BacklogOrder.LONGEST_FIRST))
        assertEquals(listOf(1L, 2L, 3L, 4L), order(BacklogOrder.MANUAL))
    }

    @Test
    fun completedGoalConsumesNoTimeAndBudgetControlsDates() {
        val completed = game(1, 1, null, played = null, status = GameStatus.MAIN_STORY_COMPLETED)
        val waiting = game(2, 2, 130)
        val fast = planBacklog(listOf(completed, waiting), BacklogOrder.MANUAL, today, 3)
        val slow = planBacklog(listOf(completed, waiting), BacklogOrder.MANUAL, today, 1)

        assertEquals(0L, fast.queue[0].remainingMinutes)
        assertNull(fast.queue[0].startsOn)
        assertEquals(today, fast.queue[1].startsOn)
        assertEquals(today, fast.finishDate)
        assertEquals(today.plusDays(2), slow.finishDate)
    }

    private fun game(
        id: Long,
        position: Long?,
        target: Int?,
        played: Int? = 0,
        status: GameStatus = GameStatus.NOT_STARTED
    ) = Game(
        id = id,
        backlogPosition = position,
        status = status,
        durations = Durations(mainMinutes = target),
        currentPlaythrough = Playthrough(playedMinutes = played)
    )
}
