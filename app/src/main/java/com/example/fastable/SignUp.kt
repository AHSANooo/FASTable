package com.example.fastable

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.example.fastable.data.local.AppDatabase
import com.example.fastable.data.models.UserProfile
import com.example.fastable.api.ProfileApiService
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class SignUp : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var profileImageView: CircleImageView
    private var selectedImageUri: Uri? = null
    private var selectedBitmap: Bitmap? = null
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        profileImageView = findViewById(R.id.profile_image)
        val nameEt = findViewById<EditText>(R.id.name)
        val emailEt = findViewById<EditText>(R.id.email)
        val passwordEt = findViewById<EditText>(R.id.password)
        val confirmPasswordEt = findViewById<EditText>(R.id.confirm_password)
        val signUpBtn = findViewById<Button>(R.id.btn_signup)

        // Set click listener for profile image
        profileImageView.setOnClickListener {
            openImagePicker()
        }

        signUpBtn.setOnClickListener {
            val name = nameEt.text.toString().trim()
            val email = emailEt.text.toString().trim()
            val password = passwordEt.text.toString().trim()
            val confirmPassword = confirmPasswordEt.text.toString().trim()

            Log.d("SignUp", "Name: '$name' (isEmpty: ${name.isEmpty()})")
            Log.d("SignUp", "Email: '$email' (isEmpty: ${email.isEmpty()})")

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show()
                nameEt.requestFocus()
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                emailEt.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
                passwordEt.requestFocus()
                return@setOnClickListener
            }
            if (confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please confirm your password", Toast.LENGTH_SHORT).show()
                confirmPasswordEt.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEt.error = "Enter valid email"
                emailEt.requestFocus()
                return@setOnClickListener
            }

            // Validate FAST NUCES email domain
            if (!email.endsWith("@isb.nu.edu.pk") && !email.endsWith("@nu.edu.pk")) {
                Toast.makeText(
                    this,
                    "Please use your FAST NUCES email (@isb.nu.edu.pk or @nu.edu.pk)",
                    Toast.LENGTH_LONG
                ).show()
                emailEt.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                passwordEt.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                confirmPasswordEt.error = "Passwords do not match"
                return@setOnClickListener
            }

            // Show loading toast
            Toast.makeText(this, "Creating account...", Toast.LENGTH_SHORT).show()

            // ✅ Create user in Firebase
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("SignUp", "User account created successfully")

                        // Save user data to Realtime Database and offline DB
                        val user = auth.currentUser
                        if (user != null) {
                            // Process everything in background
                            CoroutineScope(Dispatchers.IO).launch {
                                // Convert profile image to Base64 if selected (like a23i project)
                                val profileImageBase64 = if (selectedBitmap != null) {
                                    com.example.fastable.utils.ImageUtils.bitmapToBase64(selectedBitmap!!, 60)
                                } else {
                                    ""
                                }

                                // Save profile image locally if selected
                                val profileImagePath = if (selectedBitmap != null) {
                                    saveProfileImageLocally(user.uid)
                                } else {
                                    ""
                                }

                                val userData = mapOf(
                                    "uid" to user.uid,
                                    "name" to name,
                                    "email" to email,
                                    "batch" to "",
                                    "degree" to "",
                                    "section" to "",
                                    "profileImageUrl" to profileImageBase64  // Store Base64 in Firebase
                                )

                                // Save to Firebase
                                database.child("users").child(user.uid).setValue(userData)
                                    .addOnSuccessListener {
                                        Log.d("SignUp", "User data saved to Realtime Database")

                                        // Also save to offline database (with local file path)
                                        val userProfile = UserProfile(
                                            uid = user.uid,
                                            name = name,
                                            email = email,
                                            batch = "",
                                            degree = "",
                                            section = "",
                                            profileImageUrl = profileImagePath  // Store local path in offline DB
                                        )

                                        CoroutineScope(Dispatchers.IO).launch {
                                            try {
                                                // Save to offline database
                                                AppDatabase.getDatabase(this@SignUp).userProfileDao()
                                                    .insertUserProfile(userProfile)
                                                Log.d("SignUp", "User data saved to offline database")

                                                // Upload profile picture to MySQL in background (optional backup)
                                                if (selectedBitmap != null) {
                                                    try {
                                                        val uploadResponse = ProfileApiService.uploadProfilePicture(
                                                            userId = user.uid,
                                                            bitmap = selectedBitmap!!
                                                        )

                                                        if (uploadResponse.success) {
                                                            Log.d("SignUp", "Profile picture uploaded to MySQL: ${uploadResponse.message}")
                                                            Log.d("SignUp", "Image URL: ${uploadResponse.data}")
                                                        } else {
                                                            Log.e("SignUp", "Profile picture upload to MySQL failed: ${uploadResponse.message}")
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e("SignUp", "Error uploading profile picture to MySQL: ${e.message}", e)
                                                    }
                                                } else {
                                                    Log.d("SignUp", "No profile picture selected. Skipping MySQL upload.")
                                                }
                                            } catch (e: Exception) {
                                                Log.e("SignUp", "Failed to save offline: ${e.message}")
                                            }
                                        }

                                        sendVerificationEmail(email)
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("SignUp", "Failed to save user data: ${e.message}")
                                    }
                            }
                        }

                        Toast.makeText(
                            this,
                            "Account created! Please verify your email before logging in.",
                            Toast.LENGTH_LONG
                        ).show()

                        // Redirect to login screen after short delay
                        Handler(Looper.getMainLooper()).postDelayed({
                            startActivity(Intent(this, Login::class.java))
                            finish()
                        }, 1500)

                    } else {
                        val errorMessage = task.exception?.message ?: "Unknown error"
                        Log.e("SignUp", "Failed to create account: $errorMessage")
                        Toast.makeText(
                            this,
                            "Sign up failed: $errorMessage",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    private fun sendVerificationEmail(email: String) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Unable to send verification email: no user session.", Toast.LENGTH_SHORT).show()
            return
        }

        // Send simple verification email (no ActionCodeSettings needed)
        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("SignUp", "Verification email sent successfully to $email")
                    Toast.makeText(
                        this,
                        "Account created! Verification email sent to $email\nPlease check your inbox and spam folder.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val errorMessage = task.exception?.message ?: "Unknown error"
                    Log.e("SignUp", "Failed to send verification email: $errorMessage")
                    Toast.makeText(
                        this,
                        "Failed to send verification email: $errorMessage",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun openImagePicker() {
        // Use ACTION_GET_CONTENT with read permission flag
        // This works better with Google Photos and other content providers
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            if (selectedImageUri != null) {
                // Display selected image
                profileImageView.setImageURI(selectedImageUri)

                // Convert the selected image to bitmap and store it
                // Use contentResolver to avoid permission issues
                try {
                    val inputStream = contentResolver.openInputStream(selectedImageUri!!)
                    selectedBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                } catch (e: Exception) {
                    Log.e("SignUp", "Error loading image: ${e.message}", e)
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveProfileImageLocally(uid: String): String {
        if (selectedBitmap == null) return ""

        return try {
            val directory = File(filesDir, "profile_images")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val filename = "${uid}.jpg"
            val file = File(directory, filename)

            FileOutputStream(file).use { out ->
                // Use 60% quality to match a23i project
                selectedBitmap!!.compress(Bitmap.CompressFormat.JPEG, 60, out)
            }

            file.absolutePath
        } catch (e: Exception) {
            Log.e("SignUp", "Error saving profile image: ${e.message}", e)
            ""
        }
    }
}
