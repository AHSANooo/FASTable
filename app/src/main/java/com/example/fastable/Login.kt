package com.example.fastable

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth

class Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val emailEt = findViewById<EditText>(R.id.email)
        val passwordEt = findViewById<EditText>(R.id.password)
        val loginBtn = findViewById<Button>(R.id.btn_login)
        val signUpLinkText = findViewById<TextView>(R.id.signup_link_text)

        // Navigate to sign up
        signUpLinkText.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }

        loginBtn.setOnClickListener {
            val email = emailEt.text.toString().trim()
            val password = passwordEt.text.toString().trim()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEt.error = "Enter valid email"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordEt.error = "Enter password"
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null && user.isEmailVerified) {
                            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, Home::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            // Email not verified
                            auth.signOut()
                            showVerifyDialog(email)
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Login failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    private fun showVerifyDialog(email: String) {
        AlertDialog.Builder(this)
            .setTitle("Email Verification Required")
            .setMessage("Your email address has not been verified yet.\n\nWould you like to resend the verification email?\n\n✉️ Check your inbox and spam folder.")
            .setPositiveButton("Resend Email") { _, _ ->
                resendVerificationEmail(email)
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }

    private fun resendVerificationEmail(email: String) {
        // Re-authenticate and send verification email
        val user = auth.currentUser

        if (user != null) {
            // Create ActionCodeSettings for proper email link
            val actionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl("https://fastable.page.link/verify?email=$email")
                .setHandleCodeInApp(false)
                .setAndroidPackageName(
                    "com.example.fastable",
                    true,  // installIfNotAvailable
                    null   // minimumVersion
                )
                .build()

            user.sendEmailVerification(actionCodeSettings)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        android.util.Log.d("Login", "Verification email resent successfully to $email")
                        Toast.makeText(
                            this,
                            "✅ Verification email resent to $email\n\nCheck your inbox and spam folder.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        val errorMessage = task.exception?.message ?: "Unknown error"
                        android.util.Log.e("Login", "Failed to resend verification email: $errorMessage")
                        Toast.makeText(
                            this,
                            "❌ Failed to resend email: $errorMessage",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        } else {
            Toast.makeText(
                this,
                "⚠️ User session expired. Please try again.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
