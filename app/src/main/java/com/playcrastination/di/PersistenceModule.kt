package com.playcrastination.di

import android.content.Context
import androidx.room.Room
import com.playcrastination.core.database.PlayDatabase
import com.playcrastination.data.repository.DataStorePreferences
import com.playcrastination.data.repository.RoomGameRepository
import com.playcrastination.domain.repository.GameRepository
import com.playcrastination.domain.repository.PreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PersistenceModule {
    @Binds abstract fun games(repository: RoomGameRepository): GameRepository
    @Binds abstract fun preferences(repository: DataStorePreferences): PreferencesRepository

    companion object {
        @Provides
        @Singleton
        fun database(@ApplicationContext context: Context): PlayDatabase =
            Room.databaseBuilder(context, PlayDatabase::class.java, "playcrastination.db").build()
    }
}
