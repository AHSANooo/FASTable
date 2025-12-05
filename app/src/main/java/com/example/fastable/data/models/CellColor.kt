package com.example.fastable.data.models

// Color representation from Google Sheets
data class CellColor(
    val red: Float = 0f,
    val green: Float = 0f,
    val blue: Float = 0f
) {
    fun toHexString(): String {
        return String.format("%.2f%.2f%.2f", red, green, blue)
    }

    companion object {
        fun fromHexString(hex: String): CellColor {
            return try {
                val red = hex.substring(0, 4).toFloat()
                val green = hex.substring(4, 8).toFloat()
                val blue = hex.substring(8, 12).toFloat()
                CellColor(red, green, blue)
            } catch (e: Exception) {
                CellColor()
            }
        }
    }
}

// Batch color mapping
data class BatchColor(
    val colorHex: String,
    val batchName: String
)

