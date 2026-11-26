package com.playcrastination.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playcrastination.domain.repository.GameRepository
import com.playcrastination.domain.repository.PreferencesRepository
import com.playcrastination.domain.model.Game
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

sealed interface LibraryState {
    data object Loading : LibraryState
    data object Error : LibraryState
    data class Ready(val games: List<Game>, val dailyHours: Int) : LibraryState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    repository: GameRepository,
    preferences: PreferencesRepository
) : ViewModel() {
    private val revision = MutableStateFlow(0)
    val state = revision.flatMapLatest {
        combine<List<Game>, Int, LibraryState>(repository.observeLibrary(), preferences.dailyHours) { games, hours ->
            LibraryState.Ready(games, hours)
        }.catch { emit(LibraryState.Error) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryState.Loading)

    fun retry() = revision.update { it + 1 }
}
