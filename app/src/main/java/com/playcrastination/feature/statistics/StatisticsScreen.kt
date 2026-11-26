package com.playcrastination.feature.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.playcrastination.R
import com.playcrastination.core.designsystem.formattedDuration
import com.playcrastination.core.designsystem.EmptyPanel
import com.playcrastination.core.designsystem.Panel
import com.playcrastination.core.designsystem.PlayColors
import com.playcrastination.core.designsystem.PlayProgress
import com.playcrastination.core.designsystem.PlaySpacing
import com.playcrastination.core.designsystem.ScreenTitle
import com.playcrastination.core.designsystem.SectionTitle
import com.playcrastination.domain.model.Game

@Composable
fun StatisticsScreen(games: List<Game>) {
    val recorded = remember(games) { games.mapNotNull { it.playedMinutes?.toLong() } }
    LazyColumn(
        contentPadding = PaddingValues(start = PlaySpacing.Inset, end = PlaySpacing.Inset, bottom = PlaySpacing.Inset),
        verticalArrangement = Arrangement.spacedBy(PlaySpacing.Gap)
    ) {
        item { ScreenTitle(stringResource(R.string.statistics), stringResource(R.string.statistics_intro)) }
        if (games.isNotEmpty()) {
            item {
                Panel(Modifier.fillMaxWidth()) {
                    SectionTitle(stringResource(R.string.metric_recorded))
                    Text(formattedDuration(recorded.takeIf { it.isNotEmpty() }?.sum()), color = PlayColors.Cyan)
                    val completed = games.count { it.status.isCompleted }
                    Text(stringResource(R.string.completion_ratio, completed, games.size))
                    PlayProgress((100L * completed / games.size).toInt())
                }
            }
        }
        item { EmptyPanel(stringResource(R.string.history_empty), stringResource(R.string.history_empty_body)) }
    }
}
