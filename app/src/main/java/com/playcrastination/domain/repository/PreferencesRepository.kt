package com.playcrastination.domain.repository

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val dailyHours: Flow<Int>
    suspend fun setDailyHours(hours: Int)
}
