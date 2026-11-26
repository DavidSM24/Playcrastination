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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.playcrastination.R
import com.playcrastination.core.designsystem.BrandMark
import com.playcrastination.core.designsystem.formattedDuration
import com.playcrastination.core.designsystem.EmptyPanel
import com.playcrastination.core.designsystem.OrbitArtwork
import com.playcrastination.core.designsystem.Panel
import com.playcrastination.core.designsystem.PlayColors
import com.playcrastination.core.designsystem.PlaySpacing
import com.playcrastination.core.designsystem.SectionTitle
import com.playcrastination.domain.model.Game
import com.playcrastination.domain.model.GameStatus
import com.playcrastination.domain.model.remainingTime
import com.playcrastination.feature.library.GameRow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun DashboardScreen(games: List<Game>, hours: Int, onGame: (Long) -> Unit, onAdd: () -> Unit) {
    val playing = remember(games) { games.filter { it.status == GameStatus.PLAYING } }
    val queue = remember(games) { games.filter { it.backlogPosition != null }.sortedBy { it.backlogPosition } }
    val backlogTime = remember(queue) { queue.remainingTime() }
    val playingTime = remember(playing) { playing.remainingTime() }
    val recorded = remember(games) { games.mapNotNull { it.playedMinutes?.toLong() } }
    LazyColumn(
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
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Metric(games.size.toString(), stringResource(R.string.metric_games), Modifier.weight(1f))
                Metric(playing.size.toString(), stringResource(R.string.metric_playing), Modifier.weight(1f))
                Metric(games.count { it.status.isCompleted }.toString(), stringResource(R.string.metric_done), Modifier.weight(1f))
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.pile_of_shame))
                Text(pluralStringResource(R.plurals.queue_game_count, queue.size, queue.size), color = PlayColors.LightViolet)
                if (queue.isNotEmpty()) {
                    Text(stringResource(R.string.known_remaining, formattedDuration(backlogTime.knownMinutes)))
                    if (backlogTime.unknownGames > 0) {
                        Text(pluralStringResource(R.plurals.missing_estimates, backlogTime.unknownGames,
                            backlogTime.unknownGames), color = PlayColors.Muted)
                    }
                    val activeOutsideQueue = playing.filter { it.backlogPosition == null }
                    val schedule = (queue + activeOutsideQueue).remainingTime()
                    schedule.finishDate(LocalDate.now(), hours)?.let { finish ->
                        val locale = LocalConfiguration.current.locales[0]
                        val date = finish.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
                        Text(stringResource(R.string.forecast_finish, date, hours), color = PlayColors.Cyan)
                    }
                } else {
                    Text(stringResource(R.string.queue_empty_body), color = PlayColors.Muted)
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
        items(playing, key = { "playing-${it.id}" }) { game -> GameRow(game, { onGame(game.id) }) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle(stringResource(R.string.up_next))
                if (queue.isEmpty()) Text(stringResource(R.string.queue_empty), color = PlayColors.Muted)
            }
        }
        items(queue.take(3), key = { "queue-${it.id}" }) { game -> GameRow(game, { onGame(game.id) }) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Metric(formattedDuration(recorded.takeIf { it.isNotEmpty() }?.sum()), stringResource(R.string.metric_recorded),
                    Modifier.weight(1f))
                Metric(formattedDuration(playingTime.knownMinutes), stringResource(R.string.metric_active_remaining),
                    Modifier.weight(1f))
            }
        }
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
