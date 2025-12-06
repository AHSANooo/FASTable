package com.example.fastable.utils

import android.util.Log

/**
 * Deprecated: Migration utility is no longer needed
 * All users now use Firebase Realtime Database directly
 * This class is kept for reference only
 */
class UserDataMigration {
    companion object {
        private const val TAG = "UserDataMigration"

        // Migration is no longer needed - kept for backward compatibility
        fun migrateCurrentUser(onComplete: (Boolean, String) -> Unit) {
            Log.d(TAG, "Migration no longer needed - using Realtime Database")
            onComplete(true, "Migration not required - using Realtime Database")
        }
    }
}

