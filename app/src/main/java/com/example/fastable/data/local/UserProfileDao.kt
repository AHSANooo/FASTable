package com.example.fastable.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fastable.data.models.UserProfile

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profile WHERE uid = :uid LIMIT 1")
    suspend fun getUserProfile(uid: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    @Update
    suspend fun updateUserProfile(profile: UserProfile)

    @Query("UPDATE user_profile SET batch = :batch, degree = :degree, section = :section WHERE uid = :uid")
    suspend fun updateProfileFields(uid: String, batch: String, degree: String, section: String)

    @Query("UPDATE user_profile SET profileImageUrl = :imageUrl WHERE uid = :uid")
    suspend fun updateProfileImage(uid: String, imageUrl: String)

    @Query("DELETE FROM user_profile WHERE uid = :uid")
    suspend fun deleteUserProfile(uid: String)

    @Query("DELETE FROM user_profile")
    suspend fun deleteAllProfiles()
}

