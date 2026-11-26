package com.playcrastination.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.playcrastination.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.settings by preferencesDataStore(name = "playcrastination_preferences")

class DataStorePreferences @Inject constructor(
    @ApplicationContext context: Context
) : PreferencesRepository {
    private val store = context.settings

    override val dailyHours = store.data.map { preferences ->
        (preferences[DailyHours] ?: 2).coerceIn(1, 24)
    }

    override suspend fun setDailyHours(hours: Int) {
        require(hours in 1..24)
        store.edit { it[DailyHours] = hours }
    }

    private companion object {
        val DailyHours = intPreferencesKey("daily_hours")
    }
}
