package pl.przypominajka.librus

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import pl.przypominajka.appContainer
import java.util.concurrent.TimeUnit

/** Synchronizacja z Librusem w tle (WorkManager – działa też po restarcie telefonu). */
class LibrusSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val outcome = applicationContext.appContainer.librusSyncer.sync()
        return if (outcome is LibrusSyncOutcome.Failure && outcome.retryable && runAttemptCount < MAX_RETRIES) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    private companion object {
        const val MAX_RETRIES = 3
    }
}

class LibrusSyncScheduler(context: Context) {

    private val appContext = context.applicationContext

    /** Co ok. 6 godzin, gdy jest internet. KEEP – nie przesuwa już zaplanowanej pracy. */
    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<LibrusSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel() {
        WorkManager.getInstance(appContext).cancelUniqueWork(PERIODIC_WORK)
    }

    private companion object {
        const val PERIODIC_WORK = "librus_sync"
    }
}
