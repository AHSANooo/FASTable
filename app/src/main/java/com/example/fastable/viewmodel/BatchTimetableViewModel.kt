package com.example.fastable.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fastable.data.models.TimetableSession
import com.example.fastable.data.repository.TimetableRepository
import kotlinx.coroutines.launch

class BatchTimetableViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "BatchTimetableViewModel"
    private val repository = TimetableRepository(application)

    private val _timetableSessions = MutableLiveData<List<TimetableSession>>()
    val timetableSessions: LiveData<List<TimetableSession>> = _timetableSessions

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _batches = MutableLiveData<List<String>>()
    val batches: LiveData<List<String>> = _batches

    private val _setAsDefaultStatus = MutableLiveData<Boolean?>()
    val setAsDefaultStatus: LiveData<Boolean?> = _setAsDefaultStatus

    private var currentBatch: String = ""
    private var currentSection: String = ""

    init {
        loadBatches()
    }

    private fun loadBatches() {
        viewModelScope.launch {
            repository.getBatches().collect { batchList ->
                _batches.value = batchList
            }
        }
    }

    fun loadTimetable(batch: String, section: String) {
        currentBatch = batch
        currentSection = section
        _errorMessage.value = null

        viewModelScope.launch {
            // STEP 1: Load from database IMMEDIATELY (no waiting)
            val cachedSessions = repository.getSessionsFromDatabaseOnce(batch, section)
            if (cachedSessions.isNotEmpty()) {
                _timetableSessions.value = cachedSessions
            } else {
                _errorMessage.value = "Loading..."
            }

            // STEP 2: Fetch from API and update
            val result = repository.getBatchTimetable(batch, section)
            result.onSuccess { sessions ->
                _timetableSessions.value = sessions
                if (sessions.isEmpty()) {
                    _errorMessage.value = "No classes found for $batch - Section $section"
                } else {
                    _errorMessage.value = null
                }
            }.onFailure { exception ->
                if (_timetableSessions.value.isNullOrEmpty()) {
                    _errorMessage.value = exception.message ?: "Failed to load timetable"
                }
            }
        }
    }

    fun refreshTimetable() {
        if (currentBatch.isNotEmpty() && currentSection.isNotEmpty()) {
            loadTimetable(currentBatch, currentSection)
        }
    }

    fun syncData() {
        viewModelScope.launch {
            repository.syncData(forceRefresh = true)
        }
    }

    fun getSessionsByDay(day: String): List<TimetableSession> {
        return _timetableSessions.value?.filter { it.day == day } ?: emptyList()
    }

    fun setDefaultBatch(batch: String, section: String) {
        // Use GlobalScope to prevent cancellation when activity is destroyed
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                Log.d(TAG, "setDefaultBatch: Calling repository for $batch - Section $section")
                repository.setDefaultBatch(batch, section)
                Log.d(TAG, "setDefaultBatch: Successfully set default batch")

                // Post success on main thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _errorMessage.value = "Batch set as default!"
                    _setAsDefaultStatus.value = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "setDefaultBatch: Failed", e)

                // Post error on main thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _errorMessage.value = "Failed to set as default: ${e.message}"
                    _setAsDefaultStatus.value = false
                }
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

