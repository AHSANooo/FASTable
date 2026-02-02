package com.example.fastable.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Manages runtime permissions for the app
 * Handles POST_NOTIFICATIONS (Android 13+) and other permissions
 */
object PermissionManager {

    /**
     * Check if notification permission is granted
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // No permission needed for Android 12 and below
        }
    }

    /**
     * Request notification permission (Android 13+)
     * @param activity The activity requesting the permission
     * @param launcher The permission request launcher
     * @param showRationale Whether to show a rationale dialog first
     */
    fun requestNotificationPermission(
        activity: AppCompatActivity,
        launcher: ActivityResultLauncher<String>,
        showRationale: Boolean = false
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (showRationale) {
                // Show rationale dialog
                AlertDialog.Builder(activity)
                    .setTitle("Enable Notifications")
                    .setMessage("FASTable needs notification permission to alert you about class schedules and updates.")
                    .setPositiveButton("Allow") { _, _ ->
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    .setNegativeButton("Not Now", null)
                    .show()
            } else {
                // Directly request permission
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * Check if we should show rationale for notification permission
     */
    fun shouldShowNotificationRationale(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            false
        }
    }

    /**
     * Create a permission request launcher for notifications
     * Call this in onCreate() before requesting permissions
     */
    fun createNotificationPermissionLauncher(
        activity: AppCompatActivity,
        onGranted: () -> Unit = {},
        onDenied: () -> Unit = {}
    ): ActivityResultLauncher<String> {
        return activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                onGranted()
            } else {
                onDenied()
            }
        }
    }
}

