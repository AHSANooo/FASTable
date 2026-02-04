package com.example.fastable.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fastable.data.models.FreeRoom
import com.example.fastable.data.models.SlotWithFreeRooms
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

    private val _filteredRooms = MutableLiveData<List<FreeRoom>>()
    val filteredRooms: LiveData<List<FreeRoom>> = _filteredRooms

    // Slot-wise data for "By Slots" tab
    private val _slotWiseFreeRooms = MutableLiveData<List<SlotWithFreeRooms>>()
    val slotWiseFreeRooms: LiveData<List<SlotWithFreeRooms>> = _slotWiseFreeRooms

    // Filtered slot-wise data (rooms or labs)
    private val _filteredSlotWiseRooms = MutableLiveData<List<SlotWithFreeRooms>>()
    val filteredSlotWiseRooms: LiveData<List<SlotWithFreeRooms>> = _filteredSlotWiseRooms

    // Currently available rooms (current + next slot)
    private val _currentlyAvailable = MutableLiveData<List<SlotWithFreeRooms>>()
    val currentlyAvailable: LiveData<List<SlotWithFreeRooms>> = _currentlyAvailable

    // Filtered currently available (rooms or labs)
    private val _filteredCurrentlyAvailable = MutableLiveData<List<SlotWithFreeRooms>>()
    val filteredCurrentlyAvailable: LiveData<List<SlotWithFreeRooms>> = _filteredCurrentlyAvailable

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Cache spreadsheet to avoid repeated API calls
    private var cachedSpreadsheet: com.google.api.services.sheets.v4.model.Spreadsheet? = null
    private var cacheTime: Long = 0
    private val CACHE_DURATION = 30 * 60 * 1000L // 30 minutes

    // Cache slot-wise free rooms per day
    private val slotWiseCache = mutableMapOf<String, List<SlotWithFreeRooms>>()

    // Current filter state
    private var currentDay: String = "Monday"
    private var showLabs: Boolean = false  // false = Rooms, true = Labs

    /**
     * Load slot-wise free rooms for a day (new efficient method)
     */
    fun loadSlotWiseFreeRooms(day: String, forceRefresh: Boolean = false) {
        currentDay = day

        // Check cache first
        if (!forceRefresh) {
            slotWiseCache[day]?.let { cached ->
                Log.d(TAG, "Using cached slot-wise free rooms for $day")
                _slotWiseFreeRooms.value = cached
                applySlotWiseFilter()
                updateCurrentlyAvailable(cached)
                return
            }
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val slots = withContext(Dispatchers.IO) {
                    val spreadsheet = getSpreadsheet(forceRefresh)
                        ?: throw Exception("Failed to load timetable data")
                    FreeRoomsExtractor.getSlotWiseFreeRoomsForDay(spreadsheet, day)
                }

                // Cache the result
                slotWiseCache[day] = slots

                _slotWiseFreeRooms.value = slots
                applySlotWiseFilter()
                updateCurrentlyAvailable(slots)
                _isLoading.value = false

                Log.d(TAG, "Found ${slots.size} time slots for $day")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading slot-wise free rooms", e)
                _errorMessage.value = "Failed to load free rooms: ${e.message}"
                _isLoading.value = false
                _slotWiseFreeRooms.value = emptyList()
                _filteredSlotWiseRooms.value = emptyList()
                _currentlyAvailable.value = emptyList()
                _filteredCurrentlyAvailable.value = emptyList()
            }
        }
    }

    /**
     * Update currently available rooms from the slot data
     */
    private fun updateCurrentlyAvailable(allSlots: List<SlotWithFreeRooms>) {
        val available = allSlots.filter { it.isCurrentSlot || it.isNextSlot }
        _currentlyAvailable.value = available
        applyCurrentlyAvailableFilter()
    }

    /**
     * Apply filter for slot-wise view (show only rooms or labs count)
     */
    private fun applySlotWiseFilter() {
        val allSlots = _slotWiseFreeRooms.value ?: emptyList()
        // For slot-wise, we keep all slots but filter will affect what's shown in each slot
        _filteredSlotWiseRooms.value = allSlots
    }

    /**
     * Apply filter for currently available view
     */
    private fun applyCurrentlyAvailableFilter() {
        val available = _currentlyAvailable.value ?: emptyList()
        _filteredCurrentlyAvailable.value = available
    }

    /**
     * Refresh data - clears cache and reloads
     */
    fun refreshData() {
        // Clear all caches
        slotWiseCache.clear()
        cachedSpreadsheet = null
        cacheTime = 0

        // Reload current day
        loadSlotWiseFreeRooms(currentDay, forceRefresh = true)
    }

    /**
     * Set filter to show Labs or Rooms
     */
    fun setRoomTypeFilter(showLabsFilter: Boolean) {
        showLabs = showLabsFilter
        applySlotWiseFilter()
        applyCurrentlyAvailableFilter()
    }

    /**
     * Check if labs filter is active
     */
    fun isLabsFilterActive(): Boolean = showLabs

    private suspend fun getSpreadsheet(forceRefresh: Boolean = false): com.google.api.services.sheets.v4.model.Spreadsheet? {
        val currentTime = System.currentTimeMillis()

        // Return cached if still valid and not forcing refresh
        if (!forceRefresh && cachedSpreadsheet != null && (currentTime - cacheTime) < CACHE_DURATION) {
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

    // Legacy methods for backward compatibility
    fun loadFreeRoomsForDay(day: String, forceRefresh: Boolean = false) {
        loadSlotWiseFreeRooms(day, forceRefresh)
    }
}
