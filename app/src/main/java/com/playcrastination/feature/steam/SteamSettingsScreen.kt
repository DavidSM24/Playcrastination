package com.playcrastination.feature.steam

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playcrastination.R
import com.playcrastination.core.designsystem.OptionPicker
import com.playcrastination.core.designsystem.Panel
import com.playcrastination.core.designsystem.PlayColors
import com.playcrastination.core.designsystem.PlaySpacing
import com.playcrastination.core.designsystem.ScreenTitle
import com.playcrastination.core.designsystem.SectionTitle
import com.playcrastination.domain.model.SteamLibrary

@Composable
fun SteamSettingsScreen(viewModel: SteamViewModel, onBack: () -> Unit) {
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val library by viewModel.library.collectAsStateWithLifecycle()
    val settings = connection.settings
    val ready = ui.task == null
    var id by rememberSaveable(settings.steamId) { mutableStateOf(settings.steamId.orEmpty()) }
    var key by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current
    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = PlaySpacing.Gap)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
        }
        LazyColumn(
            contentPadding = PaddingValues(start = PlaySpacing.Inset, end = PlaySpacing.Inset, bottom = PlaySpacing.Section),
            verticalArrangement = Arrangement.spacedBy(PlaySpacing.Gap)
        ) {
            item { ScreenTitle(stringResource(R.string.steam_settings)) }
            item {
                Panel(Modifier.fillMaxWidth()) {
                    SectionTitle(stringResource(R.string.steam_account))
                    Text(stringResource(R.string.steam_privacy), color = PlayColors.Muted)
                    OutlinedTextField(
                        value = id,
                        onValueChange = { id = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.steam_id_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(if (settings.configured) R.string.steam_replace_key else R.string.steam_key_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )
                    Button(enabled = ready && id.isNotBlank() && key.isNotBlank(), onClick = {
                        viewModel.connect(id, key)
                        key = ""
                        focus.clearFocus()
                    }) { Text(stringResource(R.string.steam_save_connection)) }
                    SteamExternalLink("https://steamcommunity.com/dev/apikey", stringResource(R.string.steam_get_key))
                    if (settings.configured) {
                        OutlinedButton(enabled = ready, onClick = viewModel::validate) {
                            Text(stringResource(R.string.steam_validate))
                        }
                        TextButton(enabled = ready, onClick = viewModel::disconnect) {
                            Text(stringResource(R.string.steam_disconnect))
                        }
                    }
                    if (ui.task != null && ui.task != SteamTask.IMPORT) {
                        Text(stringResource(R.string.steam_working), color = PlayColors.Cyan)
                    }
                    if (ui.source != SteamTask.UPDATE_GAME) SteamFeedback(ui)
                }
            }
            if (settings.configured) {
                item {
                    Panel(Modifier.fillMaxWidth()) {
                        SectionTitle(stringResource(R.string.steam_library))
                        OutlinedButton(enabled = ready, onClick = { viewModel.loadLibrary(false) }) {
                            Text(stringResource(R.string.steam_load_owned))
                        }
                        OutlinedButton(enabled = ready, onClick = { viewModel.loadLibrary(true) }) {
                            Text(stringResource(R.string.steam_load_recent))
                        }
                        Text(stringResource(R.string.steam_recent_suggestion), color = PlayColors.Muted)
                    }
                }
                item {
                    Panel(Modifier.fillMaxWidth()) {
                        SectionTitle(stringResource(R.string.steam_sync))
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.steam_automatic)) },
                            trailingContent = { Switch(settings.automatic, viewModel::setAutomatic, enabled = ready) }
                        )
                        if (settings.automatic && ready) {
                            OptionPicker(
                                stringResource(R.string.steam_interval), settings.intervalMinutes,
                                (listOf(15, 30, 60, 120, 360, 1440) + settings.intervalMinutes).distinct().sorted(),
                                { stringResource(R.string.steam_interval_value, it) }, viewModel::setInterval
                            )
                        }
                        Button(enabled = ready, onClick = viewModel::sync) { Text(stringResource(R.string.steam_sync_now)) }
                    }
                }
            }
        }
    }
    library?.let { SteamImportDialog(it, ui, viewModel) }
}

@Composable
private fun SteamImportDialog(library: SteamLibrary, ui: SteamUiState, viewModel: SteamViewModel) {
    var selected by remember(library) { mutableStateOf(emptySet<Long>()) }
    var query by remember(library) { mutableStateOf("") }
    val games = remember(library, query) { library.games.filter { it.name.contains(query.trim(), ignoreCase = true) } }
    val busy = ui.task == SteamTask.IMPORT
    AlertDialog(
        onDismissRequest = viewModel::dismissLibrary,
        title = { Text(stringResource(R.string.steam_import)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PlaySpacing.Gap)) {
                if (busy) {
                    Text(stringResource(R.string.steam_import_progress, ui.count, ui.total))
                    LinearProgressIndicator(progress = { if (ui.total == 0) 0f else ui.count.toFloat() / ui.total },
                        modifier = Modifier.fillMaxWidth())
                } else if (library.games.isEmpty()) {
                    Text(stringResource(R.string.steam_no_games))
                } else {
                    OutlinedTextField(
                        query, { query = it }, modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.search_library)) }, singleLine = true
                    )
                    LazyColumn(Modifier.heightIn(max = 300.dp)) {
                        items(games, key = { it.appId }) { game ->
                            val change = {
                                selected = if (game.appId in selected) selected - game.appId else selected + game.appId
                            }
                            ListItem(
                                headlineContent = { Text(game.name) },
                                modifier = Modifier.clickable(onClick = change),
                                trailingContent = { Checkbox(game.appId in selected, onCheckedChange = null) }
                            )
                        }
                    }
                    TextButton(onClick = { viewModel.import(library.games.mapTo(mutableSetOf()) { it.appId }) }) {
                        Text(stringResource(R.string.steam_import_all))
                    }
                }
                SteamFeedback(ui)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && selected.isNotEmpty(), onClick = { viewModel.import(selected) }) {
                Text(stringResource(R.string.steam_import_selected, selected.size))
            }
        },
        dismissButton = {
            TextButton(onClick = if (busy) viewModel::cancelImport else viewModel::dismissLibrary) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
