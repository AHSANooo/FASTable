package com.example.fastable.utils

import android.util.Log
import com.google.api.services.sheets.v4.model.CellData

/**
 * Helper object for Google Sheets API v4 data access
 * Provides safe methods to access cell properties
 */
object SheetsHelper {

    private const val TAG = "SheetsHelper"

    fun getFormattedValue(cell: Any?): String? {
        return try {
            when (cell) {
                is CellData -> cell.formattedValue
                is ArrayList<*> -> {
                    // Google Sheets API sometimes nests CellData in ArrayList
                    val firstElement = cell.firstOrNull()
                    if (firstElement is CellData) {
                        firstElement.formattedValue
                    } else {
                        Log.w(TAG, "ArrayList contains unexpected type: ${firstElement?.javaClass?.name}")
                        null
                    }
                }
                else -> {
                    Log.w(TAG, "Unexpected cell type: ${cell?.javaClass?.name}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting formatted value", e)
            null
        }
    }

    fun getBackgroundColor(cell: Any?): String {
        return try {
            val cellData = when (cell) {
                is CellData -> cell
                is ArrayList<*> -> cell.firstOrNull() as? CellData
                else -> null
            } ?: return ""

            val format = cellData.effectiveFormat ?: return ""
            val bgColor = format.backgroundColor ?: return ""
            val r = bgColor.red ?: 0f
            val g = bgColor.green ?: 0f
            val b = bgColor.blue ?: 0f
            String.format(java.util.Locale.US, "%.2f%.2f%.2f", r, g, b)
        } catch (e: Exception) {
            ""
        }
    }

    fun getCellAt(collection: MutableCollection<Any>?, index: Int): Any? {
        return try {
            val list = collection?.toList()
            if (list != null && index < list.size) {
                list[index]
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

