package com.example.fastable

import android.app.Application
import com.example.fastable.data.remote.GoogleSheetsConfig
import com.example.fastable.data.remote.SpreadsheetConfigManager

class FASTableApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize the SpreadsheetConfigManager with the fallback/current spreadsheet ID
        SpreadsheetConfigManager.init(GoogleSheetsConfig.SPREADSHEET_ID)
    }
}

