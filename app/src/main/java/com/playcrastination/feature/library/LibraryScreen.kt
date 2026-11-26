package com.playcrastination.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.playcrastination.R
import com.playcrastination.core.designsystem.EmptyPanel
import com.playcrastination.core.designsystem.LoadingContent
import com.playcrastination.core.designsystem.OptionPicker
import com.playcrastination.core.designsystem.PlaySpacing
import com.playcrastination.core.designsystem.ScreenTitle
import com.playcrastination.core.designsystem.label
import com.playcrastination.domain.model.Game
import com.playcrastination.domain.model.GameStatus
import com.playcrastination.domain.model.LibraryCatalog
import com.playcrastination.domain.model.LibraryQuery
import com.playcrastination.domain.model.LibrarySort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val LibraryQuerySaver = listSaver<LibraryQuery, Any>(
    save = {
        listOf(it.text, it.genre.orEmpty(), it.platform.orEmpty(), it.status?.name.orEmpty(),
            it.year ?: 0, it.favoritesOnly, it.sort.name, it.descending)
    },
    restore = {
        LibraryQuery(
            text = it[0] as String,
            genre = (it[1] as String).ifBlank { null },
            platform = (it[2] as String).ifBlank { null },
            status = (it[3] as String).takeIf(String::isNotEmpty)?.let(GameStatus::valueOf),
            year = (it[4] as Int).takeIf { year -> year != 0 },
            favoritesOnly = it[5] as Boolean,
            sort = LibrarySort.valueOf(it[6] as String),
            descending = it[7] as Boolean
        )
    }
)

@Composable
fun LibraryScreen(games: List<Game>, onGame: (Long) -> Unit, onAdd: () -> Unit) {
    var query by rememberSaveable(stateSaver = LibraryQuerySaver) { mutableStateOf(LibraryQuery()) }
    var grid by rememberSaveable { mutableStateOf(true) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showHidden by rememberSaveable { mutableStateOf(false) }
    val available = remember(games, showHidden) { games.filter { (it.steam?.hidden == true) == showHidden } }
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    val locale = LocalConfiguration.current.locales[0]
    val catalog by produceState<LibraryCatalog?>(null, available, locale) {
        value = withContext(Dispatchers.Default) { LibraryCatalog(available, locale) }
    }
    val currentCatalog = catalog
    val filtered by produceState<List<Game>?>(null, currentCatalog, query) {
        if (currentCatalog != null) {
            value = withContext(Dispatchers.Default) { currentCatalog.find(query) }
        }
    }
    val results = filtered
    val contentPadding = PaddingValues(start = PlaySpacing.Inset, end = PlaySpacing.Inset, bottom = PlaySpacing.Inset)
    val header: @Composable () -> Unit = {
        LibraryHeader(query, { query = it }, grid, { grid = it }, { showFilters = true }, onAdd)
        if (showHidden || games.any { it.steam?.hidden == true }) {
            TextButton(onClick = { showHidden = !showHidden }) {
                Text(stringResource(if (showHidden) R.string.steam_show_active else R.string.steam_show_hidden))
            }
        }
    }
    if (grid) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(136.dp),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "library-header", span = { GridItemSpan(maxLineSpan) }) { header() }
            if (results.isNullOrEmpty()) {
                item(key = "library-empty", span = { GridItemSpan(maxLineSpan) }) {
                    LibraryEmptyContent(games.isEmpty(), results == null)
                }
            } else {
                items(results, key = { it.id }) { game -> GameTile(game) { onGame(game.id) } }
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(PlaySpacing.Gap)
        ) {
            item(key = "library-header") { header() }
            if (results.isNullOrEmpty()) {
                item(key = "library-empty") { LibraryEmptyContent(games.isEmpty(), results == null) }
            } else {
                items(results, key = { it.id }) { game -> GameRow(game, { onGame(game.id) }) }
            }
        }
    }
    if (showFilters && currentCatalog != null) {
        LibraryFiltersDialog(currentCatalog, query, { query = it }, { showFilters = false })
    }
}

