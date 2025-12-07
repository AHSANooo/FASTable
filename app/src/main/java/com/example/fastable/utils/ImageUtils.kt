package com.example.fastable.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {
    /**
     * Convert Bitmap to Base64 string (JPEG compressed)
     * @param bitmap The bitmap to convert
     * @param quality Compression quality (0-100), default 70
     * @return Base64 encoded string
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 70): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        val bytes = baos.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    /**
     * Convert Base64 string to Bitmap
     * @param base64Str The base64 encoded string
     * @return Decoded Bitmap or null if decoding fails
     */
    fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            if (base64Str.isEmpty()) {
                null
            } else {
                val bytes = Base64.decode(base64Str, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

