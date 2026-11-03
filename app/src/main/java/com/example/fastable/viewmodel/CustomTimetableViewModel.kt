package com.example.fastable.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fastable.data.local.AppDatabase
import com.example.fastable.data.models.Course
import com.example.fastable.data.models.TimetableSession
import com.example.fastable.data.repository.TimetableRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomTimetableViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TimetableRepository(application)
    private val database = AppDatabase.getDatabase(application)

    private val _courses = MutableLiveData<List<Course>>()
    val courses: LiveData<List<Course>> = _courses

    private val _selectedCourses = MutableLiveData<List<Course>>()
    val selectedCourses: LiveData<List<Course>> = _selectedCourses

    private val _timetableSessions = MutableLiveData<List<TimetableSession>>()
    val timetableSessions: LiveData<List<TimetableSession>> = _timetableSessions

    private val _departments = MutableLiveData<List<String>>()
    val departments: LiveData<List<String>> = _departments

    private val _batches = MutableLiveData<List<String>>()
    val batches: LiveData<List<String>> = _batches

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            repository.getDepartments().collect { deptList ->
                _departments.value = deptList
            }
        }

        viewModelScope.launch {
            repository.getBatches().collect { batchList ->
                _batches.value = batchList
            }
        }

        viewModelScope.launch {
            repository.getAllCourses().collect { courseList ->
                _courses.value = courseList.filter {
                    !it.name.equals("Placeholder", ignoreCase = true) &&
                    !it.name.contains("Cancelled", ignoreCase = true)
                }
            }
        }

        viewModelScope.launch {
            repository.getSelectedCourses().collect { selectedList ->
                _selectedCourses.value = selectedList
            }
        }
    }

    fun searchCourses(query: String) {
        viewModelScope.launch {
            if (query.isEmpty()) {
                repository.getAllCourses().collect { courseList ->
                    _courses.value = courseList.filter {
                        !it.name.equals("Placeholder", ignoreCase = true) &&
                        !it.name.contains("Cancelled", ignoreCase = true)
                    }
                }
            } else {
                repository.searchCourses(query).collect { courseList ->
                    _courses.value = courseList.filter {
                        !it.name.equals("Placeholder", ignoreCase = true) &&
                        !it.name.contains("Cancelled", ignoreCase = true)
                    }
                }
            }
        }
    }

    fun toggleCourseSelection(course: Course) {
        viewModelScope.launch {
            repository.toggleCourseSelection(course)
        }
    }

    fun clearAllSelections() {
        viewModelScope.launch {
            repository.clearAllSelections()
        }
    }

    /**
     * Unified Schedule Preview:
     * Generates timetable for selected courses and merges with existing dashboard courses,
     * badging each with [Existing] and [New]
     */
    fun generateCustomTimetable() {
        val selected = _selectedCourses.value ?: emptyList()

        if (selected.isEmpty()) {
            _errorMessage.value = "Please select at least one course"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.getCustomTimetable(selected)

            result.onSuccess { newSessions ->
                newSessions.forEach { it.previewTag = "New" }

                // Retrieve existing dashboard courses to display combined schedule
                val existingDashboard = try {
                    database.dashboardDao().getAllDashboardSessionsOnce()
                } catch (e: Exception) {
                    emptyList()
                }

                val existingAsTimetable = existingDashboard.map { dbSession ->
                    dbSession.toTimetableSession().apply { previewTag = "Existing" }
                }

                // Combine both and eliminate exact duplicates
                val combined = (existingAsTimetable + newSessions).distinctBy {
                    "${it.day}_${it.timeSlot}_${it.courseName}_${it.section}"
                }.sortedWith(compareBy({ it.day }, { it.rank }, { it.timeSlot }))

                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    _timetableSessions.value = combined
                    if (combined.isEmpty()) {
                        _errorMessage.value = "No classes found for selected courses"
                    }
                }
            }.onFailure { exception ->
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    _errorMessage.value = exception.message ?: "Failed to generate timetable"
                }
            }
        }
    }

    fun addToDashboard() {
        val selected = _selectedCourses.value ?: emptyList()

        if (selected.isEmpty()) {
            _errorMessage.value = "Please select courses first"
            return
        }

        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.addCustomCoursesToDashboard(selected)
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Courses added to dashboard!"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Failed to add to dashboard: ${e.message}"
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * Cache-first loading: Query Room database first.
     * If cached courses exist, emit them immediately and do not perform a network sync.
     * Only fetch from network if local cache is empty or user requested forceRefresh.
     */
    fun loadOrSyncData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val existingRealCourses = withContext(Dispatchers.IO) {
                database.courseDao().getAllCoursesOnce().filter {
                    !it.name.equals("Placeholder", ignoreCase = true)
                }
            }

            if (existingRealCourses.isNotEmpty() && !forceRefresh) {
                // Emit cached courses immediately without network sync
                _courses.value = existingRealCourses.filter {
                    !it.name.contains("Cancelled", ignoreCase = true)
                }
                _isLoading.value = false
                return@launch
            }

            // Fetch from network when cache is empty or forceRefresh requested
            _isLoading.value = true
            val result = repository.syncData(forceRefresh = forceRefresh)
            _isLoading.value = false

            result.onSuccess {
                val freshCourses = withContext(Dispatchers.IO) {
                    database.courseDao().getAllCoursesOnce().filter {
                        !it.name.equals("Placeholder", ignoreCase = true) &&
                        !it.name.contains("Cancelled", ignoreCase = true)
                    }
                }
                _courses.value = freshCourses
                if (forceRefresh) {
                    _errorMessage.value = "Data synced successfully"
                }
            }.onFailure { exception ->
                _errorMessage.value = "Sync failed: ${exception.message}"
            }
        }
    }

    fun syncData() {
        loadOrSyncData(forceRefresh = true)
    }

    fun getSessionsByDay(day: String): List<TimetableSession> {
        return _timetableSessions.value?.filter { it.day == day } ?: emptyList()
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
