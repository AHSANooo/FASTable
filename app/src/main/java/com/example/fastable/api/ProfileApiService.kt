package com.example.fastable.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * API Service for FASTable Web APIs (OPTIMIZED)
 * Base URL: https://sociallyhub.dpdns.org/fastable/
 *
 * Purpose: Handle ONLY profile pictures
 * User details stored in: Local DB + Firebase (not MySQL)
 */
object ProfileApiService {

    private const val TAG = "ProfileApiService"
    private const val BASE_URL = "https://sociallyhub.dpdns.org/fastable/"

    /**
     * Upload profile picture to MySQL server
     * This is the ONLY data stored in MySQL
     */
    suspend fun uploadProfilePicture(
        userId: String,
        bitmap: Bitmap
    ): ApiResponse = withContext(Dispatchers.IO) {
        try {
            // Convert bitmap to base64
            val base64Image = bitmapToBase64(bitmap)

            val url = URL("${BASE_URL}upload_profile_picture.php")
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connectTimeout = 30000 // 30 seconds for image upload
                readTimeout = 30000
            }

            val postData = "user_id=$userId&image=$base64Image"

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(postData)
                writer.flush()
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "uploadProfilePicture responseCode: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "uploadProfilePicture response: $response")
                parseResponse(response)
            } else {
                ApiResponse(false, "HTTP Error: $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "uploadProfilePicture error", e)
            ApiResponse(false, "Error: ${e.message}")
        }
    }

    /**
     * Get profile picture URL from MySQL server
     */
    suspend fun getProfilePictureUrl(userId: String): ApiResponse = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}get_profile_picture.php")
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val postData = "user_id=$userId"

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(postData)
                writer.flush()
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "getProfilePictureUrl responseCode: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "getProfilePictureUrl response: $response")
                parseResponse(response)
            } else {
                ApiResponse(false, "HTTP Error: $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "getProfilePictureUrl error", e)
            ApiResponse(false, "Error: ${e.message}")
        }
    }

    /**
     * Convert Bitmap to Base64 string
     * Using JPEG format with proper Base64 encoding (same as a23i project)
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()

        // Compress bitmap to JPEG with 60% quality (optimal balance)
        // This matches the working implementation from a23i project
        val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)

        if (!compressed) {
            Log.e(TAG, "Bitmap compression failed!")
        }

        val byteArray = outputStream.toByteArray()
        Log.d(TAG, "Compressed image size: ${byteArray.size} bytes (${byteArray.size / 1024} KB)")

        // Encode to Base64 with DEFAULT flag (includes line breaks for proper decoding)
        // This is CRUCIAL - NO_WRAP causes corruption!
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    /**
     * Parse JSON response
     */
    private fun parseResponse(jsonString: String): ApiResponse {
        return try {
            val json = JSONObject(jsonString)
            val status = json.getInt("status")
            val message = json.getString("message")

            val data = if (json.has("data")) {
                // Handle nested data object (e.g., get_profile_picture returns object)
                val dataObj = json.get("data")
                if (dataObj is JSONObject && dataObj.has("profile_picture_url")) {
                    // Extract profile_picture_url from nested object
                    dataObj.getString("profile_picture_url")
                } else {
                    // Fallback to string representation
                    dataObj.toString()
                }
            } else if (json.has("image_url")) {
                json.getString("image_url")
            } else {
                null
            }

            ApiResponse(status == 1, message, data)
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse error", e)
            ApiResponse(false, "Parse error: ${e.message}")
        }
    }
}

/**
 * API Response data class
 */
data class ApiResponse(
    val success: Boolean,
    val message: String,
    val data: String? = null
)

