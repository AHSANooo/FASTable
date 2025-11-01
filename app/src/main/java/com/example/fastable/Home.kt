package com.example.fastable

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class Home : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            val intent = Intent(this, Menu::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        // Handle notification icon click
        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.notification_icon -> {
                    val intent = Intent(this, Notification::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

    }
}
