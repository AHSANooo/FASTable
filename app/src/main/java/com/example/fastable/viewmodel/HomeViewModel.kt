package com.example.fastable.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fastable.data.models.DashboardSession
import com.example.fastable.data.repository.TimetableRepository
import com.example.fastable.utils.NotificationScheduler
import kotlinx.coroutines.launch
import java.util.*

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "HomeViewModel"
    private val repository = TimetableRepository(application)
    private val context = application.applicationContext

    private val _dashboardSessions = MutableLiveData<List<DashboardSession>>()
    val dashboardSessions: LiveData<List<DashboardSession>> = _dashboardSessions

    private val _currentDay = MutableLiveData<String>()
    val currentDay: LiveData<String> = _currentDay

    private val _todaysSessions = MutableLiveData<List<DashboardSession>>()
    val todaysSessions: LiveData<List<DashboardSession>> = _todaysSessions

    private val _allDaySessions = MutableLiveData<List<DashboardSession>>()
    val allDaySessions: LiveData<List<DashboardSession>> = _allDaySessions

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private var currentViewingDay: String? = null
    private var hasRefreshedOnStart = false

    init {
        updateCurrentDay()
        loadDashboardSessions()
    }

    private fun updateCurrentDay() {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val day = when (dayOfWeek) {
            Calendar.SATURDAY, Calendar.SUNDAY -> "Monday" // Show Monday on weekends
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            else -> "Monday"
        }

        _currentDay.value = day
        Log.d(TAG, "updateCurrentDay: Current day set to $day")
    }

    fun loadDashboardSessions() {
        _isLoading.value = true
        Log.d(TAG, "loadDashboardSessions: Starting to load dashboard sessions")
        viewModelScope.launch {
            repository.getDashboardSessions().collect { sessions ->
                Log.d(TAG, "loadDashboardSessions: Received ${sessions.size} total sessions from repository")
                _dashboardSessions.value = sessions
                filterTodaysSessions(sessions)

                // Schedule notifications for all sessions
                NotificationScheduler.scheduleNotificationsForSessions(context, sessions)
                Log.d(TAG, "loadDashboardSessions: Notifications scheduled for ${sessions.size} sessions")

                // Also update all-day sessions if a day is currently being viewed
                currentViewingDay?.let { day ->
                    Log.d(TAG, "loadDashboardSessions: Auto-refreshing All Timetable for $day")
                    loadAllSessionsForDay(day)
                }

                _isLoading.value = false
            }
        }
    }

    private fun filterTodaysSessions(allSessions: List<DashboardSession>) {
        val currentDay = _currentDay.value ?: "Monday"
        val filtered = allSessions.filter { it.day.equals(currentDay, ignoreCase = true) }
            .sortedBy { it.getStartTimeMillis() }

        Log.d(TAG, "filterTodaysSessions: Found ${filtered.size} sessions for $currentDay")

        _todaysSessions.value = filtered
    }

    fun deleteDashboardSession(session: DashboardSession) {
        viewModelScope.launch {
            try {
                repository.deleteDashboardSession(session)
                // Cancel notifications for this session
                NotificationScheduler.cancelNotificationForSession(context, session.id)
                _errorMessage.value = "Session removed from dashboard"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to remove session: ${e.message}"
            }
        }
    }

    fun loadAllSessionsForDay(day: String) {
        Log.d(TAG, "loadAllSessionsForDay: Loading sessions for $day")
        currentViewingDay = day
        val allSessions = _dashboardSessions.value ?: emptyList()
        val filtered = allSessions.filter { it.day.equals(day, ignoreCase = true) }
            .sortedBy { it.getStartTimeMillis() }

        Log.d(TAG, "loadAllSessionsForDay: Found ${filtered.size} sessions for $day")
        _allDaySessions.value = filtered
    }

    fun refreshData() {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.syncData(forceRefresh = true)
            _isLoading.value = false

            result.onSuccess {
                loadDashboardSessions()
            }.onFailure { exception ->
                _errorMessage.value = "Sync failed: ${exception.message}"
            }
        }
    }

    /**
     * Check if device is online
     */
    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Refresh dashboard sessions on app start to detect cancelled classes
     * Only runs once per app launch and only if online
     */
    fun refreshDashboardOnStart() {
        if (hasRefreshedOnStart) {
            Log.d(TAG, "refreshDashboardOnStart: Already refreshed this session, skipping")
            return
        }

        if (!isOnline()) {
            Log.d(TAG, "refreshDashboardOnStart: Offline, skipping refresh")
            return
        }

        hasRefreshedOnStart = true
        _isRefreshing.value = true

        viewModelScope.launch {
            try {
                Log.d(TAG, "refreshDashboardOnStart: Starting refresh for cancelled classes")

                // Get current dashboard sessions
                val currentSessions = _dashboardSessions.value ?: emptyList()
                if (currentSessions.isEmpty()) {
                    Log.d(TAG, "refreshDashboardOnStart: No sessions to refresh")
                    _isRefreshing.value = false
                    return@launch
                }

                // Get unique course names from dashboard
                val courseNames = currentSessions.map { it.courseName }.distinct()
                Log.d(TAG, "refreshDashboardOnStart: Refreshing ${courseNames.size} courses: $courseNames")

                // Refresh dashboard sessions from spreadsheet
                val result: Result<List<DashboardSession>> = repository.refreshDashboardSessions(currentSessions)

                result.onSuccess { refreshedSessions: List<DashboardSession> ->
                    Log.d(TAG, "refreshDashboardOnStart: Got ${refreshedSessions.size} refreshed sessions")

                    // Update LiveData - this will trigger UI update
                    _dashboardSessions.value = refreshedSessions
                    filterTodaysSessions(refreshedSessions)

                    // Update all-day sessions if viewing
                    currentViewingDay?.let { day ->
                        loadAllSessionsForDay(day)
                    }

                    // Schedule notifications for updated sessions
                    NotificationScheduler.scheduleNotificationsForSessions(context, refreshedSessions)
                }.onFailure { exception: Throwable ->
                    Log.e(TAG, "refreshDashboardOnStart: Failed - ${exception.message}")
                    // Don't show error to user, just keep existing data
                }
            } catch (e: Exception) {
                Log.e(TAG, "refreshDashboardOnStart: Exception - ${e.message}", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

