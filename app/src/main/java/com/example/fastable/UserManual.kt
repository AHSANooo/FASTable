package com.example.fastable

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class UserManual : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_manual)

        // Handle back button
        val backBtn = findViewById<ImageView>(R.id.backBtn)
        backBtn.setOnClickListener {
            finish()
        }
    }
}

