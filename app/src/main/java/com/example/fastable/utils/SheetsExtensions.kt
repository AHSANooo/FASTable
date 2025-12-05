package com.example.fastable.utils

import com.google.api.services.sheets.v4.model.CellData

/**
 * Extension functions to safely access Google Sheets API v4 model properties
 * Since the API returns MutableCollection<Any!>, we need to cast safely
 */

// Safe access to formattedValue
fun Any.getFormattedValueSafe(): String? {
    return try {
        (this as? CellData)?.formattedValue
    } catch (e: Exception) {
        null
    }
}

// Safe access to background color as string
fun Any.getBackgroundColorString(): String {
    return try {
        val cellData = this as? CellData ?: return ""
        val format = cellData.effectiveFormat ?: return ""
        val bgColor = format.backgroundColor ?: return ""
        val r = bgColor.red ?: 0f
        val g = bgColor.green ?: 0f
        val b = bgColor.blue ?: 0f
        String.format("%.2f%.2f%.2f", r, g, b)
    } catch (e: Exception) {
        ""
    }
}

// Safe access to collection element by index
fun MutableCollection<Any>.getAt(index: Int): Any? {
    return try {
        this.toList().getOrNull(index)
    } catch (e: Exception) {
        null
    }
}

