package com.playcrastination.domain.model

import java.time.LocalDate

data class Playthrough(
    val id: Long = 0,
    val playedMinutes: Int? = null,
    val narrativePercent: Int? = null,
    val startedOn: LocalDate? = null,
    val finishedOn: LocalDate? = null,
    val lastPlayedOn: LocalDate? = null,
    val storyMinutes: Int? = null,
    val extrasMinutes: Int? = null,
    val otherMinutes: Int? = null
) {
    val hasDetails: Boolean
        get() = playedMinutes != null || narrativePercent != null || startedOn != null ||
            finishedOn != null || lastPlayedOn != null || storyMinutes != null ||
            extrasMinutes != null || otherMinutes != null

    val hasValidDates: Boolean
        get() = startedOn == null ||
            ((finishedOn == null || !finishedOn.isBefore(startedOn)) &&
                (lastPlayedOn == null || !lastPlayedOn.isBefore(startedOn)))

    val hasValidTimes: Boolean
        get() {
            val breakdown = listOfNotNull(storyMinutes, extrasMinutes, otherMinutes)
            return (playedMinutes == null || playedMinutes >= 0) && breakdown.all { it >= 0 } &&
                (playedMinutes == null || breakdown.sumOf(Int::toLong) <= playedMinutes)
        }

    val isValid: Boolean
        get() = hasValidDates && hasValidTimes && (narrativePercent == null || narrativePercent in 0..100)
}
