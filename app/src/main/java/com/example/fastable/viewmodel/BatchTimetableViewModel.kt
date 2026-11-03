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
        viewModelScope.launch {
            Log.d(TAG, "Initializing with batch sync...")
            repository.syncBatchesOnly()
        }
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
        _isLoading.value = true

        viewModelScope.launch {
            val result = repository.getBatchTimetable(batch, section, forceFresh = false)
            _isLoading.value = false

            result.onSuccess { sessions ->
                _timetableSessions.value = sessions
                if (sessions.isEmpty()) {
                    _errorMessage.value = "No classes found for $batch - Section $section"
                } else {
                    _errorMessage.value = null
                }
            }.onFailure { exception ->
                _errorMessage.value = exception.message ?: "Failed to load timetable"
            }
        }
    }

    fun refreshTimetable() {
        if (currentBatch.isNotEmpty() && currentSection.isNotEmpty()) {
            _errorMessage.value = null
            _isLoading.value = true
            viewModelScope.launch {
                val result = repository.getBatchTimetable(currentBatch, currentSection, forceFresh = true)
                _isLoading.value = false
                result.onSuccess { sessions ->
                    _timetableSessions.value = sessions
                    _errorMessage.value = if (sessions.isEmpty()) "No classes found for $currentBatch - Section $currentSection" else null
                }.onFailure { exception ->
                    _errorMessage.value = exception.message ?: "Failed to refresh timetable"
                }
            }
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
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                Log.d(TAG, "setDefaultBatch: Calling repository for $batch - Section $section")
                repository.setDefaultBatch(batch, section)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _setAsDefaultStatus.value = true
                    _errorMessage.value = "Default batch set to $batch - Section $section"
                }
            } catch (e: Exception) {
                Log.e(TAG, "setDefaultBatch: Exception", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _setAsDefaultStatus.value = false
                    _errorMessage.value = "Error: ${e.message}"
                }
            }
        }
    }

    fun resetSetAsDefaultStatus() {
        _setAsDefaultStatus.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
