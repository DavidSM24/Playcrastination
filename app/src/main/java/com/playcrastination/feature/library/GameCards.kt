package com.playcrastination.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.playcrastination.R
import com.playcrastination.core.designsystem.formattedDuration
import com.playcrastination.core.designsystem.GameArtwork
import com.playcrastination.core.designsystem.PlayColors
import com.playcrastination.core.designsystem.PlayProgress
import com.playcrastination.core.designsystem.StatusTag
import com.playcrastination.core.designsystem.label
import com.playcrastination.domain.model.Game
import com.playcrastination.domain.model.remainingMinutesForGoal

@Composable
fun GameRow(game: Game, onClick: () -> Unit, showProgressDetails: Boolean = false) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PlayColors.Panel)
    ) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GameArtwork(game.coverUrl, game.title, Modifier.size(52.dp, 68.dp).clip(MaterialTheme.shapes.small))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(game.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val progress = game.timeProgress()
                val meta = listOfNotNull(game.genres.firstOrNull(),
                    game.platform.ifBlank { game.platforms.firstOrNull().orEmpty() }.takeIf(String::isNotBlank))
                    .joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(meta, color = PlayColors.Muted, style = MaterialTheme.typography.bodySmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                StatusTag(game.status)
                if (game.steam != null) SteamBadge()
                if (progress.percent != null && !game.hasReachedGoal) PlayProgress(progress.percent)
                if (showProgressDetails) {
                    Text(
                        stringResource(R.string.current_choice, stringResource(R.string.goal), stringResource(game.goal.label)),
                        color = PlayColors.LightViolet, style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        stringResource(R.string.playing_recap, formattedDuration(progress.playedMinutes?.toLong()),
                            formattedDuration(game.remainingMinutesForGoal())),
                        color = PlayColors.Muted, style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun GameTile(game: Game, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PlayColors.Panel)
    ) {
        GameArtwork(game.coverUrl, game.title, Modifier.fillMaxWidth().aspectRatio(0.83f))
        Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(game.title, style = MaterialTheme.typography.titleMedium, minLines = 2, maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            StatusTag(game.status)
            if (game.steam != null) SteamBadge()
            val percent = game.timeProgress().percent
            if (percent != null && !game.hasReachedGoal) PlayProgress(percent)
        }
    }
}

@Composable
private fun SteamBadge() {
    Text(stringResource(R.string.steam), color = PlayColors.LightViolet, style = MaterialTheme.typography.labelSmall)
}
