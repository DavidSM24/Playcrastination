package com.playcrastination.domain.model

enum class GameStatus(val reachedGoal: CompletionGoal? = null) {
    NOT_STARTED, PLAYING, ON_HOLD,
    MAIN_STORY_COMPLETED(CompletionGoal.MAIN_STORY),
    MAIN_AND_EXTRAS_COMPLETED(CompletionGoal.MAIN_AND_EXTRAS),
    COMPLETED_100_PERCENT(CompletionGoal.COMPLETIONIST),
    ABANDONED, WISHLIST;

    val isCompleted: Boolean get() = reachedGoal != null
}

enum class Priority { LOW, NORMAL, HIGH }

enum class CompletionGoal { MAIN_STORY, MAIN_AND_EXTRAS, COMPLETIONIST }

data class Durations(
    val mainMinutes: Int? = null,
    val extrasMinutes: Int? = null,
    val completionistMinutes: Int? = null
) {
    operator fun get(goal: CompletionGoal): Int? = when (goal) {
        CompletionGoal.MAIN_STORY -> mainMinutes
        CompletionGoal.MAIN_AND_EXTRAS -> extrasMinutes
        CompletionGoal.COMPLETIONIST -> completionistMinutes
    }
}

data class Game(
    val id: Long = 0,
    val title: String = "",
    val releaseYear: Int? = null,
    val genre: String = "",
    val platform: String = "",
    val coverUrl: String = "",
    val bannerUrl: String = "",
    val description: String = "",
    val notes: String = "",
    val status: GameStatus = GameStatus.NOT_STARTED,
    val priority: Priority = Priority.NORMAL,
    val goal: CompletionGoal = CompletionGoal.MAIN_STORY,
    val playedMinutes: Int? = null,
    val narrativePercent: Int? = null,
    val durations: Durations = Durations(),
    val favorite: Boolean = false,
    val backlogPosition: Long? = null
) {
    val hasReachedGoal: Boolean get() = status.reachedGoal?.let { it >= goal } ?: false

    fun timeProgress(forGoal: CompletionGoal = goal): TimeProgress =
        TimeProgress.from(playedMinutes, durations[forGoal])
}
