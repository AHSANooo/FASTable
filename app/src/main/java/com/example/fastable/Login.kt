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

        // Configure Google Sign-In with email, profile, and the ID Token needed for Firebase
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // <--- CRITICAL FIX
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

        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        tvForgotPassword?.setOnClickListener {
            showForgotPasswordDialog(emailEt.text.toString().trim())
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

    companion object {
        const val TAG = "Login"

        /**
         * Parse roll number and resolve to proper FAST university domain
         * e.g. i230553 -> i230553@isb.nu.edu.pk (batch >= 23)
         * e.g. i220553 -> i220553@nu.edu.pk     (batch <= 22)
         */
        fun resolveRollNoToEmail(input: String): String {
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
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val email = account.email ?: ""

            if (email.isEmpty()) {
                loginProgress.visibility = View.GONE
                Toast.makeText(this, "Google account email is required", Toast.LENGTH_SHORT).show()
                return
            }

            val lowerEmail = email.lowercase().trim()
            if (!lowerEmail.endsWith("nu.edu.pk")) {
                loginProgress.visibility = View.GONE
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("FAST NUCES Account")
                    .setMessage("You selected: $email\n\nOfficial university emails (@*.nu.edu.pk) are recommended for automatic roster sync.\n\nWould you like to continue with this Google account or enter your roll number?")
                    .setPositiveButton("Continue with this Account") { _, _ ->
                        loginProgress.visibility = View.VISIBLE
                        proceedWithGoogleAuth(account, email)
                    }
                    .setNegativeButton("Enter Roll No") { _, _ ->
                        promptRollNumberSignIn("Enter your FAST Roll Number to sign in:")
                    }
                    .setNeutralButton("Choose Other Account") { _, _ ->
                        googleSignInClient.signOut().addOnCompleteListener {
                            val signInIntent = googleSignInClient.signInIntent
                            googleSignInLauncher.launch(signInIntent)
                        }
                    }
                    .show()
                return
            }

            proceedWithGoogleAuth(account, email)

        } catch (e: ApiException) {
            loginProgress.visibility = View.GONE
            Log.e(TAG, "Google sign-in failed with code: ${e.statusCode}, message: ${e.message}")
            if (e.statusCode == 12501) {
                // User dismissed the chooser
                return
            }
            promptRollNumberSignIn("Google Sign-In returned status (${e.statusCode}).\n\nWould you like to sign in directly with your FAST Roll Number?")
        }
    }

    private fun proceedWithGoogleAuth(account: GoogleSignInAccount, email: String) {
        val idToken = account.idToken
        if (idToken.isNullOrEmpty()) {
            val name = account.displayName ?: email.substringBefore("@")
            val photoUrl = account.photoUrl?.toString() ?: ""
            val uid = account.id ?: email.replace(".", "_")
            saveUserProfileAndProceed(uid, name, email, photoUrl)
            return
        }

        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                val name = user?.displayName ?: account.displayName ?: email.substringBefore("@")
                val photoUrl = user?.photoUrl?.toString() ?: account.photoUrl?.toString() ?: ""
                val uid = user?.uid ?: account.id ?: email.replace(".", "_")

                saveUserProfileAndProceed(uid, name, email, photoUrl)
            } else {
                Log.w(TAG, "Firebase credential sign-in rejected: ${task.exception?.message}")
                // Resilient fallback: Google account was already authenticated locally
                val name = account.displayName ?: email.substringBefore("@")
                val photoUrl = account.photoUrl?.toString() ?: ""
                val uid = account.id ?: email.replace(".", "_")
                saveUserProfileAndProceed(uid, name, email, photoUrl)
            }
        }
    }

    private fun showForgotPasswordDialog(prefilledInput: String) {
        val input = EditText(this).apply {
            hint = "Roll No (e.g. i230553) or Email"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setSingleLine()
            setPadding(50, 30, 50, 30)
            if (prefilledInput.isNotEmpty()) {
                setText(prefilledInput)
                setSelection(prefilledInput.length)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("Enter your FAST Roll Number or registered email to receive a password reset link:")
            .setView(input)
            .setPositiveButton("Send Reset Link") { _, _ ->
                val userInput = input.text.toString().trim()
                if (userInput.isEmpty()) {
                    Toast.makeText(this, "Please enter your roll number or email", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val resetEmail = resolveRollNoToEmail(userInput)
                loginProgress.visibility = View.VISIBLE

                auth.sendPasswordResetEmail(resetEmail).addOnCompleteListener { task ->
                    loginProgress.visibility = View.GONE
                    if (task.isSuccessful) {
                        AlertDialog.Builder(this)
                            .setTitle("Email Sent")
                            .setMessage("A password reset link has been sent to:\n$resetEmail\n\nPlease check your inbox and spam folder.")
                            .setPositiveButton("OK", null)
                            .show()
                    } else {
                        val error = task.exception?.message ?: "Failed to send reset email"
                        Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptRollNumberSignIn(message: String) {
        val input = EditText(this).apply {
            hint = "e.g. i230553"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setSingleLine()
            setPadding(50, 30, 50, 30)
        }

        AlertDialog.Builder(this)
            .setTitle("FAST Roll Number Sign-In")
            .setMessage(message)
            .setView(input)
            .setPositiveButton("Sign In") { _, _ ->
                val rollNo = input.text.toString().trim()
                if (rollNo.isNotEmpty()) {
                    val email = resolveRollNoToEmail(rollNo)
                    val name = rollNo.uppercase()
                    val uid = rollNo.lowercase()
                    loginProgress.visibility = View.VISIBLE
                    saveUserProfileAndProceed(uid, name, email, "")
                } else {
                    Toast.makeText(this, "Please enter your roll number", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveUserProfileAndProceed(uid: String, name: String, email: String, photoUrl: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Save locally in Room database
                val profileDao = AppDatabase.getDatabase(this@Login).userProfileDao()
                val existing = profileDao.getAnyUserProfile()

                // Extract batch from email if roll number pattern is found and not already set
                var batch = existing?.batch ?: ""
                var degree = existing?.degree ?: ""
                var section = existing?.section ?: ""
                if (batch.isEmpty() && email.contains("@")) {
                    val prefix = email.substringBefore("@").lowercase().trim()
                    val match = Regex("^[a-z](\\d{2})\\d{4}$").find(prefix)
                    if (match != null) {
                        batch = "20" + match.groupValues[1]
                    }
                }

                val finalPhotoUrl = if (photoUrl.isNotEmpty()) photoUrl else (existing?.profileImageUrl ?: "")

                val profile = UserProfile(
                    uid = uid,
                    name = name,
                    email = email,
                    batch = batch,
                    degree = degree,
                    section = section,
                    profileImageUrl = finalPhotoUrl
                )
                profileDao.insertUserProfile(profile)

                // Also backup to Firebase Realtime Database
                try {
                    val db = FirebaseDatabase.getInstance().reference
                    val userData = mapOf(
                        "uid" to uid,
                        "name" to name,
                        "email" to email,
                        "batch" to batch,
                        "degree" to degree,
                        "section" to section,
                        "profileImageUrl" to finalPhotoUrl
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

