package com.example.fastable.services

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.fastable.data.models.DashboardSession
import com.example.fastable.data.repository.TimetableRepository
import com.example.fastable.utils.NotificationScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.TimeUnit

/**
 * Background worker that syncs timetable data periodically
 * This runs in the background to keep dashboard data up-to-date
 */
class TimetableSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "TimetableSyncWorker"
        private const val UNIQUE_WORK_NAME = "timetable_sync_work"
        private const val IMMEDIATE_SYNC_WORK_NAME = "timetable_immediate_sync"

        // Sync interval in hours
        private const val SYNC_INTERVAL_HOURS = 1L

        // Mutex to prevent multiple simultaneous syncs within the same process
        private val syncMutex = Mutex()

        // Track last sync time to debounce rapid sync requests
        @Volatile
        private var lastSyncTimeMs: Long = 0
        private const val MIN_SYNC_INTERVAL_MS = 30_000L // 30 seconds minimum between syncs

        /**
         * Schedule periodic background sync
         * Runs every hour when the device has network connectivity
         */
        fun schedulePeriodicSync(context: Context) {
            Log.d(TAG, "Scheduling periodic timetable sync")

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<TimetableSyncWorker>(
                SYNC_INTERVAL_HOURS, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP, // Don't replace if already scheduled
                    syncRequest
                )

            Log.d(TAG, "Periodic sync scheduled every $SYNC_INTERVAL_HOURS hour(s)")
        }

        /**
         * Run a one-time sync immediately
         * Uses unique work name with KEEP policy to prevent multiple simultaneous syncs
         */
        fun runImmediateSync(context: Context) {
            // Debounce: skip if synced recently
            val now = System.currentTimeMillis()
            if (now - lastSyncTimeMs < MIN_SYNC_INTERVAL_MS) {
                Log.d(
                    TAG,
                    "Skipping immediate sync - last sync was ${(now - lastSyncTimeMs) / 1000}s ago"
                )
                return
            }

            Log.d(TAG, "Running immediate timetable sync")

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<TimetableSyncWorker>()
                .setConstraints(constraints)
                .build()

            // Use KEEP policy - if there's already a sync in progress, don't start another
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    IMMEDIATE_SYNC_WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    syncRequest
                )
        }

        /**
         * Cancel all sync work
         */
        fun cancelSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_SYNC_WORK_NAME)
            Log.d(TAG, "Cancelled all sync work")
        }
    }

    override suspend fun doWork(): Result {
        // Use mutex to ensure only one sync runs at a time within this process
        if (!syncMutex.tryLock()) {
            Log.d(TAG, "Another sync is already in progress, skipping")
            return Result.success()
        }

        return try {
            Log.d(TAG, "Starting timetable sync work")
            lastSyncTimeMs = System.currentTimeMillis()

            val repository = TimetableRepository(applicationContext)

            // Get current dashboard sessions from database
            val currentSessions: List<DashboardSession> = repository.getDashboardSessions().first()

            if (currentSessions.isEmpty()) {
                Log.d(TAG, "No sessions to sync, skipping")
                Result.success()
            } else {
                Log.d(TAG, "Syncing ${currentSessions.size} sessions")

                // Refresh from spreadsheet
                // Use forceRefresh=false for background sync to use cached data if available
                // This makes background sync much faster while still detecting changes
                val refreshResult = repository.refreshDashboardSessions(currentSessions, forceRefresh = false)

                if (refreshResult.isSuccess) {
                    val refreshedSessions = refreshResult.getOrNull() ?: emptyList()
                    Log.d(TAG, "Sync successful - ${refreshedSessions.size} sessions updated")

                    // Reschedule notifications with updated data
                    NotificationScheduler.scheduleNotificationsForSessions(
                        applicationContext,
                        refreshedSessions
                    )

                    // Check for cancelled classes
                    val cancelledCount = refreshedSessions.count {
                        it.courseName.contains("Cancelled", ignoreCase = true)
                    }
                    if (cancelledCount > 0) {
                        Log.d(TAG, "Found $cancelledCount cancelled class(es)")
                    }
                    Result.success()
                } else {
                    val exception = refreshResult.exceptionOrNull()
                    Log.e(TAG, "Sync failed: ${exception?.message}")
                    Result.retry() // Will retry with exponential backoff
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync worker exception: ${e.message}", e)
            Result.retry()
        } finally {
            syncMutex.unlock()
        }
    }
}
