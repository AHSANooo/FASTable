package com.example.fastable.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fastable.data.models.DashboardSession
import com.example.fastable.data.repository.TimetableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TimetableRepository(application)

    private val _todaySessions = MutableStateFlow<List<DashboardSession>>(emptyList())
    val todaySessions: StateFlow<List<DashboardSession>> = _todaySessions.asStateFlow()

    private val _currentDay = MutableStateFlow("")
    val currentDay: StateFlow<String> = _currentDay.asStateFlow()

    init {
        loadTodaySessions()
    }

    /**
     * Get current day (Monday-Friday, weekend maps to Monday)
     */
    private fun getCurrentDay(): String {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        return when (dayOfWeek) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY, Calendar.SUNDAY -> "Monday" // Weekend maps to Monday
            else -> "Monday"
        }
    }

    /**
     * Load today's sessions from database
     */
    fun loadTodaySessions() {
        val today = getCurrentDay()
        _currentDay.value = today

        viewModelScope.launch {
            repository.getDashboardSessions().collect { allSessions ->
                // Filter sessions for today
                _todaySessions.value = allSessions.filter { it.day.equals(today, ignoreCase = true) }
                    .sortedBy { it.timeSlot }
            }
        }
    }

    /**
     * Delete a session from dashboard
     */
    fun deleteSession(session: DashboardSession) {
        viewModelScope.launch {
            repository.deleteDashboardSession(session)
        }
    }
}

