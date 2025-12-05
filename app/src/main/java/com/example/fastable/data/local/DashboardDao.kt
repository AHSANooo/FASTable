package com.example.fastable.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.fastable.data.models.DashboardSession
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardDao {

    // Dashboard Sessions
    @Query("SELECT * FROM dashboard_sessions WHERE day = :day ORDER BY timeSlot ASC")
    fun getSessionsByDay(day: String): Flow<List<DashboardSession>>

    @Query("SELECT * FROM dashboard_sessions")
    fun getAllDashboardSessions(): Flow<List<DashboardSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: DashboardSession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDashboardSessions(sessions: List<DashboardSession>)

    @Delete
    suspend fun deleteDashboardSession(session: DashboardSession)

    @Query("DELETE FROM dashboard_sessions WHERE isCustom = 0")
    suspend fun deleteBatchSessions()

    @Query("DELETE FROM dashboard_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Query("DELETE FROM dashboard_sessions")
    suspend fun clearAllSessions()

    // Get batch and custom sessions separately
    @Query("SELECT * FROM dashboard_sessions WHERE isCustom = 0 AND day = :day")
    fun getBatchSessionsByDay(day: String): Flow<List<DashboardSession>>

    @Query("SELECT * FROM dashboard_sessions WHERE isCustom = 1 AND day = :day")
    fun getCustomSessionsByDay(day: String): Flow<List<DashboardSession>>
}

