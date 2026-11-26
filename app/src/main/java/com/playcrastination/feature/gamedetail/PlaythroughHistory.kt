package com.playcrastination.feature.gamedetail

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.playcrastination.R
import com.playcrastination.core.designsystem.Panel
import com.playcrastination.core.designsystem.PlayColors
import com.playcrastination.core.designsystem.PlaySpacing
import com.playcrastination.core.designsystem.SectionTitle
import com.playcrastination.core.designsystem.formattedDate
import com.playcrastination.core.designsystem.formattedDuration
import com.playcrastination.domain.model.Playthrough

@Composable
internal fun DetailValue(@StringRes label: Int, value: String) {
    if (value.isNotBlank()) {
        Text(stringResource(R.string.current_choice, stringResource(label), value), color = PlayColors.Muted)
    }
}

@Composable
internal fun PlaythroughFacts(playthrough: Playthrough) {
    playthrough.startedOn?.let { DetailValue(R.string.playthrough_start, formattedDate(it)) }
    playthrough.finishedOn?.let { DetailValue(R.string.playthrough_end, formattedDate(it)) }
    playthrough.lastPlayedOn?.let { DetailValue(R.string.last_played, formattedDate(it)) }
    playthrough.storyMinutes?.let { DetailValue(R.string.story_time, formattedDuration(it.toLong())) }
    playthrough.extrasMinutes?.let { DetailValue(R.string.extras_time, formattedDuration(it.toLong())) }
    playthrough.otherMinutes?.let { DetailValue(R.string.other_time, formattedDuration(it.toLong())) }
}

@Composable
internal fun ArchivedPlaythroughCard(playthrough: Playthrough, position: Int, busy: Boolean, onEdit: () -> Unit) {
    Panel(Modifier.fillMaxWidth().padding(horizontal = PlaySpacing.Inset)) {
        SectionTitle(stringResource(R.string.archived_playthrough_number, position))
        DetailValue(R.string.played_short, formattedDuration(playthrough.playedMinutes?.toLong()))
        playthrough.narrativePercent?.let { Text(stringResource(R.string.narrative_progress, it), color = PlayColors.Muted) }
        PlaythroughFacts(playthrough)
        OutlinedButton(enabled = !busy, onClick = onEdit) { Text(stringResource(R.string.edit_playthrough)) }
    }
}

@Composable
internal fun ArchivedPlaythroughDialog(
    playthrough: Playthrough,
    busy: Boolean,
    actionError: Boolean,
    onDismiss: () -> Unit,
    onSave: (Playthrough) -> Unit
) {
    var draft by rememberPlaythroughDraft(playthrough)
    val validated = draft.validated(playthrough.id)
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.edit_playthrough)) },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PlaySpacing.Gap)
            ) {
                PlaythroughFields(draft) { draft = it }
                SectionTitle(stringResource(R.string.time_breakdown))
                TimeBreakdownFields(draft) { draft = it }
                if (actionError) Text(stringResource(R.string.save_error), color = PlayColors.Error)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && validated != null, onClick = {
                validated?.let(onSave)
            }) { Text(stringResource(R.string.save_playthrough)) }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
