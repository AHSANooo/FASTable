package com.example.fastable

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class OtpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_otp)
        val otpSubmitButton = findViewById<Button>(R.id.verifyButton)
        otpSubmitButton.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
            finish()
        }

    }
}


