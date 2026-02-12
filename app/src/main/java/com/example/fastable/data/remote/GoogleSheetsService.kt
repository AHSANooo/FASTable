package com.example.fastable.data.remote

import android.content.Context
import android.util.Log
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.ServiceAccountCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit

class GoogleSheetsService(private val context: Context) {

    private val TAG = "GoogleSheetsService"
    private val jsonFactory = GsonFactory.getDefaultInstance()

    private fun getCredentials(): ServiceAccountCredentials {
        val credentialsJson = """
        {
          "type": "${GoogleSheetsConfig.TYPE}",
          "project_id": "${GoogleSheetsConfig.PROJECT_ID}",
          "private_key_id": "${GoogleSheetsConfig.PRIVATE_KEY_ID}",
          "private_key": "${GoogleSheetsConfig.PRIVATE_KEY}",
          "client_email": "${GoogleSheetsConfig.CLIENT_EMAIL}",
          "client_id": "${GoogleSheetsConfig.CLIENT_ID}",
          "auth_uri": "${GoogleSheetsConfig.AUTH_URI}",
          "token_uri": "${GoogleSheetsConfig.TOKEN_URI}",
          "auth_provider_x509_cert_url": "${GoogleSheetsConfig.AUTH_PROVIDER_CERT_URL}",
          "client_x509_cert_url": "${GoogleSheetsConfig.CLIENT_CERT_URL}",
          "universe_domain": "${GoogleSheetsConfig.UNIVERSE_DOMAIN}"
        }
        """.trimIndent()

        return ServiceAccountCredentials.fromStream(
            ByteArrayInputStream(credentialsJson.toByteArray())
        ).createScoped(listOf(SheetsScopes.SPREADSHEETS_READONLY)) as ServiceAccountCredentials
    }

    private fun getSheetsService(): Sheets {
        // Use NetHttpTransport with optimized timeouts
        val httpTransport = NetHttpTransport.Builder()
            .setConnectionFactory { url ->
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 8000 // 8 seconds
                connection.readTimeout = 12000 // 12 seconds
                connection
            }
            .build()

        val credentials = getCredentials()

        return Sheets.Builder(httpTransport, jsonFactory, HttpCredentialsAdapter(credentials))
            .setApplicationName("FASTable")
            .build()
    }

    /**
     * Fetch spreadsheet data with grid data (includes formatting and colors)
     * Optimized: Single API call to fetch all data
     * Now uses dynamic spreadsheet ID from SpreadsheetConfigManager
     */
    suspend fun fetchSpreadsheet(): com.google.api.services.sheets.v4.model.Spreadsheet? {
        return withContext(Dispatchers.IO) {
            try {
                // Get the spreadsheet ID (from cache or Firebase)
                val spreadsheetId = SpreadsheetConfigManager.getSpreadsheetId(context)
                Log.d(TAG, "Using spreadsheet ID: $spreadsheetId")

                val service = getSheetsService()

                // Single API call - fetch entire spreadsheet with grid data
                // The API will return all sheets, we filter on the client side
                val result = withTimeout(TimeUnit.SECONDS.toMillis(15)) {
                    val request = service.spreadsheets()
                        .get(spreadsheetId)
                        .setIncludeGridData(true)

                    request.execute()
                }

                // Filter to only timetable sheets on client side (fast operation)
                val dayKeywords = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                result.sheets = result.sheets?.filter { sheet ->
                    val sheetName = sheet.properties?.title ?: ""
                    dayKeywords.any { day -> sheetName.contains(day, ignoreCase = true) }
                }

                Log.d(TAG, "Fetched ${result.sheets?.size ?: 0} timetable sheets")
                result
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e(TAG, "Request timeout after 15 seconds")
                null
            } catch (e: java.net.SocketTimeoutException) {
                Log.e(TAG, "Network timeout")
                null
            } catch (e: com.google.api.client.googleapis.json.GoogleJsonResponseException) {
                if (e.statusCode == 403) {
                    Log.e(TAG, "Permission denied - share sheet with: ${GoogleSheetsConfig.CLIENT_EMAIL}")
                }
                null
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching spreadsheet: ${e.message}")
                null
            }
        }
    }


    /**
     * Fetch ONLY batch header rows (first 5 rows) from one day - SUPER FAST
     * This is used to extract available batches without fetching the entire spreadsheet
     */
    suspend fun fetchBatchHeaders(): com.google.api.services.sheets.v4.model.Spreadsheet? {
        return withContext(Dispatchers.IO) {
            try {
                val spreadsheetId = SpreadsheetConfigManager.getSpreadsheetId(context)
                Log.d(TAG, "Fetching batch headers (fast mode)")

                val result = withTimeout(TimeUnit.SECONDS.toMillis(5)) {
                    val service = getSheetsService()

                    // Only fetch first 5 rows from Monday (enough to get batch colors)
                    val ranges = listOf("Monday!A1:AN5")

                    val request = service.spreadsheets()
                        .get(spreadsheetId)
                        .setIncludeGridData(true)
                        .setRanges(ranges)

                    request.execute()
                }

                Log.d(TAG, "Batch headers fetched successfully")
                result
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching batch headers: ${e.message}")
                null
            }
        }
    }

    /**
     * Fetch specific sheet by name
     */
    suspend fun fetchSheet(sheetName: String): com.google.api.services.sheets.v4.model.Sheet? {
        return withContext(Dispatchers.IO) {
            try {
                val spreadsheet = fetchSpreadsheet()
                spreadsheet?.sheets?.firstOrNull {
                    it.properties?.title == sheetName
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Check if online
     */
    fun isOnline(): Boolean {
        return try {
            val runtime = Runtime.getRuntime()
            val process = runtime.exec("/system/bin/ping -c 1 8.8.8.8")
            val exitValue = process.waitFor()
            exitValue == 0
        } catch (e: Exception) {
            false
        }
    }
}

