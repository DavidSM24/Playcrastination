package com.playcrastination.feature.backlog

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playcrastination.R
import com.playcrastination.core.designsystem.DailyHoursPicker
import com.playcrastination.core.designsystem.EmptyPanel
import com.playcrastination.core.designsystem.OptionPicker
import com.playcrastination.core.designsystem.Panel
import com.playcrastination.core.designsystem.PlayColors
import com.playcrastination.core.designsystem.PlaySpacing
import com.playcrastination.core.designsystem.ScreenTitle
import com.playcrastination.core.designsystem.SectionTitle
import com.playcrastination.core.designsystem.formattedDate
import com.playcrastination.core.designsystem.formattedDuration
import com.playcrastination.domain.model.BacklogOrder
import com.playcrastination.domain.model.BacklogPlan
import com.playcrastination.domain.model.Game
import com.playcrastination.domain.model.GameStatus
import com.playcrastination.domain.model.planBacklog
import java.time.LocalDate

@Composable
fun BacklogScreen(
    games: List<Game>,
    hours: Int,
    onGame: (Long) -> Unit,
    onLibrary: () -> Unit,
    onHoursChange: (Int) -> Unit,
    hoursError: Boolean,
    viewModel: BacklogViewModel = hiltViewModel()
) {
    val write by viewModel.write.collectAsStateWithLifecycle()
    var order by rememberSaveable { mutableStateOf(BacklogOrder.MANUAL) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val today = LocalDate.now()
    val plan = remember(games, order, today, hours) { planBacklog(games, order, today, hours) }
    val candidates = remember(games) { games.filter {
        it.backlogPosition == null && it.status != GameStatus.PLAYING && it.steam?.hidden != true
    } }
    val queued = plan.queue
    val canReorder = order == BacklogOrder.MANUAL && queued.size > 1 && write != BacklogWrite.SAVING
    val listState = rememberLazyListState()
    val drag = remember { BacklogDrag() }
    val dragTarget by remember(drag, listState) {
        derivedStateOf { drag.row?.let { drag.target(listState, it) } }
    }
    val dispatchMove by rememberUpdatedState { id: Long, target: BacklogDrop ->
        viewModel.move(id, target.anchorId, target.placement)
    }

    ScrollBacklogDuringDrag(drag, listState)
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().then(
            if (canReorder) Modifier.dragBacklogRows(drag, listState) { id, target -> dispatchMove(id, target) }
            else Modifier
        ),
        contentPadding = PaddingValues(start = PlaySpacing.Inset, end = PlaySpacing.Inset, bottom = PlaySpacing.Inset),
        verticalArrangement = Arrangement.spacedBy(PlaySpacing.Gap)
    ) {
        item("backlog-title") {
            ScreenTitle(stringResource(R.string.backlog), stringResource(R.string.backlog_intro))
        }
        item("backlog-summary") {
            BacklogSummary(plan, hours, onHoursChange, hoursError, write == BacklogWrite.FAILED)
        }
        val playingOutsideQueue = plan.playing.filter { it.game.backlogPosition == null }
        if (playingOutsideQueue.isNotEmpty()) {
            item("active-title") { SectionTitle(stringResource(R.string.continue_playing)) }
            items(playingOutsideQueue, key = { "active-${it.game.id}" }) { active ->
                BacklogRow(active, null, { onGame(active.game.id) }, write, viewModel)
            }
        }
        if (queued.isEmpty()) {
            item("backlog-empty") {
                if (candidates.isEmpty()) {
                    EmptyPanel(stringResource(R.string.queue_empty), stringResource(R.string.queue_empty_body),
                        stringResource(R.string.open_library), onLibrary)
                } else {
                    EmptyPanel(stringResource(R.string.queue_empty), stringResource(R.string.queue_empty_body),
                        stringResource(R.string.add_to_queue)) { adding = true }
                }
            }
        } else {
            item("backlog-order") {
                Column(verticalArrangement = Arrangement.spacedBy(PlaySpacing.Gap)) {
                    SectionTitle(stringResource(R.string.up_next))
                    OptionPicker(stringResource(R.string.backlog_order), order, BacklogOrder.entries,
                        { stringResource(it.label) }) { order = it }
                    if (order != BacklogOrder.MANUAL) {
                        Text(stringResource(R.string.backlog_preview_order), color = PlayColors.Muted)
                    }
                    if (candidates.isNotEmpty()) {
                        Button(enabled = write != BacklogWrite.SAVING, onClick = { adding = true }) {
                            Text(stringResource(R.string.add_to_queue))
                        }
                    }
                }
            }
            itemsIndexed(queued, key = { _, game -> game.game.id }) { index, scheduled ->
                val game = scheduled.game
                BacklogRow(
                    plan = scheduled,
                    position = index + 1,
                    onGame = { onGame(game.id) },
                    write = write,
                    viewModel = viewModel,
                    previousId = if (canReorder) queued.getOrNull(index - 1)?.game?.id else null,
                    nextId = if (canReorder) queued.getOrNull(index + 1)?.game?.id else null,
                    drag = drag.takeIf { canReorder },
                    drop = dragTarget?.takeIf { it.anchorId == game.id }?.placement,
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
    if (adding) {
        AddToBacklogDialog(candidates, onDismiss = { adding = false }) {
            viewModel.add(it)
            adding = false
        }
    }
}

@Composable
private fun BacklogSummary(
    plan: BacklogPlan,
    hours: Int,
    onHoursChange: (Int) -> Unit,
    hoursError: Boolean,
    writeError: Boolean
) {
    Panel(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.backlog_forecast))
        DailyHoursPicker(hours, onHoursChange)
        if (plan.queue.isNotEmpty()) {
            Text(stringResource(R.string.known_remaining, formattedDuration(plan.queueTime.knownMinutes)))
            if (plan.queueTime.unknownGames > 0) {
                Text(pluralStringResource(R.plurals.missing_estimates, plan.queueTime.unknownGames,
                    plan.queueTime.unknownGames), color = PlayColors.Muted)
            }
        }
        if (plan.playing.isNotEmpty()) {
            Text(stringResource(R.string.current_games_time, formattedDuration(plan.playingTime.knownMinutes)))
        }
        val finish = plan.finishDate
        if (finish != null) {
            Text(stringResource(R.string.forecast_finish, formattedDate(finish), hours), color = PlayColors.Cyan)
        } else if (plan.queue.isNotEmpty() || plan.playing.isNotEmpty()) {
            Text(stringResource(R.string.backlog_forecast_unknown), color = PlayColors.Muted)
        }
        if (hoursError || writeError) Text(stringResource(R.string.save_error), color = PlayColors.Error)
    }
}

@Composable
private fun AddToBacklogDialog(games: List<Game>, onDismiss: () -> Unit, onAdd: (Long) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val matches = remember(games, query) {
        games.filter { it.title.contains(query.trim(), ignoreCase = true) ||
            it.originalTitle.contains(query.trim(), ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_queue)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PlaySpacing.Gap)) {
                OutlinedTextField(
                    query,
                    { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.search_library)) },
                    singleLine = true
                )
                if (matches.isEmpty()) {
                    Text(stringResource(R.string.no_results), color = PlayColors.Muted)
                } else {
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(matches, key = { it.id }) { game ->
                            TextButton(onClick = { onAdd(game.id) }) { Text(game.title) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@get:StringRes
private val BacklogOrder.label: Int
    get() = when (this) {
        BacklogOrder.MANUAL -> R.string.backlog_manual
        BacklogOrder.SHORTEST_FIRST -> R.string.backlog_shortest_first
        BacklogOrder.LONGEST_FIRST -> R.string.backlog_longest_first
    }
