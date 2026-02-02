package com.example.fastable.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Manages the Google Sheets spreadsheet ID/link
 * - Fetches from Firebase Realtime Database
 * - Caches locally with expiration (6 days)
 * - Automatically refreshes when expired or missing
 * - Supports both full URLs and spreadsheet IDs
 */
object SpreadsheetConfigManager {

    private const val TAG = "SpreadsheetConfigManager"

    // SharedPreferences keys
    private const val PREFS_NAME = "spreadsheet_config"
    private const val KEY_SPREADSHEET_ID = "spreadsheet_id"
    private const val KEY_LAST_FETCH_TIME = "last_fetch_time"

    // Firebase path - you can paste either the full URL or just the ID here
    private const val FIREBASE_PATH = "config/spreadsheet_link"

    // Cache expiration: 6 days in milliseconds
    private const val CACHE_EXPIRATION_MS = 6L * 24 * 60 * 60 * 1000

    // Fallback spreadsheet ID (current one from GoogleSheetsConfig)
    private var fallbackSpreadsheetId: String = ""

    /**
     * Initialize with the current/fallback spreadsheet ID
     */
    fun init(fallbackId: String) {
        fallbackSpreadsheetId = fallbackId
    }

    /**
     * Get the spreadsheet ID - from cache if valid, or fetch from Firebase
     */
    suspend fun getSpreadsheetId(context: Context): String {
        return withContext(Dispatchers.IO) {
            val prefs = getPrefs(context)

            // ALWAYS check Firebase first to see if the link has changed
            try {
                val newLinkOrId = fetchFromFirebase()
                if (newLinkOrId != null && newLinkOrId.isNotEmpty()) {
                    val newId = extractSpreadsheetId(newLinkOrId)

                    // Check if this is different from cached ID
                    val cachedId = prefs.getString(KEY_SPREADSHEET_ID, null)
                    if (cachedId != newId) {
                        Log.d(TAG, "Spreadsheet ID changed! Old: $cachedId, New: $newId")
                        // Save new ID to cache
                        saveToCache(prefs, newId)
                    } else {
                        Log.d(TAG, "Spreadsheet ID unchanged, refreshing cache timestamp")
                        // Same ID, just refresh the timestamp
                        saveToCache(prefs, newId)
                    }

                    return@withContext newId
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch from Firebase: ${e.message}")
            }

            // If Firebase fetch failed, use cached ID if available
            val cachedId = prefs.getString(KEY_SPREADSHEET_ID, null)
            val lastFetchTime = prefs.getLong(KEY_LAST_FETCH_TIME, 0)
            val currentTime = System.currentTimeMillis()

            val isCacheValid = cachedId != null &&
                               cachedId.isNotEmpty() &&
                               (currentTime - lastFetchTime) < CACHE_EXPIRATION_MS

            if (isCacheValid || cachedId != null) {
                Log.d(TAG, "Using cached spreadsheet ID (Firebase unavailable)")
                return@withContext cachedId!!
            }


            // Last resort: use the hardcoded fallback
            Log.d(TAG, "Using hardcoded fallback spreadsheet ID")
            return@withContext fallbackSpreadsheetId
        }
    }

    /**
     * Extract spreadsheet ID from a Google Sheets URL or return as-is if already an ID
     *
     * Supports formats:
     * - Full URL: https://docs.google.com/spreadsheets/d/1ABC123xyz/edit?gid=0#gid=0
     * - Short URL: https://docs.google.com/spreadsheets/d/1ABC123xyz
     * - Just ID: 1ABC123xyz
     */
    private fun extractSpreadsheetId(linkOrId: String): String {
        val trimmed = linkOrId.trim()

        // Check if it's a URL containing "/d/"
        if (trimmed.contains("/d/")) {
            return try {
                // Extract the ID between /d/ and the next /
                val afterD = trimmed.split("/d/")[1]
                val id = afterD.split("/")[0].split("?")[0].split("#")[0]
                Log.d(TAG, "Extracted spreadsheet ID from URL: $id")
                id
            } catch (e: Exception) {
                Log.e(TAG, "Failed to extract ID from URL, using as-is: $trimmed")
                trimmed
            }
        }

        // It's already just an ID
        return trimmed
    }

    /**
     * Force refresh the spreadsheet ID from Firebase
     */
    suspend fun forceRefresh(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val linkOrId = fetchFromFirebase()
                if (linkOrId != null && linkOrId.isNotEmpty()) {
                    val newId = extractSpreadsheetId(linkOrId)
                    val prefs = getPrefs(context)
                    saveToCache(prefs, newId)
                    Log.d(TAG, "Force refreshed spreadsheet ID from Firebase: $newId")
                    return@withContext true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Force refresh failed: ${e.message}")
            }
            return@withContext false
        }
    }

    /**
     * Clear the cached spreadsheet ID (forces a refresh on next fetch)
     */
    fun clearCache(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit()
            .remove(KEY_SPREADSHEET_ID)
            .remove(KEY_LAST_FETCH_TIME)
            .apply()
        Log.d(TAG, "Cache cleared")
    }

    /**
     * Check if the cache is expired
     */
    fun isCacheExpired(context: Context): Boolean {
        val prefs = getPrefs(context)
        val lastFetchTime = prefs.getLong(KEY_LAST_FETCH_TIME, 0)
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastFetchTime) >= CACHE_EXPIRATION_MS
    }

    /**
     * Get days until cache expires
     */
    fun getDaysUntilExpiration(context: Context): Int {
        val prefs = getPrefs(context)
        val lastFetchTime = prefs.getLong(KEY_LAST_FETCH_TIME, 0)
        val currentTime = System.currentTimeMillis()
        val remaining = CACHE_EXPIRATION_MS - (currentTime - lastFetchTime)
        return if (remaining > 0) (remaining / (1000 * 60 * 60 * 24)).toInt() else 0
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun saveToCache(prefs: SharedPreferences, spreadsheetId: String) {
        prefs.edit()
            .putString(KEY_SPREADSHEET_ID, spreadsheetId)
            .putLong(KEY_LAST_FETCH_TIME, System.currentTimeMillis())
            .apply()
    }

    private suspend fun fetchFromFirebase(): String? {
        return try {
            val database = FirebaseDatabase.getInstance()
            val ref = database.getReference(FIREBASE_PATH)
            val snapshot = ref.get().await()
            snapshot.getValue(String::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase fetch error: ${e.message}")
            null
        }
    }
}

