package com.example.fastable.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Manages the Google Sheets spreadsheet ID/link
 * - Fetches from Firebase Realtime Database ONCE per app session (on app start)
 * - Caches in memory for the entire session (no repeated Firebase calls)
 * - Also persists to SharedPreferences as backup for offline use
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

    // Cache expiration: 6 days in milliseconds (for SharedPreferences fallback)
    private const val CACHE_EXPIRATION_MS = 6L * 24 * 60 * 60 * 1000

    // Fallback spreadsheet ID (current one from GoogleSheetsConfig)
    private var fallbackSpreadsheetId: String = ""

    // SESSION-BASED CACHING: In-memory cache that persists for the app session
    // This prevents multiple Firebase calls during the same app session
    @Volatile
    private var sessionCachedSpreadsheetId: String? = null
    private var hasInitializedThisSession = false

    // Mutex to prevent multiple simultaneous Firebase fetches
    private val initMutex = Mutex()

    /**
     * Initialize with the current/fallback spreadsheet ID
     * Also triggers fetching from Firebase for this session
     */
    fun init(fallbackId: String) {
        fallbackSpreadsheetId = fallbackId
    }

    /**
     * Initialize the spreadsheet ID for this session
     * Should be called once when app starts (typically in Application.onCreate or first Activity)
     * This fetches from Firebase and caches for the entire session
     */
    suspend fun initializeForSession(context: Context) {
        initMutex.withLock {
            if (hasInitializedThisSession && sessionCachedSpreadsheetId != null) {
                Log.d(TAG, "Already initialized this session, using cached ID: $sessionCachedSpreadsheetId")
                return
            }

            Log.d(TAG, "Initializing spreadsheet ID for this session...")

            // Try to fetch from Firebase
            try {
                val linkOrId = fetchFromFirebase()
                if (!linkOrId.isNullOrEmpty()) {
                    val newId = extractSpreadsheetId(linkOrId)
                    sessionCachedSpreadsheetId = newId
                    hasInitializedThisSession = true

                    // Also save to SharedPreferences for offline backup
                    saveToCache(getPrefs(context), newId)
                    Log.d(TAG, "Session initialized with Firebase ID: $newId")
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch from Firebase during session init: ${e.message}")
            }

            // Fallback to SharedPreferences cache
            val prefs = getPrefs(context)
            val cachedId = prefs.getString(KEY_SPREADSHEET_ID, null)
            if (!cachedId.isNullOrEmpty()) {
                sessionCachedSpreadsheetId = cachedId
                hasInitializedThisSession = true
                Log.d(TAG, "Session initialized with cached ID: $cachedId")
                return
            }

            // Last resort: use hardcoded fallback
            sessionCachedSpreadsheetId = fallbackSpreadsheetId
            hasInitializedThisSession = true
            Log.d(TAG, "Session initialized with fallback ID: $fallbackSpreadsheetId")
        }
    }

    /**
     * Get the spreadsheet ID - uses session cache (INSTANT, no network call)
     * If session not initialized, falls back to SharedPreferences or fallback
     */
    suspend fun getSpreadsheetId(context: Context): String {
        // If we have a session-cached ID, return immediately (no suspension)
        sessionCachedSpreadsheetId?.let {
            return it
        }

        // If not initialized, initialize now (this should rarely happen if initializeForSession was called on app start)
        return withContext(Dispatchers.IO) {
            initMutex.withLock {
                // Double-check after acquiring lock
                sessionCachedSpreadsheetId?.let { return@withContext it }

                Log.d(TAG, "Session not initialized, initializing now...")

                // Try SharedPreferences first (fast)
                val prefs = getPrefs(context)
                val cachedId = prefs.getString(KEY_SPREADSHEET_ID, null)
                if (!cachedId.isNullOrEmpty()) {
                    sessionCachedSpreadsheetId = cachedId
                    hasInitializedThisSession = true
                    Log.d(TAG, "Using SharedPreferences cached ID: $cachedId")
                    return@withContext cachedId
                }

                // Try Firebase (slower, but needed first time)
                try {
                    val linkOrId = fetchFromFirebase()
                    if (!linkOrId.isNullOrEmpty()) {
                        val newId = extractSpreadsheetId(linkOrId)
                        sessionCachedSpreadsheetId = newId
                        hasInitializedThisSession = true
                        saveToCache(prefs, newId)
                        Log.d(TAG, "Fetched and cached from Firebase: $newId")
                        return@withContext newId
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase fetch failed: ${e.message}")
                }

                // Last resort
                sessionCachedSpreadsheetId = fallbackSpreadsheetId
                hasInitializedThisSession = true
                Log.d(TAG, "Using fallback ID: $fallbackSpreadsheetId")
                return@withContext fallbackSpreadsheetId
            }
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
     * Also updates the session cache
     */
    suspend fun forceRefresh(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            initMutex.withLock {
                try {
                    val linkOrId = fetchFromFirebase()
                    if (linkOrId != null && linkOrId.isNotEmpty()) {
                        val newId = extractSpreadsheetId(linkOrId)
                        val prefs = getPrefs(context)
                        saveToCache(prefs, newId)
                        sessionCachedSpreadsheetId = newId
                        hasInitializedThisSession = true
                        Log.d(TAG, "Force refreshed spreadsheet ID from Firebase: $newId")
                        return@withContext true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Force refresh failed: ${e.message}")
                }
                return@withContext false
            }
        }
    }

    /**
     * Clear the session cache (useful for testing or when you know the link changed)
     */
    fun clearSessionCache() {
        sessionCachedSpreadsheetId = null
        hasInitializedThisSession = false
        Log.d(TAG, "Session cache cleared")
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

