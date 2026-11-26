package com.playcrastination.core.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.playcrastination.R
import com.playcrastination.core.designsystem.EmptyPanel
import com.playcrastination.core.designsystem.ErrorPanel
import com.playcrastination.core.designsystem.LoadingContent
import com.playcrastination.core.designsystem.PlayColors
import com.playcrastination.core.designsystem.PlaySpacing
import com.playcrastination.core.designsystem.ScreenTransitionMillis
import com.playcrastination.feature.backlog.BacklogScreen
import com.playcrastination.feature.dashboard.DashboardScreen
import com.playcrastination.feature.gamedetail.GameAction
import com.playcrastination.feature.gamedetail.GameDetailScreen
import com.playcrastination.feature.gamedetail.GameEditor
import com.playcrastination.feature.gamedetail.GameState
import com.playcrastination.feature.gamedetail.GameViewModel
import com.playcrastination.feature.library.LibraryScreen
import com.playcrastination.feature.library.LibraryState
import com.playcrastination.feature.library.LibraryViewModel
import com.playcrastination.feature.settings.SettingsScreen
import com.playcrastination.feature.statistics.StatisticsScreen

private enum class Section(@StringRes val title: Int, val icon: ImageVector) {
    DASHBOARD(R.string.dashboard, Icons.Default.Home),
    LIBRARY(R.string.library, Icons.AutoMirrored.Filled.List),
    BACKLOG(R.string.backlog, Icons.Default.DateRange),
    STATISTICS(R.string.statistics, Icons.Default.Star),
    SETTINGS(R.string.settings, Icons.Default.Settings);

    val route: String get() = name
}

private const val DetailRoute = "game/{gameId}"
private const val EditorRoute = "edit/{gameId}"
private val gameArgument = navArgument("gameId") { type = NavType.LongType }

@Composable
fun PlaycrastinationApp(viewModel: LibraryViewModel = hiltViewModel()) {
    val navigation = rememberNavController()
    val currentEntry by navigation.currentBackStackEntryAsState()
    val selected = Section.entries.firstOrNull { it.route == currentEntry?.destination?.route }
    val library by viewModel.state.collectAsStateWithLifecycle()
    val addGame = { navigation.navigate("edit/0") { launchSingleTop = true } }
    val showGame: (Long) -> Unit = { id -> navigation.navigate("game/$id") { launchSingleTop = true } }

    Scaffold(
        containerColor = PlayColors.Background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (selected != null) {
                NavigationBar(containerColor = PlayColors.Navigation) {
                    for (section in Section.entries) {
                        NavigationBarItem(
                            selected = selected == section,
                            onClick = { if (selected != section) navigation.openSection(section) },
                            icon = { Icon(section.icon, contentDescription = null) },
                            label = {
                                Text(stringResource(section.title), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PlayColors.Cyan, selectedTextColor = PlayColors.Text,
                                indicatorColor = PlayColors.Surface, unselectedIconColor = PlayColors.Muted,
                                unselectedTextColor = PlayColors.Muted
                            )
                        )
                    }
                }
            }
        }
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.TopCenter) {
            NavHost(
                navController = navigation,
                startDestination = Section.DASHBOARD.route,
                modifier = Modifier.widthIn(max = PlaySpacing.ContentWidth).fillMaxSize(),
                enterTransition = { fadeIn(tween(ScreenTransitionMillis)) },
                exitTransition = { fadeOut(tween(ScreenTransitionMillis)) }
            ) {
                composable(Section.DASHBOARD.route) {
                    LibraryContent(library, viewModel::retry) { DashboardScreen(it.games, it.dailyHours, showGame, addGame) }
                }
                composable(Section.LIBRARY.route) {
                    LibraryContent(library, viewModel::retry) { LibraryScreen(it.games, showGame, addGame) }
                }
                composable(Section.BACKLOG.route) {
                    LibraryContent(library, viewModel::retry) {
                        BacklogScreen(it.games, showGame) { navigation.openSection(Section.LIBRARY) }
                    }
                }
                composable(Section.STATISTICS.route) {
                    LibraryContent(library, viewModel::retry) { StatisticsScreen(it.games) }
                }
                composable(Section.SETTINGS.route) {
                    LibraryContent(library, viewModel::retry) { SettingsScreen(it.dailyHours) }
                }
                composable(DetailRoute, arguments = listOf(gameArgument)) { entry ->
                    GamePage(
                        onBack = { navigation.popBackStack() },
                        onEdit = { navigation.navigate("edit/${entry.arguments?.getLong("gameId")}") },
                        onFinished = { navigation.popBackStack() }
                    )
                }
                composable(EditorRoute, arguments = listOf(gameArgument)) { entry ->
                    val create = entry.arguments?.getLong("gameId") == 0L
                    GamePage(
                        editor = true,
                        onBack = { navigation.popBackStack() },
                        onFinished = { id ->
                            navigation.popBackStack()
                            if (create && id != null) showGame(id)
                        }
                    )
                }
            }
        }
    }
}

private fun NavController.openSection(section: Section) {
    navigate(section.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun LibraryContent(state: LibraryState, onRetry: () -> Unit, content: @Composable (LibraryState.Ready) -> Unit) {
    when (state) {
        LibraryState.Loading -> LoadingContent()
        LibraryState.Error -> Box(Modifier.padding(PlaySpacing.Inset)) { ErrorPanel(onRetry) }
        is LibraryState.Ready -> content(state)
    }
}

@Composable
private fun GamePage(
    onBack: () -> Unit,
    onFinished: (Long?) -> Unit,
    editor: Boolean = false,
    onEdit: () -> Unit = {},
    viewModel: GameViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val action by viewModel.action.collectAsStateWithLifecycle()
    LaunchedEffect(action) {
        when (val result = action) {
            is GameAction.Saved -> {
                viewModel.acknowledgeAction()
                onFinished(result.id)
            }
            GameAction.Deleted -> {
                viewModel.acknowledgeAction()
                onFinished(null)
            }
            else -> Unit
        }
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = PlaySpacing.Gap)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
        }
        when (val value = state) {
            GameState.Loading -> LoadingContent()
            GameState.Error -> Box(Modifier.padding(PlaySpacing.Inset)) { ErrorPanel(viewModel::retry) }
            GameState.Missing -> Box(Modifier.fillMaxWidth().padding(PlaySpacing.Inset)) {
                EmptyPanel(stringResource(R.string.game_missing), stringResource(R.string.game_missing_body))
            }
            is GameState.Ready -> if (editor) {
                GameEditor(value.game, action == GameAction.Busy, action == GameAction.Error, viewModel::save)
            } else {
                GameDetailScreen(value.game, action == GameAction.Busy, action == GameAction.Error,
                    onEdit, viewModel::setBacklog, viewModel::delete)
            }
        }
    }
}
