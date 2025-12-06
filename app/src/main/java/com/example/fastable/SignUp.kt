package com.example.fastable

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.example.fastable.data.local.AppDatabase
import com.example.fastable.data.models.UserProfile
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
    private val PICK_IMAGE_REQUEST = 1
    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        profileImageView = findViewById(R.id.profile_image)
        val nameEt = findViewById<EditText>(R.id.name)
        val emailEt = findViewById<EditText>(R.id.email)
        val batchEt = findViewById<EditText>(R.id.batch)
        val degreeEt = findViewById<EditText>(R.id.degree)
        val sectionEt = findViewById<EditText>(R.id.section)
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
            val batch = batchEt.text.toString().trim()
            val degree = degreeEt.text.toString().trim()
            val section = sectionEt.text.toString().trim()
            val password = passwordEt.text.toString().trim()
            val confirmPassword = confirmPasswordEt.text.toString().trim()

            Log.d("SignUp", "Name: '$name' (isEmpty: ${name.isEmpty()})")
            Log.d("SignUp", "Email: '$email' (isEmpty: ${email.isEmpty()})")
            Log.d("SignUp", "Batch: '$batch' (isEmpty: ${batch.isEmpty()})")
            Log.d("SignUp", "Degree: '$degree' (isEmpty: ${degree.isEmpty()})")
            Log.d("SignUp", "Section: '$section' (isEmpty: ${section.isEmpty()})")

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
            if (batch.isEmpty()) {
                Toast.makeText(this, "Please enter your batch", Toast.LENGTH_SHORT).show()
                batchEt.requestFocus()
                return@setOnClickListener
            }
            if (degree.isEmpty()) {
                Toast.makeText(this, "Please enter your degree", Toast.LENGTH_SHORT).show()
                degreeEt.requestFocus()
                return@setOnClickListener
            }
            if (section.isEmpty()) {
                Toast.makeText(this, "Please enter your section", Toast.LENGTH_SHORT).show()
                sectionEt.requestFocus()
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
                            // Save profile image if selected
                            val profileImagePath = saveProfileImageLocally(user.uid)

                            val userData = mapOf(
                                "uid" to user.uid,
                                "name" to name,
                                "email" to email,
                                "batch" to batch,
                                "degree" to degree,
                                "section" to section,
                                "profileImageUrl" to profileImagePath
                            )

                            // Save to Firebase
                            database.child("users").child(user.uid).setValue(userData)
                                .addOnSuccessListener {
                                    Log.d("SignUp", "User data saved to Realtime Database")

                                    // Also save to offline database
                                    val userProfile = UserProfile(
                                        uid = user.uid,
                                        name = name,
                                        email = email,
                                        batch = batch,
                                        degree = degree,
                                        section = section,
                                        profileImageUrl = profileImagePath
                                    )

                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            AppDatabase.getDatabase(this@SignUp).userProfileDao()
                                                .insertUserProfile(userProfile)
                                            Log.d("SignUp", "User data saved to offline database")
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
        if (checkPermission()) {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        } else {
            requestPermission()
        }
    }

    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES),
                PERMISSION_REQUEST_CODE
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker()
            } else {
                Toast.makeText(this, "Permission denied. Cannot access gallery.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            if (selectedImageUri != null) {
                profileImageView.setImageURI(selectedImageUri)
            }
        }
    }

    private fun saveProfileImageLocally(uid: String): String {
        if (selectedImageUri == null) return ""

        return try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImageUri)

            val directory = File(filesDir, "profile_images")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val filename = "${uid}.jpg"
            val file = File(directory, filename)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            file.absolutePath
        } catch (e: Exception) {
            Log.e("SignUp", "Error saving profile image: ${e.message}")
            ""
        }
    }
}
