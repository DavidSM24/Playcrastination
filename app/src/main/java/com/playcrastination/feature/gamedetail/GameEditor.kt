package com.playcrastination.feature.gamedetail

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.net.toUri
import com.playcrastination.R
import com.playcrastination.core.designsystem.OptionPicker
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
    var originalTitle by rememberSaveable(original.id) { mutableStateOf(original.originalTitle) }
    var year by rememberSaveable(original.id) { mutableStateOf(original.releaseYear?.toString().orEmpty()) }
    var releasedOn by rememberSaveable(original.id) { mutableStateOf(original.releaseDate?.toString().orEmpty()) }
    var developer by rememberSaveable(original.id) { mutableStateOf(original.developer) }
    var publisher by rememberSaveable(original.id) { mutableStateOf(original.publisher) }
    var genre by rememberSaveable(original.id) { mutableStateOf(original.genre) }
    var platform by rememberSaveable(original.id) { mutableStateOf(original.platform) }
    var availablePlatforms by rememberSaveable(original.id) { mutableStateOf(original.availablePlatforms) }
    var cover by rememberSaveable(original.id) { mutableStateOf(original.coverUrl) }
    var banner by rememberSaveable(original.id) { mutableStateOf(original.bannerUrl) }
    var background by rememberSaveable(original.id) { mutableStateOf(original.backgroundUrl) }
    var description by rememberSaveable(original.id) { mutableStateOf(original.description) }
    var notes by rememberSaveable(original.id) { mutableStateOf(original.notes) }
    var rating by rememberSaveable(original.id) { mutableStateOf(original.rating?.toString().orEmpty()) }
    var playthrough by rememberPlaythroughDraft(original.currentPlaythrough)
    var main by rememberSaveable(original.id) { mutableStateOf(original.durations.mainMinutes?.toString().orEmpty()) }
    var extras by rememberSaveable(original.id) { mutableStateOf(original.durations.extrasMinutes?.toString().orEmpty()) }
    var all by rememberSaveable(original.id) { mutableStateOf(original.durations.completionistMinutes?.toString().orEmpty()) }
    var status by rememberSaveable(original.id) { mutableStateOf(original.status) }
    var goal by rememberSaveable(original.id) { mutableStateOf(original.goal) }
    var priority by rememberSaveable(original.id) { mutableStateOf(original.priority) }
    var favorite by rememberSaveable(original.id) { mutableStateOf(original.favorite) }

    val validNumbers = (releasedOn.isNotBlank() || year.validNumber(1..9999)) && rating.validNumber(1..10) &&
        listOf(main, extras, all).all { it.validNumber(1..Int.MAX_VALUE) }
    val current = playthrough.validated(original.currentPlaythrough.id)
    val canSave = title.isNotBlank() && validNumbers && releasedOn.validDate() && current != null &&
        listOf(cover, banner, background).all(String::validImageUrl)

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = PlaySpacing.Inset, end = PlaySpacing.Inset, bottom = PlaySpacing.Gap),
            verticalArrangement = Arrangement.spacedBy(PlaySpacing.Gap)
        ) {
            item { ScreenTitle(stringResource(if (original.id == 0L) R.string.add_game else R.string.edit_game)) }
            item {
                Panel(Modifier.fillMaxWidth()) {
                    SectionTitle(stringResource(R.string.general_info))
                    EditField(title, { title = it }, R.string.game_name)
                    EditField(originalTitle, { originalTitle = it }, R.string.original_title)
                    DateField(releasedOn, { releasedOn = it }, R.string.release_date)
                    if (releasedOn.isBlank()) EditField(year, { year = it }, R.string.year, 1..9999)
                    EditField(developer, { developer = it }, R.string.developer)
                    EditField(publisher, { publisher = it }, R.string.publisher)
                    EditField(description, { description = it }, R.string.description, multiline = true)
                }
            }
            item {
                Panel(Modifier.fillMaxWidth()) {
                    SectionTitle(stringResource(R.string.catalog_info))
                    EditField(genre, { genre = it }, R.string.genres_list, hint = R.string.catalog_list_hint)
                    EditField(platform, { platform = it }, R.string.played_platform)
                    EditField(availablePlatforms, { availablePlatforms = it }, R.string.available_platforms,
                        hint = R.string.catalog_list_hint)
                }
            }
            item {
                Panel(Modifier.fillMaxWidth()) {
                    SectionTitle(stringResource(R.string.artwork))
                    EditField(cover, { cover = it }, R.string.cover_url, isUrl = true)
                    EditField(banner, { banner = it }, R.string.banner_url, isUrl = true)
                    EditField(background, { background = it }, R.string.background_url, isUrl = true)
                }
            }
            item {
                Panel(Modifier.fillMaxWidth()) {
                    SectionTitle(stringResource(R.string.personal_info))
                    OptionPicker(stringResource(R.string.status), status, GameStatus.entries,
                        { stringResource(it.label) }) { status = it }
                    OptionPicker(stringResource(R.string.goal), goal, CompletionGoal.entries,
                        { stringResource(it.label) }) { goal = it }
                    OptionPicker(stringResource(R.string.priority), priority, Priority.entries,
                        { stringResource(it.label) }) { priority = it }
                    EditField(rating, { rating = it }, R.string.rating_input, 1..10)
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.favorite)) },
                        trailingContent = { Checkbox(checked = favorite, onCheckedChange = { favorite = it }) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    EditField(notes, { notes = it }, R.string.notes, multiline = true)
                }
            }
            item {
                Panel(Modifier.fillMaxWidth()) {
                    SectionTitle(stringResource(R.string.current_playthrough))
                    PlaythroughFields(playthrough) { playthrough = it }
                }
            }
            item {
                Panel(Modifier.fillMaxWidth()) {
                    SectionTitle(stringResource(R.string.time_breakdown))
                    TimeBreakdownFields(playthrough) { playthrough = it }
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
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = PlaySpacing.Inset, vertical = PlaySpacing.Gap)) {
            if (attemptFailed) Text(stringResource(R.string.save_error), color = PlayColors.Error)
            Button(enabled = canSave && !busy, onClick = {
                val validatedPlaythrough = current ?: return@Button
                onSave(original.copy(
                    title = title,
                    originalTitle = originalTitle,
                    releaseYear = releasedOn.parsedDate()?.year ?: year.trim().toIntOrNull(),
                    releaseDate = releasedOn.parsedDate(),
                    developer = developer,
                    publisher = publisher,
                    genre = genre,
                    platform = platform,
                    availablePlatforms = availablePlatforms,
                    coverUrl = cover,
                    bannerUrl = banner,
                    backgroundUrl = background,
                    description = description,
                    notes = notes,
                    rating = rating.trim().toIntOrNull(),
                    currentPlaythrough = validatedPlaythrough,
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

internal fun String.validNumber(range: IntRange): Boolean =
    isBlank() || trim().toIntOrNull()?.let { it in range } == true

private fun String.validImageUrl(): Boolean {
    if (isBlank()) return true
    val url = trim().toUri()
    return url.scheme.equals("https", ignoreCase = true) && !url.host.isNullOrBlank() && url.userInfo == null
}

@Composable
internal fun EditField(
    value: String,
    onChange: (String) -> Unit,
    @StringRes label: Int,
    numericRange: IntRange? = null,
    isUrl: Boolean = false,
    multiline: Boolean = false,
    @StringRes validationError: Int? = null,
    @StringRes hint: Int? = null
) {
    val error = validationError ?: when {
        isUrl && !value.validImageUrl() -> R.string.invalid_image_url
        numericRange?.let { !value.validNumber(it) } == true -> R.string.invalid_number
        else -> null
    }
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
        isError = error != null,
        supportingText = (error ?: hint)?.let { message -> { Text(stringResource(message)) } },
        singleLine = !multiline,
        minLines = if (multiline) 2 else 1
    )
}
