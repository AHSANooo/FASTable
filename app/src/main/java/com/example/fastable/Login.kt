package com.example.fastable

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        var loginButton = findViewById<Button>(R.id.btn_login)
        loginButton.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
        }

        var signUpButton = findViewById<TextView>(R.id.signup_link_text)
        signUpButton.setOnClickListener {
            val intent = Intent(this, Sign_Up::class.java)
            startActivity(intent)
        }

    }
}