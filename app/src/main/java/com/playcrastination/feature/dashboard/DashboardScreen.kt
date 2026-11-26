package com.playcrastination.feature.dashboard

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playcrastination.R
import com.playcrastination.core.designsystem.BrandMark
import com.playcrastination.core.designsystem.DailyHoursPicker
import com.playcrastination.core.designsystem.formattedDate
import com.playcrastination.core.designsystem.formattedDuration
import com.playcrastination.core.designsystem.EmptyPanel
import com.playcrastination.core.designsystem.OrbitArtwork
import com.playcrastination.core.designsystem.Panel
import com.playcrastination.core.designsystem.PlayColors
import com.playcrastination.core.designsystem.PlaySpacing
import com.playcrastination.core.designsystem.SectionTitle
import com.playcrastination.domain.model.BacklogOrder
import com.playcrastination.domain.model.Game
import com.playcrastination.domain.model.GameStatus
import com.playcrastination.domain.model.planBacklog
import com.playcrastination.feature.library.GameRow
import com.playcrastination.feature.steam.SteamDashboardCard
import com.playcrastination.feature.steam.SteamTask
import com.playcrastination.feature.steam.SteamViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    games: List<Game>,
    hours: Int,
    onGame: (Long) -> Unit,
    onAdd: () -> Unit,
    onHoursChange: (Int) -> Unit,
    hoursError: Boolean,
    steam: SteamViewModel
) {
    val steamConnection by steam.connection.collectAsStateWithLifecycle()
    val steamUi by steam.ui.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val today = LocalDate.now()
    val plan = remember(games, hours, today) { planBacklog(games, BacklogOrder.MANUAL, today, hours) }
    val playing = plan.playing
    val upNext = plan.queue.filter { it.game.status != GameStatus.PLAYING && !it.game.hasReachedGoal }.take(3)
    val recorded = remember(games) { games.mapNotNull(Game::recordedMinutes) }
    val content: @Composable () -> Unit = {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = PlaySpacing.Inset, end = PlaySpacing.Inset, bottom = PlaySpacing.Section),
            verticalArrangement = Arrangement.spacedBy(PlaySpacing.Gap)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BrandMark(Modifier.size(27.dp))
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
                }
            }
            item { DashboardHero(games.isEmpty(), onAdd) }
            if (steamConnection.settings.configured) {
                item { SteamDashboardCard(steamConnection, steamUi, steam) }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Metric(games.count { it.steam?.hidden != true }.toString(), stringResource(R.string.metric_games), Modifier.weight(1f))
                    Metric(games.count { it.status == GameStatus.NOT_STARTED || it.status == GameStatus.ON_HOLD }.toString(),
                        stringResource(R.string.metric_pending), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Metric(playing.size.toString(), stringResource(R.string.metric_playing), Modifier.weight(1f))
                    Metric(games.count { it.status.isCompleted }.toString(), stringResource(R.string.metric_done), Modifier.weight(1f))
                }
            }
            item {
                Panel(Modifier.fillMaxWidth()) {
                    SectionTitle(stringResource(R.string.pile_of_shame))
                    Text(pluralStringResource(R.plurals.queue_game_count, plan.queue.size, plan.queue.size),
                        color = PlayColors.LightViolet)
                    if (plan.queue.isNotEmpty()) {
                        Text(stringResource(R.string.known_remaining, formattedDuration(plan.queueTime.knownMinutes)))
                        if (plan.queueTime.unknownGames > 0) {
                            Text(pluralStringResource(R.plurals.missing_estimates, plan.queueTime.unknownGames,
                                plan.queueTime.unknownGames), color = PlayColors.Muted)
                        }
                    } else {
                        Text(stringResource(R.string.queue_empty_body), color = PlayColors.Muted)
                    }
                    if (plan.queue.isNotEmpty() || plan.playing.isNotEmpty()) {
                        DailyHoursPicker(hours, onHoursChange)
                        val finish = plan.finishDate
                        if (finish != null) {
                            Text(stringResource(R.string.forecast_finish, formattedDate(finish), hours), color = PlayColors.Cyan)
                        } else {
                            Text(stringResource(R.string.backlog_forecast_unknown), color = PlayColors.Muted)
                        }
                        if (hoursError) Text(stringResource(R.string.save_error), color = PlayColors.Error)
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle(stringResource(R.string.continue_playing))
                    if (playing.isEmpty()) {
                        EmptyPanel(stringResource(R.string.playing_empty), stringResource(R.string.playing_empty_body))
                    }
                }
            }
            items(playing, key = { "playing-${it.game.id}" }) { active ->
                GameRow(active.game, { onGame(active.game.id) }, showProgressDetails = true)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionTitle(stringResource(R.string.up_next))
                    if (upNext.isEmpty()) Text(stringResource(R.string.no_upcoming_games), color = PlayColors.Muted)
                }
            }
            items(upNext, key = { "queue-${it.game.id}" }) { upcoming -> GameRow(upcoming.game, { onGame(upcoming.game.id) }) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Metric(formattedDuration(recorded.takeIf { it.isNotEmpty() }?.sum()), stringResource(R.string.metric_recorded),
                        Modifier.weight(1f))
                    Metric(formattedDuration(plan.playingTime.knownMinutes), stringResource(R.string.metric_active_remaining),
                        Modifier.weight(1f))
                }
            }
        }
    }
    if (steamConnection.settings.configured) {
        PullToRefreshBox(
            isRefreshing = steamUi.task == SteamTask.SYNC,
            onRefresh = steam::sync,
            content = { content() }
        )
    } else {
        content()
    }
}

@Composable
private fun DashboardHero(isEmpty: Boolean, onAdd: () -> Unit) {
    val shape = MaterialTheme.shapes.large
    Box(
        Modifier.fillMaxWidth().heightIn(min = 224.dp).clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF241840), Color(0xFF181832), Color(0xFF132741))))
            .border(1.dp, PlayColors.LightViolet.copy(alpha = 0.28f), shape)
    ) {
        OrbitArtwork(Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            listOf(Color(0xD922163B), Color(0x6022173B), Color.Transparent)
        )))
        Column(
            Modifier.align(Alignment.CenterStart).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.dashboard_hero), style = MaterialTheme.typography.displaySmall)
            Text(stringResource(R.string.tagline), color = PlayColors.LightViolet, style = MaterialTheme.typography.bodySmall)
            if (isEmpty) {
                Button(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(stringResource(R.string.add_game), Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun Metric(value: String, caption: String, modifier: Modifier = Modifier) {
    Panel(modifier.heightIn(min = 87.dp)) {
        Text(value, color = PlayColors.Text, style = MaterialTheme.typography.titleLarge)
        Text(caption, color = PlayColors.Muted, style = MaterialTheme.typography.labelSmall)
    }
}
