package com.example.fastable.services

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.fastable.data.models.DashboardSession
import com.example.fastable.data.repository.TimetableRepository
import com.example.fastable.utils.NotificationScheduler
import kotlinx.coroutines.flow.first
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

        // Sync interval in hours
        private const val SYNC_INTERVAL_HOURS = 1L

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
         * Used when app opens to get fresh data
         */
        fun runImmediateSync(context: Context) {
            Log.d(TAG, "Running immediate timetable sync")

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<TimetableSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueue(syncRequest)
        }

        /**
         * Cancel all sync work
         */
        fun cancelSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
            Log.d(TAG, "Cancelled periodic sync")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting timetable sync work")

        return try {
            val repository = TimetableRepository(applicationContext)

            // Get current dashboard sessions from database
            val currentSessions: List<DashboardSession> = repository.getDashboardSessions().first()

            if (currentSessions.isEmpty()) {
                Log.d(TAG, "No sessions to sync, skipping")
                return Result.success()
            }

            Log.d(TAG, "Syncing ${currentSessions.size} sessions")

            // Refresh from spreadsheet (force refresh to get latest data)
            val result = repository.refreshDashboardSessions(currentSessions, forceRefresh = true)

            result.onSuccess { refreshedSessions ->
                Log.d(TAG, "Sync successful - ${refreshedSessions.size} sessions updated")

                // Reschedule notifications with updated data
                NotificationScheduler.scheduleNotificationsForSessions(applicationContext, refreshedSessions)

                // Check for cancelled classes
                val cancelledCount = refreshedSessions.count {
                    it.courseName.contains("Cancelled", ignoreCase = true)
                }
                if (cancelledCount > 0) {
                    Log.d(TAG, "Found $cancelledCount cancelled class(es)")
                }
            }.onFailure { exception ->
                Log.e(TAG, "Sync failed: ${exception.message}")
                return Result.retry() // Will retry with exponential backoff
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync worker exception: ${e.message}", e)
            Result.retry()
        }
    }
}

