package com.playcrastination.feature.gamedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.playcrastination.R
import com.playcrastination.core.designsystem.formattedDuration
import com.playcrastination.core.designsystem.GameArtwork
import com.playcrastination.core.designsystem.OrbitArtwork
import com.playcrastination.core.designsystem.Panel
import com.playcrastination.core.designsystem.PlayColors
import com.playcrastination.core.designsystem.PlayProgress
import com.playcrastination.core.designsystem.PlaySpacing
import com.playcrastination.core.designsystem.SectionTitle
import com.playcrastination.core.designsystem.StatusTag
import com.playcrastination.core.designsystem.label
import com.playcrastination.domain.model.CompletionGoal
import com.playcrastination.domain.model.Game

@Composable
fun GameDetailScreen(
    game: Game,
    busy: Boolean,
    actionError: Boolean,
    onEdit: () -> Unit,
    onSetBacklog: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val scroll = rememberLazyListState()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val progress = game.timeProgress()
    LazyColumn(
        state = scroll,
        contentPadding = PaddingValues(bottom = PlaySpacing.Section),
        verticalArrangement = Arrangement.spacedBy(PlaySpacing.Gap)
    ) {
        item { DetailHero(game) { if (scroll.firstVisibleItemIndex == 0) scroll.firstVisibleItemScrollOffset else 0 } }
        item {
            Panel(Modifier.fillMaxWidth().padding(horizontal = PlaySpacing.Inset)) {
                SectionTitle(stringResource(R.string.personal_progress))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TimeMetric(formattedDuration(progress.playedMinutes?.toLong()), stringResource(R.string.played_short), Modifier.weight(1f))
                    TimeMetric(formattedDuration(progress.targetMinutes?.toLong()), stringResource(R.string.target_short), Modifier.weight(1f))
                    TimeMetric(formattedDuration(progress.remainingMinutes?.toLong()), stringResource(R.string.remaining_short), Modifier.weight(1f))
                }
                Text(stringResource(R.string.current_choice, stringResource(R.string.goal), stringResource(game.goal.label)),
                    color = PlayColors.Muted)
                if (progress.percent != null) {
                    Text(stringResource(R.string.timed_progress, progress.percent), color = PlayColors.Cyan)
                    PlayProgress(progress.percent)
                } else {
                    Text(stringResource(R.string.progress_unavailable), color = PlayColors.Muted)
                }
                if (progress.aboveEstimate) Text(stringResource(R.string.above_estimate), color = PlayColors.Warning)
                game.narrativePercent?.let { narrative ->
                    Text(stringResource(R.string.narrative_progress, narrative), color = PlayColors.LightViolet)
                }
            }
        }
        item { GoalsSection(game) }
        if (game.description.isNotBlank() || game.genre.isNotBlank() || game.platform.isNotBlank()) {
            item {
                Panel(Modifier.fillMaxWidth().padding(horizontal = PlaySpacing.Inset)) {
                    SectionTitle(stringResource(R.string.general_info))
                    if (game.description.isNotBlank()) Text(game.description)
                    if (game.genre.isNotBlank()) {
                        Text(stringResource(R.string.current_choice, stringResource(R.string.genre), game.genre), color = PlayColors.Muted)
                    }
                    if (game.platform.isNotBlank()) {
                        Text(stringResource(R.string.current_choice, stringResource(R.string.platform), game.platform), color = PlayColors.Muted)
                    }
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth().padding(horizontal = PlaySpacing.Inset)) {
                SectionTitle(stringResource(R.string.personal_info))
                Text(stringResource(game.priority.label), color = PlayColors.LightViolet)
                if (game.favorite) Text(stringResource(R.string.favorite), color = PlayColors.Cyan)
                if (game.notes.isNotBlank()) Text(game.notes)
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = PlaySpacing.Inset),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onEdit, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.edit_game))
                }
                FilledTonalButton(
                    onClick = { onSetBacklog(game.backlogPosition == null) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(if (game.backlogPosition == null) R.string.add_to_queue else R.string.remove_from_queue))
                }
                TextButton(onClick = { confirmDelete = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.delete_game), color = PlayColors.Error)
                }
                if (actionError) Text(stringResource(R.string.save_error), color = PlayColors.Error)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_game)) },
            text = { Text(stringResource(R.string.delete_confirmation, game.title)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun DetailHero(game: Game, scrollOffset: () -> Int) {
    Box(Modifier.fillMaxWidth().heightIn(min = 306.dp).clip(MaterialTheme.shapes.large)) {
        val background = game.bannerUrl.ifBlank { game.coverUrl }
        val backdrop = Modifier.matchParentSize().graphicsLayer {
            translationY = scrollOffset() * 0.35f
            scaleX = 1.08f
            scaleY = 1.08f
        }
        if (background.isBlank()) {
            OrbitArtwork(backdrop.background(Brush.linearGradient(listOf(Color(0xFF43276E), Color(0xFF112B42)))))
        } else {
            GameArtwork(background, game.title, backdrop)
        }
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(
            listOf(Color(0x100B0F19), Color(0x6B0B0F19), PlayColors.Background)
        )))
        Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(PlaySpacing.Inset),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val coverShape = MaterialTheme.shapes.medium
            GameArtwork(game.coverUrl, game.title, Modifier.size(86.dp, 116.dp)
                .clip(coverShape).border(1.dp, PlayColors.LightViolet.copy(alpha = 0.55f), coverShape))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                StatusTag(game.status)
                Text(game.title, style = MaterialTheme.typography.headlineMedium)
                val metadata = listOfNotNull(game.releaseYear?.toString(), game.genre, game.platform)
                    .filter { it.isNotBlank() }.joinToString(" · ")
                if (metadata.isNotBlank()) Text(metadata, color = PlayColors.Muted)
            }
        }
    }
}

