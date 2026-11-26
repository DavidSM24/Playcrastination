package com.playcrastination.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.playcrastination.R
import com.playcrastination.core.designsystem.EmptyPanel
import com.playcrastination.core.designsystem.PlaySpacing
import com.playcrastination.core.designsystem.ScreenTitle
import com.playcrastination.domain.model.Game

@Composable
fun LibraryScreen(games: List<Game>, onGame: (Long) -> Unit, onAdd: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var grid by rememberSaveable { mutableStateOf(true) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    val filtered = remember(games, query, favoritesOnly) {
        games.filter { it.title.contains(query.trim(), ignoreCase = true) && (!favoritesOnly || it.favorite) }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = PlaySpacing.Inset),
        verticalArrangement = Arrangement.spacedBy(PlaySpacing.Gap)) {
        ScreenTitle(stringResource(R.string.library), stringResource(R.string.library_intro))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.search_library)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(grid, { grid = true }, { Text(stringResource(R.string.grid)) })
            FilterChip(!grid, { grid = false }, { Text(stringResource(R.string.list)) })
            FilterChip(favoritesOnly, { favoritesOnly = !favoritesOnly }, { Text(stringResource(R.string.favorites)) })
        }
        Button(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(stringResource(R.string.add_game), Modifier.padding(start = 6.dp))
        }
        when {
            games.isEmpty() -> EmptyPanel(stringResource(R.string.library_empty), stringResource(R.string.library_empty_body))
            filtered.isEmpty() -> EmptyPanel(stringResource(R.string.no_results), stringResource(R.string.no_results_body))
            grid -> LazyVerticalGrid(
                columns = GridCells.Adaptive(136.dp),
                contentPadding = PaddingValues(bottom = PlaySpacing.Inset),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { game -> GameTile(game) { onGame(game.id) } }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = PlaySpacing.Inset),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(filtered, key = { it.id }) { game -> GameRow(game, { onGame(game.id) }) }
            }
        }
    }
}
