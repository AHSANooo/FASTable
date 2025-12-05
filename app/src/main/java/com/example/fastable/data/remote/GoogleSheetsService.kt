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
        // Use NetHttpTransport with timeouts for Android compatibility
        val httpTransport = NetHttpTransport.Builder()
            .setConnectionFactory { url ->
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 15000 // 15 seconds
                connection.readTimeout = 30000 // 30 seconds
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
     */
    suspend fun fetchSpreadsheet(): com.google.api.services.sheets.v4.model.Spreadsheet? {
        return withContext(Dispatchers.IO) {
            try {
                // 20-second timeout for better UX
                val result = withTimeout(TimeUnit.SECONDS.toMillis(20)) {
                    val service = getSheetsService()

                    // Fetch spreadsheet with includeGridData to get cell formatting and colors
                    val request = service.spreadsheets()
                        .get(GoogleSheetsConfig.SPREADSHEET_ID)
                        .setIncludeGridData(true)

                    request.execute()
                }

                result
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e(TAG, "Request timeout")
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

