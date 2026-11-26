package com.playcrastination.domain.model

data class TimeProgress(
    val playedMinutes: Int?,
    val targetMinutes: Int?,
    val remainingMinutes: Int?,
    val percent: Int?,
    val aboveEstimate: Boolean
) {
    companion object {
        fun from(playedMinutes: Int?, targetMinutes: Int?): TimeProgress {
            val played = playedMinutes?.takeIf { it >= 0 }
            val target = targetMinutes?.takeIf { it > 0 }
            val remaining = if (played != null && target != null) {
                (target - played).coerceAtLeast(0)
            } else null
            val percent = if (played != null && target != null) {
                (played.coerceAtMost(target) * 100L / target).toInt()
            } else null
            return TimeProgress(
                playedMinutes = played,
                targetMinutes = target,
                remainingMinutes = remaining,
                percent = percent,
                aboveEstimate = played != null && target != null && played > target
            )
        }
    }
}
