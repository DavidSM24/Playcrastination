package com.playcrastination.feature.gamedetail

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.net.toUri
import com.playcrastination.R
import com.playcrastination.core.designsystem.Panel
import com.playcrastination.core.designsystem.PlayColors
import com.playcrastination.core.designsystem.PlaySpacing
import com.playcrastination.core.designsystem.ScreenTitle
import com.playcrastination.core.designsystem.SectionTitle
import com.playcrastination.core.designsystem.label
import com.playcrastination.domain.model.CompletionGoal
import com.playcrastination.domain.model.Durations
import com.playcrastination.domain.model.Game
import com.playcrastination.domain.model.GameStatus
import com.playcrastination.domain.model.Priority

@Composable
fun GameEditor(original: Game, busy: Boolean, attemptFailed: Boolean, onSave: (Game) -> Unit) {
    var title by rememberSaveable(original.id) { mutableStateOf(original.title) }
    var year by rememberSaveable(original.id) { mutableStateOf(original.releaseYear?.toString().orEmpty()) }
    var genre by rememberSaveable(original.id) { mutableStateOf(original.genre) }
    var platform by rememberSaveable(original.id) { mutableStateOf(original.platform) }
    var cover by rememberSaveable(original.id) { mutableStateOf(original.coverUrl) }
    var banner by rememberSaveable(original.id) { mutableStateOf(original.bannerUrl) }
    var description by rememberSaveable(original.id) { mutableStateOf(original.description) }
    var notes by rememberSaveable(original.id) { mutableStateOf(original.notes) }
    var played by rememberSaveable(original.id) { mutableStateOf(original.playedMinutes?.toString().orEmpty()) }
    var narrative by rememberSaveable(original.id) { mutableStateOf(original.narrativePercent?.toString().orEmpty()) }
    var main by rememberSaveable(original.id) { mutableStateOf(original.durations.mainMinutes?.toString().orEmpty()) }
    var extras by rememberSaveable(original.id) { mutableStateOf(original.durations.extrasMinutes?.toString().orEmpty()) }
    var all by rememberSaveable(original.id) { mutableStateOf(original.durations.completionistMinutes?.toString().orEmpty()) }
    var status by rememberSaveable(original.id) { mutableStateOf(original.status) }
    var goal by rememberSaveable(original.id) { mutableStateOf(original.goal) }
    var priority by rememberSaveable(original.id) { mutableStateOf(original.priority) }
    var favorite by rememberSaveable(original.id) { mutableStateOf(original.favorite) }

    val validNumbers = year.validNumber(1..9999) && played.validNumber(0..Int.MAX_VALUE) &&
        narrative.validNumber(0..100) && listOf(main, extras, all).all { it.validNumber(1..Int.MAX_VALUE) }
    val canSave = title.isNotBlank() && validNumbers && cover.validImageUrl() && banner.validImageUrl()

    LazyColumn(
        contentPadding = PaddingValues(start = PlaySpacing.Inset, end = PlaySpacing.Inset, bottom = PlaySpacing.Section),
        verticalArrangement = Arrangement.spacedBy(PlaySpacing.Gap)
    ) {
        item { ScreenTitle(stringResource(if (original.id == 0L) R.string.add_game else R.string.edit_game)) }
        item {
            Panel(Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.general_info))
                EditField(title, { title = it }, R.string.game_name)
                EditField(year, { year = it }, R.string.year, 1..9999)
                EditField(genre, { genre = it }, R.string.genre)
                EditField(platform, { platform = it }, R.string.platform)
                EditField(cover, { cover = it }, R.string.cover_url, isUrl = true)
                EditField(banner, { banner = it }, R.string.banner_url, isUrl = true)
                EditField(description, { description = it }, R.string.description, multiline = true)
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.personal_progress))
                Choice(R.string.status, status, GameStatus.entries, { it.label }) { status = it }
                Choice(R.string.goal, goal, CompletionGoal.entries, { it.label }) { goal = it }
                EditField(played, { played = it }, R.string.played_input, 0..Int.MAX_VALUE)
                EditField(narrative, { narrative = it }, R.string.narrative_input, 0..100)
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.duration_inputs))
                EditField(main, { main = it }, R.string.goal_main, 1..Int.MAX_VALUE)
                EditField(extras, { extras = it }, R.string.goal_extras, 1..Int.MAX_VALUE)
                EditField(all, { all = it }, R.string.goal_all, 1..Int.MAX_VALUE)
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.personal_info))
                Choice(R.string.priority, priority, Priority.entries, { it.label }) { priority = it }
                ListItem(
                    headlineContent = { Text(stringResource(R.string.favorite)) },
                    trailingContent = { Checkbox(checked = favorite, onCheckedChange = { favorite = it }) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                EditField(notes, { notes = it }, R.string.notes, multiline = true)
            }
        }
        if (attemptFailed) item { Text(stringResource(R.string.save_error), color = PlayColors.Error) }
        item {
            Button(enabled = canSave && !busy, onClick = {
                onSave(original.copy(
                    title = title,
                    releaseYear = year.trim().toIntOrNull(),
                    genre = genre,
                    platform = platform,
                    coverUrl = cover,
                    bannerUrl = banner,
                    description = description,
                    notes = notes,
                    playedMinutes = played.trim().toIntOrNull(),
                    narrativePercent = narrative.trim().toIntOrNull(),
                    status = status,
                    goal = goal,
                    priority = priority,
                    favorite = favorite,
                    durations = Durations(main.trim().toIntOrNull(), extras.trim().toIntOrNull(), all.trim().toIntOrNull())
                ))
            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
        }
    }
}

private fun String.validNumber(range: IntRange): Boolean =
    isBlank() || trim().toIntOrNull()?.let { it in range } == true

private fun String.validImageUrl(): Boolean {
    if (isBlank()) return true
    val url = trim().toUri()
    return url.scheme.equals("https", ignoreCase = true) && !url.host.isNullOrBlank() && url.userInfo == null
}

@Composable
private fun EditField(
    value: String,
    onChange: (String) -> Unit,
    @StringRes label: Int,
    numericRange: IntRange? = null,
    isUrl: Boolean = false,
    multiline: Boolean = false
) {
    val error = numericRange?.let { !value.validNumber(it) } == true || (isUrl && !value.validImageUrl())
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = when {
            numericRange != null -> KeyboardType.Number
            isUrl -> KeyboardType.Uri
            else -> KeyboardType.Text
        }),
        isError = error,
        supportingText = if (error) {
            { Text(stringResource(if (isUrl) R.string.invalid_image_url else R.string.invalid_number)) }
        } else null,
        singleLine = !multiline,
        minLines = if (multiline) 2 else 1
    )
}

@Composable
private fun <T> Choice(
    @StringRes label: Int,
    selection: T,
    options: List<T>,
    optionLabel: (T) -> Int,
    onSelection: (T) -> Unit
) {
    var isOpen by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { isOpen = true }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.current_choice, stringResource(label), stringResource(optionLabel(selection))))
    }
    DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
        options.forEach { option ->
            DropdownMenuItem(text = { Text(stringResource(optionLabel(option))) }, onClick = {
                onSelection(option)
                isOpen = false
            })
        }
    }
}
