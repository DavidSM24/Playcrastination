package com.playcrastination.feature.steam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playcrastination.data.steam.SteamSyncScheduler
import com.playcrastination.domain.model.SteamConnection
import com.playcrastination.domain.model.SteamFailure
import com.playcrastination.domain.model.SteamLibrary
import com.playcrastination.domain.model.SteamProblem
import com.playcrastination.domain.repository.SteamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SteamTask { CONFIGURE, VALIDATE, PREVIEW, IMPORT, SYNC, UPDATE_GAME }
enum class SteamNotice { SAVED, DISCONNECTED, VALIDATED, IMPORTED, SYNCED, UPDATED, CANCELLED }

data class SteamUiState(
    val task: SteamTask? = null,
    val source: SteamTask? = null,
    val gameId: Long? = null,
    val notice: SteamNotice? = null,
    val problem: SteamProblem? = null,
    val count: Int = 0,
    val total: Int = 0
)

@HiltViewModel
class SteamViewModel @Inject constructor(
    private val steam: SteamRepository,
    private val scheduler: SteamSyncScheduler
) : ViewModel() {
    val connection = steam.connection.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SteamConnection())
    private val _ui = MutableStateFlow(SteamUiState())
    val ui = _ui.asStateFlow()
    private val _library = MutableStateFlow<SteamLibrary?>(null)
    val library = _library.asStateFlow()
    private var job: Job? = null

    fun connect(id: String, key: String) = run(SteamTask.CONFIGURE) {
        steam.saveConnection(id, key)
        _library.value = null
        scheduler.schedule()
        SteamUiState(notice = SteamNotice.SAVED)
    }

    fun disconnect() = run(SteamTask.CONFIGURE) {
        steam.disconnect()
        _library.value = null
        scheduler.schedule()
        SteamUiState(notice = SteamNotice.DISCONNECTED)
    }

    fun validate() = run(SteamTask.VALIDATE) {
        SteamUiState(notice = SteamNotice.VALIDATED, count = steam.validate())
    }

    fun loadLibrary(recentOnly: Boolean) = run(SteamTask.PREVIEW) {
        _library.value = steam.preview(recentOnly)
        SteamUiState()
    }

    fun dismissLibrary() {
        if (_ui.value.task != SteamTask.IMPORT) _library.value = null
    }

    fun import(ids: Set<Long>) {
        val preview = _library.value ?: return
        run(SteamTask.IMPORT) {
            val added = steam.import(preview, ids) { count, total -> _ui.update { it.copy(count = count, total = total) } }
            _library.value = null
            SteamUiState(notice = SteamNotice.IMPORTED, count = added)
        }
    }

    fun cancelImport() {
        if (_ui.value.task == SteamTask.IMPORT) job?.cancel()
    }

    fun sync() = run(SteamTask.SYNC) {
        steam.sync()
        SteamUiState(notice = SteamNotice.SYNCED)
    }

    fun setAutomatic(enabled: Boolean) = run(SteamTask.CONFIGURE) {
        steam.setAutomatic(enabled)
        scheduler.schedule()
        SteamUiState()
    }

    fun setInterval(minutes: Int) = run(SteamTask.CONFIGURE) {
        steam.setInterval(minutes)
        scheduler.schedule()
        SteamUiState()
    }

    fun setHidden(gameId: Long, hidden: Boolean) = run(SteamTask.UPDATE_GAME, gameId) {
        steam.setHidden(gameId, hidden)
        SteamUiState()
    }

    fun metadata(gameId: Long) = run(SteamTask.UPDATE_GAME, gameId) {
        steam.refreshMetadata(gameId)
        SteamUiState(notice = SteamNotice.UPDATED)
    }

    fun achievements(gameId: Long) = run(SteamTask.UPDATE_GAME, gameId) {
        steam.refreshAchievements(gameId)
        SteamUiState(notice = SteamNotice.UPDATED)
    }

    private fun run(task: SteamTask, gameId: Long? = null, operation: suspend () -> SteamUiState) {
        if (_ui.value.task != null) return
        _ui.value = SteamUiState(task = task, source = task, gameId = gameId)
        job = viewModelScope.launch {
            try {
                _ui.value = operation().copy(source = task, gameId = gameId)
            } catch (cancelled: CancellationException) {
                _ui.value = SteamUiState(notice = SteamNotice.CANCELLED, source = task, gameId = gameId)
                throw cancelled
            } catch (failure: SteamFailure) {
                _ui.value = SteamUiState(problem = failure.problem, source = task, gameId = gameId)
            } catch (_: Exception) {
                _ui.value = SteamUiState(problem = SteamProblem.STORAGE, source = task, gameId = gameId)
            }
        }
    }
}