@Composable
private fun LibraryHeader(
    query: LibraryQuery,
    onQuery: (LibraryQuery) -> Unit,
    grid: Boolean,
    onMode: (Boolean) -> Unit,
    onFilters: () -> Unit,
    onAdd: () -> Unit
) {
    val activeFilters = listOfNotNull(query.genre, query.platform, query.status, query.year).size +
        if (query.favoritesOnly) 1 else 0
    Column(verticalArrangement = Arrangement.spacedBy(PlaySpacing.Gap)) {
        ScreenTitle(stringResource(R.string.library), stringResource(R.string.library_intro))
        OutlinedTextField(
            value = query.text,
            onValueChange = { onQuery(query.copy(text = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.search_library)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(grid, { onMode(true) }, { Text(stringResource(R.string.grid)) })
            FilterChip(!grid, { onMode(false) }, { Text(stringResource(R.string.list)) })
            FilterChip(activeFilters > 0, onFilters, {
                Text(if (activeFilters == 0) stringResource(R.string.filters_sort)
                    else stringResource(R.string.active_filters, activeFilters))
            })
        }
        Button(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(stringResource(R.string.add_game), Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun LibraryEmptyContent(emptyLibrary: Boolean, loading: Boolean) {
    when {
        emptyLibrary -> EmptyPanel(stringResource(R.string.library_empty), stringResource(R.string.library_empty_body))
        loading -> LoadingContent()
        else -> EmptyPanel(stringResource(R.string.no_results), stringResource(R.string.no_results_body))
    }
}

@Composable
private fun LibraryFiltersDialog(
    catalog: LibraryCatalog,
    query: LibraryQuery,
    onChange: (LibraryQuery) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filters_sort)) },
        text = {
            val any = stringResource(R.string.filter_any)
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionPicker(stringResource(R.string.genre), query.genre, listOf(null) + catalog.genres,
                    { it ?: any }) { onChange(query.copy(genre = it)) }
                OptionPicker(stringResource(R.string.platform), query.platform, listOf(null) + catalog.platforms,
                    { it ?: any }) { onChange(query.copy(platform = it)) }
                OptionPicker(stringResource(R.string.status), query.status, listOf(null) + GameStatus.entries,
                    { status -> status?.let { stringResource(it.label) } ?: any }) { onChange(query.copy(status = it)) }
                OptionPicker(stringResource(R.string.year), query.year, listOf(null) + catalog.years,
                    { it?.toString() ?: any }) { onChange(query.copy(year = it)) }
                FilterChip(query.favoritesOnly, { onChange(query.copy(favoritesOnly = !query.favoritesOnly)) },
                    { Text(stringResource(R.string.favorites)) })
                OptionPicker(stringResource(R.string.library_sort), query.sort, LibrarySort.entries,
                    { stringResource(it.label) }) { onChange(query.copy(sort = it)) }
                OptionPicker(stringResource(R.string.sort_direction), query.descending, listOf(false, true),
                    { stringResource(if (it) R.string.descending else R.string.ascending) }) {
                    onChange(query.copy(descending = it))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
        dismissButton = {
            TextButton(onClick = {
                onChange(query.copy(genre = null, platform = null, status = null, year = null, favoritesOnly = false))
            }) { Text(stringResource(R.string.clear_filters)) }
        }
    )
}

@get:StringRes
private val LibrarySort.label: Int
    get() = when (this) {
        LibrarySort.TITLE -> R.string.sort_title
        LibrarySort.PLAYTIME -> R.string.sort_playtime
        LibrarySort.DURATION -> R.string.sort_duration
        LibrarySort.PROGRESS -> R.string.sort_progress
        LibrarySort.ACTIVITY -> R.string.sort_activity
        LibrarySort.RELEASE -> R.string.sort_release
    }
