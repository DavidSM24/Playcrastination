package com.playcrastination.feature.backlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playcrastination.domain.model.BacklogPlacement
import com.playcrastination.domain.model.CompletionGoal
import com.playcrastination.domain.model.Priority
import com.playcrastination.domain.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BacklogWrite { IDLE, SAVING, FAILED }

@HiltViewModel
class BacklogViewModel @Inject constructor(private val repository: GameRepository) : ViewModel() {
    private val _write = MutableStateFlow(BacklogWrite.IDLE)
    val write = _write.asStateFlow()

    fun add(id: Long) = save { repository.setBacklog(id, true) }
    fun remove(id: Long) = save { repository.setBacklog(id, false) }
    fun start(id: Long) = save { repository.startQueuedGame(id) }
    fun pause(id: Long) = save { repository.pauseAndQueue(id) }
    fun setPriority(id: Long, priority: Priority) = save { repository.setPriority(id, priority) }
    fun setGoal(id: Long, goal: CompletionGoal) = save { repository.setGoal(id, goal) }
    fun move(id: Long, anchorId: Long, placement: BacklogPlacement) =
        save { repository.moveInBacklog(id, anchorId, placement) }

    private fun save(operation: suspend () -> Unit) {
        if (_write.value == BacklogWrite.SAVING) return
        _write.value = BacklogWrite.SAVING
        viewModelScope.launch {
            _write.value = try {
                operation()
                BacklogWrite.IDLE
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                BacklogWrite.FAILED
            }
        }
    }
}
