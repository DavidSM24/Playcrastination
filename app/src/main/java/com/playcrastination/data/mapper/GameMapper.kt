package com.playcrastination.data.mapper

import com.playcrastination.core.database.GameEntity
import com.playcrastination.core.database.GameRecord
import com.playcrastination.domain.model.CompletionGoal
import com.playcrastination.domain.model.Durations
import com.playcrastination.domain.model.Game
import com.playcrastination.domain.model.GameStatus
import com.playcrastination.domain.model.Priority

internal fun GameRecord.toDomain() = Game(
    id = game.id,
    title = game.title,
    releaseYear = game.releaseYear,
    genre = game.genre,
    platform = game.platform,
    coverUrl = game.coverUrl,
    bannerUrl = game.bannerUrl,
    description = game.description,
    notes = game.notes,
    status = GameStatus.valueOf(game.status),
    priority = Priority.valueOf(game.priority),
    goal = CompletionGoal.valueOf(game.goal),
    playedMinutes = progress?.playedMinutes,
    narrativePercent = progress?.narrativePercent,
    durations = Durations(durations?.mainMinutes, durations?.extrasMinutes, durations?.completionistMinutes),
    favorite = game.favorite,
    backlogPosition = backlog?.position
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
    updatedAt = updatedAt
)
