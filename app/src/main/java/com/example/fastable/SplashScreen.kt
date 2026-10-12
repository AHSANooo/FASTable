package com.example.fastable

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        
        checkForUpdates()
    }

    private fun checkForUpdates() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600) // fetch once an hour max in production
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)

        // Set default values in case of no internet
        val defaults = mapOf(
            "latest_app_version" to com.example.fastable.BuildConfig.VERSION_CODE.toLong(),
            "download_url" to "https://github.com"
        )
        remoteConfig.setDefaultsAsync(defaults)

        remoteConfig.fetchAndActivate().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val latestVersion = remoteConfig.getLong("latest_app_version").toInt()
                val downloadUrl = remoteConfig.getString("download_url")
                val currentVersion = com.example.fastable.BuildConfig.VERSION_CODE

                if (latestVersion > currentVersion) {
                    showUpdateDialog(downloadUrl)
                } else {
                    proceedToApp()
                }
            } else {
                proceedToApp() // Failed to fetch, just let them in
            }
        }
    }

    private fun showUpdateDialog(downloadUrl: String) {
        AlertDialog.Builder(this)
            .setTitle("New Version Available")
            .setMessage("Please update the app to the latest version to continue.")
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
            val currentUser = FirebaseAuth.getInstance().currentUser

            val intent = if (currentUser != null && currentUser.isEmailVerified) {
                Intent(this, Home::class.java)
            } else {
                Intent(this, Login::class.java)
            }

            startActivity(intent)
            finish()
        }, 500)
    }
}

