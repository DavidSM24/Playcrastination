package com.playcrastination.data.steam

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.playcrastination.core.database.PlayDatabase
import com.playcrastination.domain.model.SteamFailure
import com.playcrastination.domain.repository.SteamRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

@HiltWorker
class SteamSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val steam: SteamRepository
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        steam.sync(ifStale = true)
        Result.success()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: SteamFailure) {
        if (failure.problem.retryable && runAttemptCount < 4) Result.retry() else Result.failure()
    } catch (_: Exception) {
        Result.failure()
    }
}

@Singleton
class SteamSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configuration: SteamConfiguration,
    private val database: PlayDatabase
) {
    suspend fun schedule() {
        val options = configuration.settings.first()
        val work = WorkManager.getInstance(context)
        if (!options.automatic || !options.configured) {
            work.cancelUniqueWork(PeriodicWork)
            work.cancelUniqueWork(ForegroundWork)
            return
        }
        val connected = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val interval = options.intervalMinutes.toLong()
        work.enqueueUniquePeriodicWork(
            PeriodicWork,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<SteamSyncWorker>(interval, TimeUnit.MINUTES)
                .setInitialDelay(interval, TimeUnit.MINUTES)
                .setConstraints(connected)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 60, TimeUnit.SECONDS)
                .build()
        )

        val lastSync = options.steamId?.let { database.steamDao().profile(it)?.lastSyncMillis }
        if (lastSync == null || System.currentTimeMillis() - lastSync >= interval * 60_000L) {
            work.enqueueUniqueWork(
                ForegroundWork,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<SteamSyncWorker>()
                    .setConstraints(connected)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 60, TimeUnit.SECONDS)
                    .build()
            )
        }
    }

    private companion object {
        const val PeriodicWork = "steam.periodic"
        const val ForegroundWork = "steam.foreground"
    }
}
