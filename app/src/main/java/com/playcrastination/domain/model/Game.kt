package com.playcrastination.domain.model

import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId

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
    val originalTitle: String = "",
    val releaseYear: Int? = null,
    val releaseDate: LocalDate? = null,
    val developer: String = "",
    val publisher: String = "",
    val genre: String = "",
    val platform: String = "",
    val availablePlatforms: String = "",
    val coverUrl: String = "",
    val bannerUrl: String = "",
    val backgroundUrl: String = "",
    val description: String = "",
    val notes: String = "",
    val status: GameStatus = GameStatus.NOT_STARTED,
    val priority: Priority = Priority.NORMAL,
    val goal: CompletionGoal = CompletionGoal.MAIN_STORY,
    val currentPlaythrough: Playthrough = Playthrough(),
    val previousPlaythroughs: List<Playthrough> = emptyList(),
    val durations: Durations = Durations(),
    val favorite: Boolean = false,
    val rating: Int? = null,
    val backlogPosition: Long? = null,
    val steam: SteamLink? = null
) {
    val hasReachedGoal: Boolean get() = status.reachedGoal?.let { it >= goal } ?: false

    val resolvedReleaseYear: Int? get() = releaseDate?.year ?: releaseYear
    val genres: List<String> get() = genre.catalogValues()
    val platforms: List<String> get() = (availablePlatforms.catalogValues() + platform.trim()).filter(String::isNotEmpty)

    val recordedMinutes: Long?
        get() {
            val past = previousPlaythroughs.mapNotNull { it.playedMinutes?.toLong() }
            val current = currentPlaythrough.playedMinutes
            return if (past.isEmpty() && current == null) null else past.sum() + (current ?: 0)
        }

    val lastPlayedOn: LocalDate?
        get() = (previousPlaythroughs.mapNotNull(Playthrough::lastPlayedOn) +
            listOfNotNull(currentPlaythrough.lastPlayedOn, steam?.lastPlayedSeconds?.let {
                Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).toLocalDate()
            })).maxOrNull()

    fun timeProgress(forGoal: CompletionGoal = goal): TimeProgress =
        TimeProgress.from(currentPlaythrough.playedMinutes, durations[forGoal])
}

private fun String.catalogValues(): List<String> =
    split(',', ';').map(String::trim).filter(String::isNotEmpty)
