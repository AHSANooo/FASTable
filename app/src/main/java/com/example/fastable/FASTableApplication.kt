package com.example.fastable

import android.app.Application
import androidx.work.WorkManager
import com.example.fastable.data.remote.GoogleSheetsConfig
import com.example.fastable.data.remote.SpreadsheetConfigManager
import com.example.fastable.services.TimetableSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FASTableApplication : Application() {

    // Application-scoped coroutine scope for initialization tasks
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Initialize the SpreadsheetConfigManager with the fallback/current spreadsheet ID
        SpreadsheetConfigManager.init(GoogleSheetsConfig.SPREADSHEET_ID)

        // Initialize spreadsheet ID from Firebase for this session (runs in background)
        // This fetches from Firebase ONCE and caches for the entire app session
        applicationScope.launch {
            SpreadsheetConfigManager.initializeForSession(this@FASTableApplication)
        }

        // Cancel any stale/orphaned sync work from previous app sessions
        // This prevents multiple simultaneous syncs that can happen after app updates or crashes
        WorkManager.getInstance(this).cancelAllWorkByTag("com.example.fastable.services.TimetableSyncWorker")

        // Schedule periodic background sync (every 1 hour)
        // This ensures timetable data stays up-to-date even when app is in background
        TimetableSyncWorker.schedulePeriodicSync(this)
    }
}

