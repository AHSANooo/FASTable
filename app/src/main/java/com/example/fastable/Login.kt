package com.example.fastable

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fastable.data.local.AppDatabase
import com.example.fastable.data.models.UserProfile
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Login : AppCompatActivity() {

    private val TAG = "Login"
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var loginProgress: ProgressBar

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleGoogleSignInResult(task)
            } else {
                loginProgress.visibility = View.GONE
                Log.d(TAG, "Google Sign-In cancelled or failed, code: ${result.resultCode}")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Configure Google Sign-In with email and profile
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val emailEt = findViewById<EditText>(R.id.email)
        val passwordEt = findViewById<EditText>(R.id.password)
        val loginBtn = findViewById<Button>(R.id.btn_login)
        val googleSignInBtn = findViewById<Button>(R.id.btn_google_signin)
        val signUpLinkText = findViewById<TextView>(R.id.signup_link_text)
        loginProgress = findViewById(R.id.loginProgress)

        signUpLinkText.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }

        googleSignInBtn.setOnClickListener {
            loginProgress.visibility = View.VISIBLE
            // Sign out first to ensure account chooser shows
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }

        loginBtn.setOnClickListener {
            val input = emailEt.text.toString().trim()
            val password = passwordEt.text.toString().trim()

            if (input.isEmpty()) {
                emailEt.error = "Enter Roll No (e.g. i230553) or Email"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordEt.error = "Enter password"
                return@setOnClickListener
            }

            // Resolve roll number into email if entered without domain
            val email = resolveRollNoToEmail(input)

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEt.error = "Enter valid roll number or email"
                return@setOnClickListener
            }

            loginProgress.visibility = View.VISIBLE

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    loginProgress.visibility = View.GONE
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            saveUserProfileAndProceed(
                                uid = user.uid,
                                name = user.displayName ?: email.substringBefore("@"),
                                email = email,
                                photoUrl = user.photoUrl?.toString() ?: ""
                            )
                        } else {
                            navigateToHome()
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

    /**
     * Parse roll number and resolve to proper FAST university domain
     * e.g. i230553 -> i230553@isb.nu.edu.pk (batch >= 23)
     * e.g. i220553 -> i220553@nu.edu.pk     (batch <= 22)
     */
    private fun resolveRollNoToEmail(input: String): String {
        if (input.contains("@")) return input

        val cleaned = input.lowercase().trim()
        val match = Regex("^[a-z](\\d{2})\\d{4}$").find(cleaned)
        return if (match != null) {
            val batchYear = match.groupValues[1].toIntOrNull() ?: 23
            if (batchYear >= 23) {
                "$cleaned@isb.nu.edu.pk"
            } else {
                "$cleaned@nu.edu.pk"
            }
        } else {
            // Default to Islamabad campus domain
            "$cleaned@isb.nu.edu.pk"
        }
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val email = account.email ?: ""

            // Domain check: Only allow FAST NUCES university accounts
            if (!email.endsWith("@isb.nu.edu.pk", ignoreCase = true) &&
                !email.endsWith("@nu.edu.pk", ignoreCase = true)
            ) {
                loginProgress.visibility = View.GONE
                googleSignInClient.signOut()
                AlertDialog.Builder(this)
                    .setTitle("University Account Required")
                    .setMessage("Please sign in using your official FAST NUCES university email:\n\n• @isb.nu.edu.pk (Batch 23+)\n• @nu.edu.pk (Batch 22 & below)")
                    .setPositiveButton("OK", null)
                    .show()
                return
            }

            val name = account.displayName ?: email.substringBefore("@")
            val photoUrl = account.photoUrl?.toString() ?: ""
            val uid = account.id ?: email.replace(".", "_")

            saveUserProfileAndProceed(uid, name, email, photoUrl)

        } catch (e: ApiException) {
            loginProgress.visibility = View.GONE
            Log.e(TAG, "Google sign-in failed with code: ${e.statusCode}, message: ${e.message}")
            Toast.makeText(this, "Google Sign-In failed (${e.statusCode}). You can also sign in with your Roll No.", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveUserProfileAndProceed(uid: String, name: String, email: String, photoUrl: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Save locally in Room database
                val profileDao = AppDatabase.getDatabase(this@Login).userProfileDao()
                val existing = profileDao.getAnyUserProfile()
                val profile = UserProfile(
                    uid = uid,
                    name = name,
                    email = email,
                    batch = existing?.batch ?: "",
                    degree = existing?.degree ?: "",
                    section = existing?.section ?: "",
                    profileImageUrl = if (photoUrl.isNotEmpty()) photoUrl else (existing?.profileImageUrl ?: "")
                )
                profileDao.insertUserProfile(profile)

                // Also backup to Firebase Realtime Database
                try {
                    val db = FirebaseDatabase.getInstance().reference
                    val userData = mapOf(
                        "uid" to uid,
                        "name" to name,
                        "email" to email,
                        "profileImageUrl" to photoUrl
                    )
                    db.child("users").child(uid).updateChildren(userData)
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase backup skipped: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving profile: ${e.message}")
            }

            withContext(Dispatchers.Main) {
                loginProgress.visibility = View.GONE
                Toast.makeText(this@Login, "Welcome, $name!", Toast.LENGTH_SHORT).show()
                navigateToHome()
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, Home::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
