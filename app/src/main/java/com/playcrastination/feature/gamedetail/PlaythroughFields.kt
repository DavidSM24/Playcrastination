package com.playcrastination.feature.gamedetail

import androidx.annotation.StringRes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import com.playcrastination.R
import com.playcrastination.core.designsystem.PlayColors
import com.playcrastination.domain.model.Playthrough
import java.time.LocalDate
import java.time.format.DateTimeParseException

internal data class PlaythroughDraft(
    val played: String,
    val narrative: String,
    val start: String,
    val finish: String,
    val lastPlayed: String,
    val story: String,
    val extras: String,
    val other: String
) {
    constructor(playthrough: Playthrough) : this(
        playthrough.playedMinutes?.toString().orEmpty(),
        playthrough.narrativePercent?.toString().orEmpty(),
        playthrough.startedOn?.toString().orEmpty(),
        playthrough.finishedOn?.toString().orEmpty(),
        playthrough.lastPlayedOn?.toString().orEmpty(),
        playthrough.storyMinutes?.toString().orEmpty(),
        playthrough.extrasMinutes?.toString().orEmpty(),
        playthrough.otherMinutes?.toString().orEmpty()
    )

    fun parsed(id: Long = 0): Playthrough = Playthrough(
        id = id,
        playedMinutes = played.trim().toIntOrNull(),
        narrativePercent = narrative.trim().toIntOrNull(),
        startedOn = start.parsedDate(),
        finishedOn = finish.parsedDate(),
        lastPlayedOn = lastPlayed.parsedDate(),
        storyMinutes = story.trim().toIntOrNull(),
        extrasMinutes = extras.trim().toIntOrNull(),
        otherMinutes = other.trim().toIntOrNull()
    )

    fun validated(id: Long): Playthrough? {
        if (listOf(played, story, extras, other).any { !it.validNumber(0..Int.MAX_VALUE) } ||
            !narrative.validNumber(0..100) || listOf(start, finish, lastPlayed).any { !it.validDate() }
        ) return null
        return parsed(id).takeIf(Playthrough::isValid)
    }
}

private val PlaythroughDraftSaver = listSaver<PlaythroughDraft, String>(
    save = { listOf(it.played, it.narrative, it.start, it.finish, it.lastPlayed, it.story, it.extras, it.other) },
    restore = { PlaythroughDraft(it[0], it[1], it[2], it[3], it[4], it[5], it[6], it[7]) }
)

@Composable
internal fun rememberPlaythroughDraft(playthrough: Playthrough): MutableState<PlaythroughDraft> =
    rememberSaveable(playthrough.id, stateSaver = PlaythroughDraftSaver) {
        mutableStateOf(PlaythroughDraft(playthrough))
    }

@Composable
internal fun PlaythroughFields(draft: PlaythroughDraft, onChange: (PlaythroughDraft) -> Unit) {
    EditField(draft.played, { onChange(draft.copy(played = it)) }, R.string.played_input, 0..Int.MAX_VALUE)
    EditField(draft.narrative, { onChange(draft.copy(narrative = it)) }, R.string.narrative_input, 0..100)
    DateField(draft.start, { onChange(draft.copy(start = it)) }, R.string.playthrough_start)
    DateField(draft.finish, { onChange(draft.copy(finish = it)) }, R.string.playthrough_end)
    DateField(draft.lastPlayed, { onChange(draft.copy(lastPlayed = it)) }, R.string.last_played)
    if (!draft.parsed().hasValidDates) {
        Text(stringResource(R.string.invalid_playthrough_dates), color = PlayColors.Error)
    }
}

@Composable
internal fun TimeBreakdownFields(draft: PlaythroughDraft, onChange: (PlaythroughDraft) -> Unit) {
    EditField(draft.story, { onChange(draft.copy(story = it)) }, R.string.story_minutes, 0..Int.MAX_VALUE)
    EditField(draft.extras, { onChange(draft.copy(extras = it)) }, R.string.extras_minutes, 0..Int.MAX_VALUE)
    EditField(draft.other, { onChange(draft.copy(other = it)) }, R.string.other_minutes, 0..Int.MAX_VALUE)
    if (!draft.parsed().hasValidTimes && listOf(draft.played, draft.story, draft.extras, draft.other)
            .all { it.validNumber(0..Int.MAX_VALUE) }
    ) {
        Text(stringResource(R.string.invalid_breakdown), color = PlayColors.Error)
    }
}

@Composable
internal fun DateField(value: String, onChange: (String) -> Unit, @StringRes label: Int) {
    EditField(
        value,
        onChange,
        label,
        validationError = if (value.validDate()) null else R.string.invalid_date,
        hint = R.string.iso_date_hint
    )
}

internal fun String.validDate(): Boolean = isBlank() || parsedDate() != null

internal fun String.parsedDate(): LocalDate? {
    if (isBlank()) return null
    return try {
        LocalDate.parse(trim()).takeIf { it.year in 1..9999 }
    } catch (_: DateTimeParseException) {
        null
    }
}
