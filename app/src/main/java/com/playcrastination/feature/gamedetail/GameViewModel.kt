package com.playcrastination.feature.gamedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playcrastination.domain.model.Game
import com.playcrastination.domain.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface GameState {
    data object Loading : GameState
    data object Error : GameState
    data object Missing : GameState
    data class Ready(val game: Game) : GameState
}

sealed interface GameAction {
    data object Idle : GameAction
    data object Busy : GameAction
    data object Error : GameAction
    data object Deleted : GameAction
    data class Saved(val id: Long) : GameAction
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GameViewModel @Inject constructor(
    arguments: SavedStateHandle,
    private val repository: GameRepository
) : ViewModel() {
    private val id = checkNotNull(arguments.get<Long>("gameId"))
    private val revision = MutableStateFlow(0)
    val state = revision.flatMapLatest {
        if (id == 0L) {
            flowOf(GameState.Ready(Game()))
        } else {
            repository.observeGame(id)
                .map<Game?, GameState> { game -> game?.let(GameState::Ready) ?: GameState.Missing }
                .catch { emit(GameState.Error) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameState.Loading)

    private val _action = MutableStateFlow<GameAction>(GameAction.Idle)
    val action = _action.asStateFlow()

    fun retry() = revision.update { it + 1 }
    fun save(game: Game) = perform { GameAction.Saved(repository.save(game)) }
    fun delete() = perform {
        repository.delete(id)
        GameAction.Deleted
    }

    fun setBacklog(inBacklog: Boolean) = perform {
        repository.setBacklog(id, inBacklog)
        GameAction.Idle
    }

    fun acknowledgeAction() {
        _action.value = GameAction.Idle
    }

    private fun perform(operation: suspend () -> GameAction) {
        if (_action.value == GameAction.Busy) return
        _action.value = GameAction.Busy
        viewModelScope.launch {
            try {
                _action.value = operation()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _action.value = GameAction.Error
            }
        }
    }
}
