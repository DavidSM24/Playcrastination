package com.playcrastination.feature.backlog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.playcrastination.R
import com.playcrastination.core.designsystem.formattedDuration
import com.playcrastination.core.designsystem.EmptyPanel
import com.playcrastination.core.designsystem.Panel
import com.playcrastination.core.designsystem.PlayColors
import com.playcrastination.core.designsystem.PlaySpacing
import com.playcrastination.core.designsystem.ScreenTitle
import com.playcrastination.domain.model.Game
import com.playcrastination.domain.model.remainingTime
import com.playcrastination.feature.library.GameRow

@Composable
fun BacklogScreen(games: List<Game>, onGame: (Long) -> Unit, onLibrary: () -> Unit) {
    val queue = remember(games) { games.filter { it.backlogPosition != null }.sortedBy { it.backlogPosition } }
    val remaining = remember(queue) { queue.remainingTime() }
    LazyColumn(
        contentPadding = PaddingValues(start = PlaySpacing.Inset, end = PlaySpacing.Inset, bottom = PlaySpacing.Inset),
        verticalArrangement = Arrangement.spacedBy(PlaySpacing.Gap)
    ) {
        item { ScreenTitle(stringResource(R.string.backlog), stringResource(R.string.backlog_intro)) }
        if (queue.isEmpty()) {
            item {
                EmptyPanel(stringResource(R.string.queue_empty), stringResource(R.string.queue_empty_body),
                    stringResource(R.string.open_library), onLibrary)
            }
        } else {
            item {
                Panel(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.known_remaining, formattedDuration(remaining.knownMinutes)))
                    if (remaining.unknownGames > 0) {
                        Text(pluralStringResource(R.plurals.missing_estimates, remaining.unknownGames,
                            remaining.unknownGames), color = PlayColors.Muted)
                    }
                }
            }
            itemsIndexed(queue, key = { _, game -> game.id }) { index, game ->
                GameRow(game, { onGame(game.id) }, position = index + 1)
            }
        }
    }
}
