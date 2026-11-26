package com.playcrastination.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playcrastination.R
import com.playcrastination.core.designsystem.Panel
import com.playcrastination.core.designsystem.PlayColors
import com.playcrastination.core.designsystem.PlaySpacing
import com.playcrastination.core.designsystem.ScreenTitle
import com.playcrastination.core.designsystem.SectionTitle

@Composable
fun SettingsScreen(hours: Int, viewModel: SettingsViewModel = hiltViewModel(), onSteamSettings: () -> Unit) {
    val error by viewModel.writeError.collectAsStateWithLifecycle()
    LazyColumn(
        contentPadding = PaddingValues(start = PlaySpacing.Inset, end = PlaySpacing.Inset, bottom = PlaySpacing.Inset),
        verticalArrangement = Arrangement.spacedBy(PlaySpacing.Gap)
    ) {
        item { ScreenTitle(stringResource(R.string.settings), stringResource(R.string.settings_intro)) }
        item {
            Panel(Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.steam))
                FilledTonalButton(onClick = onSteamSettings) { Text(stringResource(R.string.steam_settings)) }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.play_budget))
                Text(stringResource(R.string.play_budget_body), color = PlayColors.Muted)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(enabled = hours > 1, onClick = { viewModel.setDailyHours(hours - 1) }) {
                        Text(stringResource(R.string.less_hours))
                    }
                    Text(stringResource(R.string.hours_per_day, hours), style = MaterialTheme.typography.titleMedium)
                    FilledTonalButton(enabled = hours < 24, onClick = { viewModel.setDailyHours(hours + 1) }) {
                        Text(stringResource(R.string.more_hours))
                    }
                }
                if (error) Text(stringResource(R.string.save_error), color = PlayColors.Error)
            }
        }
    }
}
