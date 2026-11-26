package com.playcrastination.domain.model

import java.time.LocalDate

data class DurationTotal(val knownMinutes: Long?, val unknownGames: Int) {
    fun finishDate(start: LocalDate, dailyHours: Int): LocalDate? {
        require(dailyHours in 1..24)
        val minutes = knownMinutes ?: return null
        if (unknownGames > 0) return null
        val perDay = dailyHours * 60L
        val days = (minutes + perDay - 1) / perDay
        return start.plusDays((days - 1).coerceAtLeast(0))
    }
}

fun List<Game>.remainingTime(): DurationTotal {
    var total = 0L
    var known = false
    var unknown = 0
    for (game in this) {
        val remaining = game.remainingMinutesForGoal()
        if (remaining == null) {
            unknown++
        } else {
            known = true
            total += remaining
        }
    }
    return DurationTotal(total.takeIf { known }, unknown)
}

fun Game.remainingMinutesForGoal(): Long? =
    if (hasReachedGoal) 0L else timeProgress().remainingMinutes?.toLong()
