package com.example.fastable

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fastable.data.local.AppDatabase
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SplashScreen : AppCompatActivity() {

    private val TAG = "SplashScreen"
    private val GITHUB_REPO_LATEST_RELEASE = "https://api.github.com/repos/AHSANooo/FASTable/releases/latest"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        checkForUpdates()
    }

    private fun checkForUpdates() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(GITHUB_REPO_LATEST_RELEASE)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("User-Agent", "FASTable-Android")
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseText)
                    val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                    val releaseName = json.optString("name", "New Update")
                    val releaseNotes = json.optString("body", "Bug fixes and performance improvements.")
                    val htmlUrl = json.optString("html_url", "https://github.com/AHSANooo/FASTable/releases")

                    // Look for .apk asset download URL
                    var downloadUrl = htmlUrl
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val assetName = asset.optString("name", "")
                            if (assetName.endsWith(".apk", ignoreCase = true)) {
                                downloadUrl = asset.optString("browser_download_url", htmlUrl)
                                break
                            }
                        }
                    }

                    val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").trim()
                    Log.d(TAG, "Current: $currentVersion, Latest: $tagName")

                    if (isNewerVersion(tagName, currentVersion)) {
                        withContext(Dispatchers.Main) {
                            showUpdateDialog(releaseName, releaseNotes, downloadUrl)
                        }
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Update check failed or offline: ${e.message}")
            }

            withContext(Dispatchers.Main) {
                proceedToApp()
            }
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        if (latest.isEmpty() || current.isEmpty()) return false
        val latestParts = latest.split(".").mapNotNull { it.filter { ch -> ch.isDigit() }.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.filter { ch -> ch.isDigit() }.toIntOrNull() }

        val length = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until length) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    private fun showUpdateDialog(title: String, notes: String, downloadUrl: String) {
        val trimmedNotes = if (notes.length > 200) notes.substring(0, 197) + "..." else notes
        MaterialAlertDialogBuilder(this)
            .setTitle("Update Available: $title")
            .setMessage("$trimmedNotes\n\nWould you like to update FASTable now?")
            .setCancelable(false)
            .setPositiveButton("Update") { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    proceedToApp()
                }
            }
            .setNegativeButton("Later") { _, _ ->
                proceedToApp()
            }
            .show()
    }

    private fun proceedToApp() {
        Handler(Looper.getMainLooper()).postDelayed({
            lifecycleScope.launch(Dispatchers.IO) {
                val currentUser = FirebaseAuth.getInstance().currentUser
                val hasLocalProfile = AppDatabase.getDatabase(this@SplashScreen)
                    .userProfileDao().getAnyUserProfile() != null

                withContext(Dispatchers.Main) {
                    // If user is authenticated in Firebase OR has existing profile in Room DB, go straight to Home
                    val intent = if ((currentUser != null && currentUser.isEmailVerified) || hasLocalProfile) {
                        Intent(this@SplashScreen, Home::class.java)
                    } else {
                        Intent(this@SplashScreen, Login::class.java)
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }, 600)
    }
}
