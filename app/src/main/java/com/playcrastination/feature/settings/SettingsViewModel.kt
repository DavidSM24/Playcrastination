package com.playcrastination.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playcrastination.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: PreferencesRepository
) : ViewModel() {
    private val _writeError = MutableStateFlow(false)
    val writeError = _writeError.asStateFlow()

    fun setDailyHours(hours: Int) {
        viewModelScope.launch {
            try {
                preferences.setDailyHours(hours)
                _writeError.value = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _writeError.value = true
            }
        }
    }
}
