package com.example.fastable.data.local

import androidx.room.*
import com.example.fastable.data.models.Course
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses")
    fun getAllCourses(): Flow<List<Course>>

    @Query("SELECT * FROM courses")
    suspend fun getAllCoursesOnce(): List<Course>

    @Query("SELECT * FROM courses WHERE isSelected = 1")
    fun getSelectedCourses(): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE department = :department")
    fun getCoursesByDepartment(department: String): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE batch LIKE '%' || :year || '%'")
    fun getCoursesByYear(year: String): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE name LIKE '%' || :query || '%' OR department LIKE '%' || :query || '%'")
    fun searchCourses(query: String): Flow<List<Course>>

    @Query("SELECT DISTINCT department FROM courses ORDER BY department")
    fun getAllDepartments(): Flow<List<String>>

    @Query("SELECT DISTINCT batch FROM courses ORDER BY batch")
    fun getAllBatches(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<Course>)

    @Update
    suspend fun updateCourse(course: Course)

    @Delete
    suspend fun deleteCourse(course: Course)

    @Query("DELETE FROM courses")
    suspend fun deleteAllCourses()

    @Query("UPDATE courses SET isSelected = :isSelected WHERE id = :courseId")
    suspend fun updateCourseSelection(courseId: Long, isSelected: Boolean)

    @Query("UPDATE courses SET isSelected = 0")
    suspend fun clearAllSelections()
}

