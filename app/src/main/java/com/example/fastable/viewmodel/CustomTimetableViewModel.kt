package com.example.fastable.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fastable.data.models.Course
import com.example.fastable.data.models.TimetableSession
import com.example.fastable.data.repository.TimetableRepository
import kotlinx.coroutines.launch

class CustomTimetableViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TimetableRepository(application)

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
            // Load departments
            repository.getDepartments().collect { deptList ->
                _departments.value = deptList
            }
        }

        viewModelScope.launch {
            // Load batches
            repository.getBatches().collect { batchList ->
                _batches.value = batchList
            }
        }

        viewModelScope.launch {
            // Load all courses
            repository.getAllCourses().collect { courseList ->
                _courses.value = courseList
            }
        }

        viewModelScope.launch {
            // Load selected courses
            repository.getSelectedCourses().collect { selectedList ->
                _selectedCourses.value = selectedList
            }
        }
    }

    fun searchCourses(query: String) {
        viewModelScope.launch {
            if (query.isEmpty()) {
                repository.getAllCourses().collect { courseList ->
                    _courses.value = courseList
                }
            } else {
                repository.searchCourses(query).collect { courseList ->
                    _courses.value = courseList
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

    fun generateCustomTimetable() {
        val selected = _selectedCourses.value ?: emptyList()

        if (selected.isEmpty()) {
            _errorMessage.value = "Please select at least one course"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = repository.getCustomTimetable(selected)
            _isLoading.value = false

            result.onSuccess { sessions ->
                _timetableSessions.value = sessions
                if (sessions.isEmpty()) {
                    _errorMessage.value = "No classes found for selected courses"
                }
            }.onFailure { exception ->
                _errorMessage.value = exception.message ?: "Failed to generate timetable"
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
        // Use GlobalScope to prevent cancellation when activity is destroyed
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                repository.addCustomCoursesToDashboard(selected)

                // Post success on main thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _errorMessage.value = "Courses added to dashboard!"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                // Post error on main thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _errorMessage.value = "Failed to add to dashboard: ${e.message}"
                    _isLoading.value = false
                }
            }
        }
    }

    fun syncData() {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.syncData(forceRefresh = true)
            _isLoading.value = false

            result.onSuccess {
                _errorMessage.value = "Data synced successfully"
            }.onFailure { exception ->
                _errorMessage.value = "Sync failed: ${exception.message}"
            }
        }
    }

    fun getSessionsByDay(day: String): List<TimetableSession> {
        return _timetableSessions.value?.filter { it.day == day } ?: emptyList()
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

