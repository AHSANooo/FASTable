package com.example.fastable.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val uid: String,
    val name: String,
    val email: String,
    val batch: String,
    val degree: String,
    val section: String,
    val profileImageUrl: String,
    val lastSyncTime: Long = System.currentTimeMillis()
)

