package com.playcrastination.domain.model

import java.time.LocalDate

enum class BacklogOrder { MANUAL, SHORTEST_FIRST, LONGEST_FIRST }

enum class BacklogPlacement { BEFORE, AFTER }

data class ScheduledGame(
    val game: Game,
    val remainingMinutes: Long?,
    val startsOn: LocalDate?,
    val finishesOn: LocalDate?
)

data class BacklogPlan(
    val queue: List<ScheduledGame>,
    val playing: List<ScheduledGame>,
    val queueTime: DurationTotal,
    val playingTime: DurationTotal,
    val finishDate: LocalDate?
)

fun planBacklog(
    games: List<Game>,
    order: BacklogOrder,
    start: LocalDate,
    dailyHours: Int
): BacklogPlan {
    require(dailyHours in 1..24)
    val manualQueue = games.filter { it.backlogPosition != null }.sortedBy { it.backlogPosition }
    val queue = when (order) {
        BacklogOrder.MANUAL -> manualQueue
        BacklogOrder.SHORTEST_FIRST -> manualQueue.sortedWith(compareBy(nullsLast(naturalOrder<Int>())) {
            it.durations[it.goal]
        })
        BacklogOrder.LONGEST_FIRST -> manualQueue.sortedWith(compareBy(nullsLast(reverseOrder<Int>())) {
            it.durations[it.goal]
        })
    }
    val playing = games.filter { it.status == GameStatus.PLAYING }
        .sortedWith(compareBy<Game, LocalDate?>(nullsLast(reverseOrder<LocalDate>())) { it.lastPlayedOn }
            .thenBy { it.id })
    // Older versions also let a game be both queued and playing. Plan its time only once.
    val timeline = playing + queue.filter { it.status != GameStatus.PLAYING }
    val dailyMinutes = dailyHours * 60L
    var usedMinutes: Long? = 0L
    val scheduled = timeline.associate { game ->
        val remaining = game.remainingMinutesForGoal()
        val before = usedMinutes
        val after = if (before != null && remaining != null) before + remaining else null
        usedMinutes = after
        val startsOn = before?.takeIf { remaining != 0L }?.let { start.plusDays(it / dailyMinutes) }
        val finishesOn = after?.takeIf { remaining != 0L }?.let { start.plusDays((it - 1) / dailyMinutes) }
        game.id to ScheduledGame(game, remaining, startsOn, finishesOn)
    }
    return BacklogPlan(
        queue = queue.map { scheduled.getValue(it.id) },
        playing = playing.map { scheduled.getValue(it.id) },
        queueTime = queue.remainingTime(),
        playingTime = playing.remainingTime(),
        finishDate = timeline.remainingTime().finishDate(start, dailyHours)
    )
}
