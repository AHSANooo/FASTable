package com.example.fastable.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "default_batch")
data class DefaultBatch(
    @PrimaryKey
    val id: Int = 1, // Only one default batch at a time
    val batchName: String,
    val section: String,
    val timestamp: Long = System.currentTimeMillis()
)

