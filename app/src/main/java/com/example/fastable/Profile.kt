package com.example.fastable

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView
import android.widget.TextView
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import java.io.File
import java.io.FileOutputStream
import com.example.fastable.data.local.AppDatabase
import com.example.fastable.data.models.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Profile : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var profileImage: CircleImageView
    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        database = FirebaseDatabase.getInstance().reference
        val topAppBar = findViewById<Toolbar>(R.id.topAppBar)
        profileImage = findViewById(R.id.profileImage)

        topAppBar.setNavigationOnClickListener {
            finish()
        }

        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    showEditDialog()
                    true
                }
                else -> false
            }
        }

        loadUserProfile()

        profileImage.setOnClickListener {
            openImagePicker()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload profile data when returning to this screen
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            android.util.Log.d("Profile", "Loading profile for user: ${currentUser.uid}")

            // Load from offline database first (instant)
            CoroutineScope(Dispatchers.IO).launch {
                val offlineProfile = AppDatabase.getDatabase(this@Profile).userProfileDao()
                    .getUserProfile(currentUser.uid)

                withContext(Dispatchers.Main) {
                    if (offlineProfile != null) {
                        // Display offline data immediately
                        displayUserProfile(offlineProfile)
                        android.util.Log.d("Profile", "Loaded from offline DB")
                    } else {
                        // If no offline data, fetch from Firebase
                        syncProfileFromFirebase(currentUser.uid)
                    }
                }
            }
        } else {
            android.util.Log.w("Profile", "No current user")
        }
    }

    private fun displayUserProfile(profile: UserProfile) {
        findViewById<TextView>(R.id.value_name).text = profile.name
        findViewById<TextView>(R.id.value_email).text = profile.email
        findViewById<TextView>(R.id.value_batch).text = profile.batch
        findViewById<TextView>(R.id.value_degree).text = profile.degree
        findViewById<TextView>(R.id.value_section).text = profile.section

        if (profile.profileImageUrl.isNotEmpty()) {
            val localFile = File(profile.profileImageUrl)
            if (localFile.exists()) {
                Glide.with(this@Profile).load(localFile)
                    .placeholder(R.drawable.img_demo).into(profileImage)
            } else {
                profileImage.setImageResource(R.drawable.img_demo)
            }
        } else {
            profileImage.setImageResource(R.drawable.img_demo)
        }
    }

    private fun syncProfileFromFirebase(uid: String) {
        database.child("users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    android.util.Log.d("Profile", "Data fetched from Firebase")
                    if (snapshot.exists()) {
                        val name = snapshot.child("name").getValue(String::class.java) ?: ""
                        val email = snapshot.child("email").getValue(String::class.java) ?: ""
                        val batch = snapshot.child("batch").getValue(String::class.java) ?: ""
                        val degree = snapshot.child("degree").getValue(String::class.java) ?: ""
                        val section = snapshot.child("section").getValue(String::class.java) ?: ""
                        val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java) ?: ""

                        val profile = UserProfile(
                            uid = uid,
                            name = name,
                            email = email,
                            batch = batch,
                            degree = degree,
                            section = section,
                            profileImageUrl = profileImageUrl
                        )

                        // Save to offline database
                        CoroutineScope(Dispatchers.IO).launch {
                            AppDatabase.getDatabase(this@Profile).userProfileDao()
                                .insertUserProfile(profile)

                            withContext(Dispatchers.Main) {
                                displayUserProfile(profile)
                                android.util.Log.d("Profile", "Synced from Firebase")
                            }
                        }
                    } else {
                        android.util.Log.w("Profile", "User data doesn't exist in Firebase")
                        Toast.makeText(this@Profile, "User profile not found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e("Profile", "Error loading from Firebase: ${error.message}")
                    Toast.makeText(this@Profile, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showEditDialog() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        android.util.Log.d("Profile", "Opening edit dialog")

        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val etBatch = dialogView.findViewById<EditText>(R.id.etEditBatch)
        val etDegree = dialogView.findViewById<EditText>(R.id.etEditDegree)
        val etSection = dialogView.findViewById<EditText>(R.id.etEditSection)

        // Load current values from offline DB first
        CoroutineScope(Dispatchers.IO).launch {
            val profile = AppDatabase.getDatabase(this@Profile).userProfileDao()
                .getUserProfile(currentUser.uid)

            withContext(Dispatchers.Main) {
                if (profile != null) {
                    etBatch.setText(profile.batch)
                    etDegree.setText(profile.degree)
                    etSection.setText(profile.section)
                    android.util.Log.d("Profile", "Loaded values from offline DB")
                }
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, which ->
                val batch = etBatch.text.toString().trim()
                val degree = etDegree.text.toString().trim()
                val section = etSection.text.toString().trim()

                if (batch.isEmpty() || degree.isEmpty() || section.isEmpty()) {
                    Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                } else {
                    updateUserProfile(batch, degree, section)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateUserProfile(batch: String, degree: String, section: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        android.util.Log.d("Profile", "Updating profile: batch=$batch, degree=$degree, section=$section")

        // First update offline database (instant)
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.getDatabase(this@Profile).userProfileDao()
                .updateProfileFields(currentUser.uid, batch, degree, section)

            withContext(Dispatchers.Main) {
                android.util.Log.d("Profile", "Updated offline DB")
                Toast.makeText(this@Profile, "Profile updated", Toast.LENGTH_SHORT).show()
                loadUserProfile()
            }
        }

        // Then sync to Firebase in background
        val updates = mapOf(
            "batch" to batch,
            "degree" to degree,
            "section" to section
        )

        database.child("users").child(currentUser.uid).updateChildren(updates)
            .addOnSuccessListener {
                android.util.Log.d("Profile", "Synced to Firebase")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("Profile", "Firebase sync failed: ${e.message}")
            }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            if (selectedImageUri != null) {
                saveProfileImageLocally(selectedImageUri!!)
            }
        }
    }

    private fun saveProfileImageLocally(imageUri: Uri) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        try {
            Toast.makeText(this, "Saving image...", Toast.LENGTH_SHORT).show()

            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)

            val directory = File(filesDir, "profile_images")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val filename = "${currentUser.uid}.jpg"
            val file = File(directory, filename)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            val imagePath = file.absolutePath

            // Update offline database first (instant)
            CoroutineScope(Dispatchers.IO).launch {
                AppDatabase.getDatabase(this@Profile).userProfileDao()
                    .updateProfileImage(currentUser.uid, imagePath)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Profile, "Profile image updated", Toast.LENGTH_SHORT).show()
                    Glide.with(this@Profile).load(file).placeholder(R.drawable.img_demo).into(profileImage)
                }
            }

            // Then sync to Firebase in background
            database.child("users").child(currentUser.uid).child("profileImageUrl").setValue(imagePath)
                .addOnSuccessListener {
                    android.util.Log.d("Profile", "Image synced to Firebase")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("Profile", "Image sync failed: ${e.message}")
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}