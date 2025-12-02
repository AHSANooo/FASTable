package com.example.fastable

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // Delay for 1 second and then check login status
        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = FirebaseAuth.getInstance().currentUser

            val intent = if (currentUser != null && currentUser.isEmailVerified) {
                // User is already logged in and verified, go to Home
                Intent(this, Home::class.java)
            } else {
                // User not logged in or not verified, go to Login
                Intent(this, Login::class.java)
            }

            startActivity(intent)
            finish()
        }, 500)
    }
}

