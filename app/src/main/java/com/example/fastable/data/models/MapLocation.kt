package com.example.fastable.data.models

/**
 * Data class representing a room/lab location on campus
 */
data class MapLocation(
    val roomName: String,
    val location: String,
    val colorCode: Int  // Color resource ID for the indicator
)

