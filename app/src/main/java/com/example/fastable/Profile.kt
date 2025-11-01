package com.example.fastable

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class Profile : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        val topAppBar = findViewById<Toolbar>(R.id.topAppBar)

        // Handle back navigation
        topAppBar.setNavigationOnClickListener {
            finish()
        }

        // Handle edit menu item
        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    // TODO: Implement edit profile functionality
                    Toast.makeText(this, "Edit profile coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }
}