package com.example.fastable.data.local

import androidx.room.*
import com.example.fastable.data.models.TimetableSession
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable_sessions ORDER BY day, rank, timeSlot")
    fun getAllSessions(): Flow<List<TimetableSession>>

    @Query("SELECT * FROM timetable_sessions WHERE day = :day ORDER BY rank, timeSlot")
    fun getSessionsByDay(day: String): Flow<List<TimetableSession>>

    @Query("SELECT * FROM timetable_sessions WHERE batch = :batch AND section = :section ORDER BY day, rank, timeSlot")
    fun getSessionsByBatchAndSection(batch: String, section: String): Flow<List<TimetableSession>>

    @Query("SELECT * FROM timetable_sessions WHERE batch = :batch AND section = :section ORDER BY day, rank, timeSlot")
    suspend fun getSessionsByBatchAndSectionOnce(batch: String, section: String): List<TimetableSession>

    @Query("SELECT * FROM timetable_sessions WHERE isCustom = 1 ORDER BY day, rank, timeSlot")
    fun getCustomSessions(): Flow<List<TimetableSession>>

    @Query("SELECT DISTINCT day FROM timetable_sessions WHERE batch = :batch AND section = :section ORDER BY CASE day WHEN 'Monday' THEN 1 WHEN 'Tuesday' THEN 2 WHEN 'Wednesday' THEN 3 WHEN 'Thursday' THEN 4 WHEN 'Friday' THEN 5 END")
    fun getDaysForBatchAndSection(batch: String, section: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TimetableSession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<TimetableSession>)

    @Update
    suspend fun updateSession(session: TimetableSession)

    @Delete
    suspend fun deleteSession(session: TimetableSession)

    @Query("DELETE FROM timetable_sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM timetable_sessions WHERE batch = :batch AND section = :section")
    suspend fun deleteSessionsForBatchAndSection(batch: String, section: String)

    @Query("DELETE FROM timetable_sessions WHERE isCustom = 1")
    suspend fun deleteCustomSessions()
}
