package com.playcrastination.feature.steam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.playcrastination.R
import com.playcrastination.core.designsystem.GameArtwork
import com.playcrastination.core.designsystem.Panel
import com.playcrastination.core.designsystem.PlayColors
import com.playcrastination.core.designsystem.PlaySpacing
import com.playcrastination.core.designsystem.SectionTitle
import com.playcrastination.core.designsystem.formattedDuration
import com.playcrastination.domain.model.Game
import com.playcrastination.domain.model.GameStatus
import com.playcrastination.domain.model.SteamConnection
import com.playcrastination.domain.model.SteamProblem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun SteamFeedback(ui: SteamUiState) {
    val error = ui.problem
    if (error != null) {
        val label = when (error) {
            SteamProblem.NOT_CONFIGURED -> R.string.steam_not_connected
            SteamProblem.INVALID_INPUT -> R.string.steam_invalid_input
            SteamProblem.KEY_REJECTED -> R.string.steam_access_rejected
            SteamProblem.LIBRARY_PRIVATE -> R.string.steam_library_private
            SteamProblem.PROFILE_MISSING -> R.string.steam_profile_missing
            SteamProblem.UNAVAILABLE -> R.string.steam_unavailable
            SteamProblem.STORAGE -> R.string.steam_storage_error
            SteamProblem.OFFLINE -> R.string.steam_offline
            SteamProblem.RATE_LIMITED -> R.string.steam_rate_limited
            SteamProblem.TEMPORARY -> R.string.steam_temporary_error
        }
        Text(stringResource(label), color = PlayColors.Error)
    }
    val message = when (ui.notice) {
        SteamNotice.SAVED -> stringResource(R.string.steam_saved)
        SteamNotice.DISCONNECTED -> stringResource(R.string.steam_disconnected)
        SteamNotice.VALIDATED -> pluralStringResource(R.plurals.steam_validated, ui.count, ui.count)
        SteamNotice.IMPORTED -> pluralStringResource(R.plurals.steam_imported, ui.count, ui.count)
        SteamNotice.SYNCED -> stringResource(R.string.steam_synced)
        SteamNotice.UPDATED -> stringResource(R.string.steam_updated)
        SteamNotice.CANCELLED -> stringResource(R.string.steam_import_cancelled)
        null -> null
    }
    if (message != null) Text(message, color = PlayColors.Cyan)
}

@Composable
fun SteamDashboardCard(connection: SteamConnection, ui: SteamUiState, viewModel: SteamViewModel) {
    Panel(Modifier.fillMaxWidth()) {
        val profile = connection.profile
        val title = profile?.name?.takeIf(String::isNotBlank) ?: stringResource(R.string.steam)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PlaySpacing.Gap)) {
            if (profile?.avatarUrl != null) {
                GameArtwork(profile.avatarUrl, title, Modifier.size(42.dp).clip(CircleShape))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionTitle(title)
                profile?.currentGame?.let { Text(stringResource(R.string.steam_now_playing, it), color = PlayColors.Cyan) }
                Text(
                    profile?.lastSyncMillis?.let { stringResource(R.string.steam_last_sync, steamTimestamp(it)) }
                        ?: stringResource(R.string.steam_sync_pending),
                    color = PlayColors.Muted, style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Button(enabled = ui.task == null, onClick = viewModel::sync) {
            Text(stringResource(if (ui.task == SteamTask.SYNC) R.string.steam_syncing else R.string.steam_sync))
        }
        if (ui.source == SteamTask.SYNC) SteamFeedback(ui)
    }
}

@Composable
fun SteamGameCard(game: Game, connection: SteamConnection, ui: SteamUiState, viewModel: SteamViewModel) {
    val link = game.steam ?: return
    val accountMatches = connection.settings.steamId == link.steamId && connection.settings.configured
    val ready = ui.task == null
    Panel(Modifier.fillMaxWidth().padding(horizontal = PlaySpacing.Inset)) {
        SectionTitle(stringResource(R.string.steam))
        Text(stringResource(R.string.steam_total_time, formattedDuration(link.playedMinutes?.toLong())))
        link.recentMinutes?.let { Text(stringResource(R.string.steam_recent_time, formattedDuration(it.toLong()))) }
        if ((link.recentMinutes ?: 0) > 0 && game.status in listOf(GameStatus.NOT_STARTED, GameStatus.ON_HOLD)) {
            Text(stringResource(R.string.steam_suggest_playing), color = PlayColors.Muted)
        }
        link.lastPlayedSeconds?.let { Text(stringResource(R.string.steam_last_played, steamTimestamp(it * 1000L))) }
        link.achievements?.let { Text(stringResource(R.string.steam_achievements_count, it.unlocked, it.total)) }
        if (accountMatches) {
            OutlinedButton(enabled = ready, onClick = viewModel::sync) { Text(stringResource(R.string.steam_sync_hours)) }
            OutlinedButton(enabled = ready, onClick = { viewModel.achievements(game.id) }) {
                Text(stringResource(R.string.steam_check_achievements))
            }
        }
        OutlinedButton(enabled = ready, onClick = { viewModel.metadata(game.id) }) {
            Text(stringResource(R.string.steam_fill_metadata))
        }
        OutlinedButton(enabled = ready, onClick = { viewModel.setHidden(game.id, !link.hidden) }) {
            Text(stringResource(if (link.hidden) R.string.steam_unhide_game else R.string.steam_hide_game))
        }
        SteamExternalLink(link.storeUrl, stringResource(R.string.steam_open_store))
        if (ui.task == SteamTask.SYNC || (ui.task == SteamTask.UPDATE_GAME && ui.gameId == game.id)) {
            Text(stringResource(R.string.steam_syncing), color = PlayColors.Cyan)
        }
        if ((accountMatches && ui.source == SteamTask.SYNC) || ui.gameId == game.id) SteamFeedback(ui)
    }
}

@Composable
internal fun SteamExternalLink(url: String, label: String) {
    val uri = LocalUriHandler.current
    var failed by remember(url) { mutableStateOf(false) }
    OutlinedButton(onClick = {
        failed = try {
            uri.openUri(url)
            false
        } catch (_: IllegalArgumentException) {
            true
        }
    }) { Text(label) }
    if (failed) Text(stringResource(R.string.open_link_error), color = PlayColors.Error)
}

@Composable
private fun steamTimestamp(millis: Long): String = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
    .withLocale(LocalConfiguration.current.locales[0])
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(millis))
