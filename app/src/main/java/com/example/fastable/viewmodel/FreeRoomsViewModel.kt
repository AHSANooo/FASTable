package com.example.fastable.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fastable.data.models.FreeRoom
import com.example.fastable.data.remote.FreeRoomsExtractor
import com.example.fastable.data.remote.GoogleSheetsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FreeRoomsViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "FreeRoomsViewModel"
    private val sheetsService = GoogleSheetsService(application)

    private val _freeRooms = MutableLiveData<List<FreeRoom>>()
    val freeRooms: LiveData<List<FreeRoom>> = _freeRooms

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Cache spreadsheet to avoid repeated API calls
    private var cachedSpreadsheet: com.google.api.services.sheets.v4.model.Spreadsheet? = null
    private var cacheTime: Long = 0
    private val CACHE_DURATION = 30 * 60 * 1000L // 30 minutes

    // Cache free rooms per day
    private val freeRoomsCache = mutableMapOf<String, List<FreeRoom>>()

    fun loadFreeRoomsForDay(day: String) {
        // Check cache first
        freeRoomsCache[day]?.let { cached ->
            Log.d(TAG, "Using cached free rooms for $day")
            _freeRooms.value = cached
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val rooms = withContext(Dispatchers.IO) {
                    val spreadsheet = getSpreadsheet()
                    if (spreadsheet == null) {
                        throw Exception("Failed to load timetable data")
                    }
                    FreeRoomsExtractor.getFreeRoomsForDay(spreadsheet, day)
                }

                // Cache the result
                freeRoomsCache[day] = rooms

                _freeRooms.value = rooms
                _isLoading.value = false

                if (rooms.isEmpty()) {
                    Log.d(TAG, "No free rooms found for $day")
                } else {
                    Log.d(TAG, "Found ${rooms.size} rooms with free slots for $day")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading free rooms", e)
                _errorMessage.value = "Failed to load free rooms: ${e.message}"
                _isLoading.value = false
                _freeRooms.value = emptyList()
            }
        }
    }

    private suspend fun getSpreadsheet(): com.google.api.services.sheets.v4.model.Spreadsheet? {
        val currentTime = System.currentTimeMillis()

        // Return cached if still valid
        if (cachedSpreadsheet != null && (currentTime - cacheTime) < CACHE_DURATION) {
            Log.d(TAG, "Using cached spreadsheet")
            return cachedSpreadsheet
        }

        // Fetch new
        Log.d(TAG, "Fetching spreadsheet from Google Sheets...")
        val spreadsheet = sheetsService.fetchSpreadsheet()

        if (spreadsheet != null) {
            cachedSpreadsheet = spreadsheet
            cacheTime = currentTime
        }

        return spreadsheet
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

