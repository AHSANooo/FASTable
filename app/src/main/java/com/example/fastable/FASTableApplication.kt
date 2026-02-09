package com.example.fastable

import android.app.Application
import com.example.fastable.data.remote.GoogleSheetsConfig
import com.example.fastable.data.remote.SpreadsheetConfigManager
import com.example.fastable.services.TimetableSyncWorker

class FASTableApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize the SpreadsheetConfigManager with the fallback/current spreadsheet ID
        SpreadsheetConfigManager.init(GoogleSheetsConfig.SPREADSHEET_ID)

        // Schedule periodic background sync (every 1 hour)
        // This ensures timetable data stays up-to-date even when app is in background
        TimetableSyncWorker.schedulePeriodicSync(this)
    }
}

