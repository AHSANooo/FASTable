package com.example.fastable.data.dao

import androidx.room.*
import com.example.fastable.data.models.DashboardSession
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardDao {

    @Query("SELECT * FROM dashboard_sessions ORDER BY day, timeSlot")
    fun getAllDashboardSessions(): Flow<List<DashboardSession>>

    @Query("SELECT * FROM dashboard_sessions WHERE day = :day ORDER BY timeSlot")
    fun getDashboardSessionsByDay(day: String): Flow<List<DashboardSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDashboardSession(session: DashboardSession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDashboardSessions(sessions: List<DashboardSession>)

    @Delete
    suspend fun deleteDashboardSession(session: DashboardSession)

    @Query("DELETE FROM dashboard_sessions WHERE isCustom = 0")
    suspend fun deleteBatchSessions()

    @Query("DELETE FROM dashboard_sessions WHERE isCustom = 1")
    suspend fun deleteCustomSessions()

    @Query("DELETE FROM dashboard_sessions")
    suspend fun deleteAllDashboardSessions()

    @Query("SELECT COUNT(*) FROM dashboard_sessions")
    suspend fun getDashboardSessionCount(): Int
}

