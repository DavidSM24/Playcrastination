package com.playcrastination.feature.backlog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.playcrastination.R
import com.playcrastination.core.designsystem.GameArtwork
import com.playcrastination.core.designsystem.OptionPicker
import com.playcrastination.core.designsystem.Panel
import com.playcrastination.core.designsystem.PlayColors
import com.playcrastination.core.designsystem.StatusTag
import com.playcrastination.core.designsystem.formattedDate
import com.playcrastination.core.designsystem.formattedDuration
import com.playcrastination.core.designsystem.label
import com.playcrastination.domain.model.BacklogPlacement
import com.playcrastination.domain.model.CompletionGoal
import com.playcrastination.domain.model.Game
import com.playcrastination.domain.model.GameStatus
import com.playcrastination.domain.model.Priority
import com.playcrastination.domain.model.ScheduledGame

private enum class BacklogPreference { PRIORITY, GOAL }

@Composable
internal fun BacklogRow(
    plan: ScheduledGame,
    position: Int?,
    onGame: () -> Unit,
    write: BacklogWrite,
    viewModel: BacklogViewModel,
    modifier: Modifier = Modifier,
    previousId: Long? = null,
    nextId: Long? = null,
    drag: BacklogDrag? = null,
    drop: BacklogPlacement? = null
) {
    val game = plan.game
    val dragging = drag?.row?.id == game.id
    Column(modifier.fillMaxWidth()) {
        DropLine(drop == BacklogPlacement.BEFORE)
        Panel(
            Modifier.fillMaxWidth().then(
                if (dragging) Modifier.border(2.dp, PlayColors.Cyan, MaterialTheme.shapes.large) else Modifier
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (drag != null) DragHandle(game, drag)
                Row(
                    Modifier.weight(1f).clip(MaterialTheme.shapes.small).clickable(onClick = onGame),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    GameArtwork(game.coverUrl, game.title, Modifier.size(38.dp, 50.dp).clip(MaterialTheme.shapes.small))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        if (position != null) {
                            Text(stringResource(R.string.backlog_position, position), color = PlayColors.Cyan,
                                style = MaterialTheme.typography.labelSmall)
                        }
                        Text(game.title, style = MaterialTheme.typography.titleMedium, maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                        StatusTag(game.status)
                    }
                }
                BacklogActions(game, previousId, nextId, write != BacklogWrite.SAVING, viewModel)
            }
            Text(
                stringResource(R.string.backlog_priority_goal, stringResource(game.priority.label), stringResource(game.goal.label)),
                style = MaterialTheme.typography.labelSmall,
                color = if (game.priority == Priority.HIGH) PlayColors.Warning else PlayColors.LightViolet
            )
            Text(
                stringResource(
                    R.string.queue_game_duration,
                    formattedDuration(game.durations[game.goal]?.toLong()),
                    formattedDuration(plan.remainingMinutes)
                ),
                color = PlayColors.Muted,
                style = MaterialTheme.typography.bodySmall
            )
            if (plan.remainingMinutes != 0L) {
                val starts = plan.startsOn?.let { formattedDate(it) } ?: stringResource(R.string.unknown_value)
                val finishes = plan.finishesOn?.let { formattedDate(it) } ?: stringResource(R.string.unknown_value)
                val dateLabel = if (game.status == GameStatus.PLAYING) R.string.backlog_active_dates
                    else R.string.backlog_game_dates
                Text(stringResource(dateLabel, starts, finishes), color = PlayColors.Cyan,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        DropLine(drop == BacklogPlacement.AFTER)
    }
}

@Composable
private fun DropLine(visible: Boolean) {
    Box(Modifier.fillMaxWidth().height(2.dp).background(if (visible) PlayColors.Cyan else Color.Transparent))
}

@Composable
private fun DragHandle(game: Game, drag: BacklogDrag) {
    DisposableEffect(game.id, drag) {
        onDispose { drag.handles.remove(game.id) }
    }
    Box(
        Modifier.size(42.dp, 48.dp).onGloballyPositioned { drag.handles[game.id] = it.boundsInRoot() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.drag_backlog_game, game.title),
            tint = PlayColors.Cyan)
    }
}

@Composable
private fun BacklogActions(
    game: Game,
    previousId: Long?,
    nextId: Long?,
    enabled: Boolean,
    viewModel: BacklogViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    var preference by rememberSaveable { mutableStateOf<BacklogPreference?>(null) }
    Box {
        IconButton(enabled = enabled, onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.backlog_game_actions, game.title))
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.change_priority)) }, onClick = {
                expanded = false
                preference = BacklogPreference.PRIORITY
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.change_goal)) }, onClick = {
                expanded = false
                preference = BacklogPreference.GOAL
            })
            if (previousId != null) {
                DropdownMenuItem(text = { Text(stringResource(R.string.move_backlog_up)) }, onClick = {
                    expanded = false
                    viewModel.move(game.id, previousId, BacklogPlacement.BEFORE)
                })
            }
            if (nextId != null) {
                DropdownMenuItem(text = { Text(stringResource(R.string.move_backlog_down)) }, onClick = {
                    expanded = false
                    viewModel.move(game.id, nextId, BacklogPlacement.AFTER)
                })
            }
            if (game.status == GameStatus.PLAYING) {
                DropdownMenuItem(text = { Text(stringResource(R.string.pause_to_backlog)) }, onClick = {
                    expanded = false
                    viewModel.pause(game.id)
                })
            } else if (game.backlogPosition != null) {
                DropdownMenuItem(text = { Text(stringResource(R.string.start_from_backlog)) }, onClick = {
                    expanded = false
                    viewModel.start(game.id)
                })
            }
            if (game.backlogPosition != null) {
                DropdownMenuItem(text = { Text(stringResource(R.string.remove_from_queue)) }, onClick = {
                    expanded = false
                    viewModel.remove(game.id)
                })
            }
        }
    }

    preference?.let { selected ->
        AlertDialog(
            onDismissRequest = { preference = null },
            title = {
                Text(stringResource(if (selected == BacklogPreference.PRIORITY) R.string.change_priority else R.string.change_goal))
            },
            text = {
                when (selected) {
                    BacklogPreference.PRIORITY -> OptionPicker(stringResource(R.string.priority), game.priority,
                        Priority.entries, { stringResource(it.label) }) {
                        preference = null
                        viewModel.setPriority(game.id, it)
                    }
                    BacklogPreference.GOAL -> OptionPicker(stringResource(R.string.goal), game.goal,
                        CompletionGoal.entries, { stringResource(it.label) }) {
                        preference = null
                        viewModel.setGoal(game.id, it)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { preference = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