@Composable
private fun TimeMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = PlayColors.Text)
        Text(label, style = MaterialTheme.typography.labelSmall, color = PlayColors.Muted)
    }
}

@Composable
private fun GoalsSection(game: Game) {
    val uriHandler = LocalUriHandler.current
    var externalError by rememberSaveable { mutableStateOf(false) }
    Panel(Modifier.fillMaxWidth().padding(horizontal = PlaySpacing.Inset)) {
        SectionTitle(stringResource(R.string.goals_section))
        CompletionGoal.entries.forEach { goal ->
            val progress = game.timeProgress(goal)
            val selected = goal == game.goal
            val shape = MaterialTheme.shapes.medium
            Column(
                Modifier.fillMaxWidth().clip(shape)
                    .background(if (selected) PlayColors.Violet.copy(alpha = 0.17f) else PlayColors.Navigation)
                    .border(1.dp, if (selected) PlayColors.Violet else PlayColors.Line, shape)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(stringResource(goal.label), color = if (selected) PlayColors.LightViolet else PlayColors.Text,
                    style = MaterialTheme.typography.titleMedium)
                if (selected) Text(stringResource(R.string.selected_goal), style = MaterialTheme.typography.labelSmall)
                Text(stringResource(R.string.queue_game_duration, formattedDuration(progress.targetMinutes?.toLong()),
                    formattedDuration(progress.remainingMinutes?.toLong())), color = PlayColors.Muted)
                if (progress.targetMinutes != null) Text(stringResource(R.string.manual_estimate),
                    color = PlayColors.Muted, style = MaterialTheme.typography.labelSmall)
                if (progress.percent != null) PlayProgress(progress.percent)
            }
        }
        OutlinedButton(onClick = {
            try {
                uriHandler.openUri("https://howlongtobeat.com/")
                externalError = false
            } catch (_: IllegalArgumentException) {
                externalError = true
            }
        }) { Text(stringResource(R.string.open_hltb)) }
        if (externalError) Text(stringResource(R.string.open_link_error), color = PlayColors.Error)
    }
}
