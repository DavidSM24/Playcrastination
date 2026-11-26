package com.playcrastination.data.mapper

import com.playcrastination.core.database.GameEntity
import com.playcrastination.core.database.GameRecord
import com.playcrastination.core.database.PlaythroughEntity
import com.playcrastination.core.database.SteamLinkEntity
import com.playcrastination.domain.model.CompletionGoal
import com.playcrastination.domain.model.Durations
import com.playcrastination.domain.model.Game
import com.playcrastination.domain.model.GameStatus
import com.playcrastination.domain.model.Playthrough
import com.playcrastination.domain.model.Priority
import com.playcrastination.domain.model.SteamAchievements
import com.playcrastination.domain.model.SteamLink
import java.time.LocalDate

internal fun GameRecord.toDomain(): Game {
    val orderedPlaythroughs = playthroughs.sortedByDescending(PlaythroughEntity::id)
    return Game(
        id = game.id,
        title = game.title,
        originalTitle = game.originalTitle.orEmpty(),
        releaseYear = game.releaseYear,
        releaseDate = game.releaseDay?.let(LocalDate::ofEpochDay),
        developer = game.developer.orEmpty(),
        publisher = game.publisher.orEmpty(),
        genre = game.genre,
        platform = game.platform,
        availablePlatforms = game.availablePlatforms.orEmpty(),
        coverUrl = game.coverUrl,
        bannerUrl = game.bannerUrl,
        backgroundUrl = game.backgroundUrl.orEmpty(),
        description = game.description,
        notes = game.notes,
        status = GameStatus.valueOf(game.status),
        priority = Priority.valueOf(game.priority),
        goal = CompletionGoal.valueOf(game.goal),
        currentPlaythrough = checkNotNull(orderedPlaythroughs.firstOrNull()).toDomain(),
        previousPlaythroughs = orderedPlaythroughs.drop(1).map(PlaythroughEntity::toDomain),
        durations = Durations(durations?.mainMinutes, durations?.extrasMinutes, durations?.completionistMinutes),
        favorite = game.favorite,
        rating = game.rating,
        backlogPosition = backlog?.position,
        steam = steam?.toDomain()
    )
}

private fun SteamLinkEntity.toDomain() = SteamLink(
    steamId, appId, playedMinutes, recentMinutes, lastPlayedSeconds, hidden,
    if (unlocked != null && achievementCount != null) SteamAchievements(unlocked, achievementCount) else null
)

internal fun Game.toEntity(createdAt: Long, updatedAt: Long) = GameEntity(
    id = id,
    title = title.trim(),
    releaseYear = releaseYear,
    genre = genre.trim(),
    platform = platform.trim(),
    coverUrl = coverUrl.trim(),
    bannerUrl = bannerUrl.trim(),
    description = description.trim(),
    notes = notes.trim(),
    status = status.name,
    priority = priority.name,
    goal = goal.name,
    favorite = favorite,
    createdAt = createdAt,
    updatedAt = updatedAt,
    originalTitle = originalTitle.trim().ifEmpty { null },
    releaseDay = releaseDate?.toEpochDay(),
    developer = developer.trim().ifEmpty { null },
    publisher = publisher.trim().ifEmpty { null },
    availablePlatforms = availablePlatforms.trim().ifEmpty { null },
    backgroundUrl = backgroundUrl.trim().ifEmpty { null },
    rating = rating
)

internal fun Playthrough.toEntity(gameId: Long) = PlaythroughEntity(
    id = id,
    gameId = gameId,
    playedMinutes = playedMinutes,
    narrativePercent = narrativePercent,
    startedDay = startedOn?.toEpochDay(),
    finishedDay = finishedOn?.toEpochDay(),
    lastPlayedDay = lastPlayedOn?.toEpochDay(),
    storyMinutes = storyMinutes,
    extrasMinutes = extrasMinutes,
    otherMinutes = otherMinutes
)

internal fun PlaythroughEntity.toDomain() = Playthrough(
    id = id,
    playedMinutes = playedMinutes,
    narrativePercent = narrativePercent,
    startedOn = startedDay?.let(LocalDate::ofEpochDay),
    finishedOn = finishedDay?.let(LocalDate::ofEpochDay),
    lastPlayedOn = lastPlayedDay?.let(LocalDate::ofEpochDay),
    storyMinutes = storyMinutes,
    extrasMinutes = extrasMinutes,
    otherMinutes = otherMinutes
)
