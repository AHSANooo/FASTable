package com.example.fastable

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
import com.example.fastable.api.ProfileApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Base64
import android.graphics.BitmapFactory

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
        CoroutineScope(Dispatchers.IO).launch {
            val profileDao = AppDatabase.getDatabase(this@Profile).userProfileDao()
            var offlineProfile = if (currentUser != null) {
                profileDao.getUserProfile(currentUser.uid) ?: profileDao.getAnyUserProfile()
            } else {
                profileDao.getAnyUserProfile()
            }

            // Check if Google Sign-In has a photo URL
            val lastGoogleAccount = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(this@Profile)
            val googlePhoto = lastGoogleAccount?.photoUrl?.toString() ?: ""
            if (offlineProfile != null && googlePhoto.isNotEmpty() && (offlineProfile.profileImageUrl.isEmpty() || offlineProfile.profileImageUrl != googlePhoto)) {
                profileDao.updateProfileImage(offlineProfile.uid, googlePhoto)
                offlineProfile = offlineProfile.copy(profileImageUrl = googlePhoto)
            }

            val uid = currentUser?.uid ?: offlineProfile?.uid

            withContext(Dispatchers.Main) {
                if (offlineProfile != null) {
                    displayUserProfile(offlineProfile)
                    android.util.Log.d("Profile", "Loaded from offline DB: ${offlineProfile.uid}")
                }
                if (uid != null) {
                    syncProfileFromFirebase(uid)
                } else if (offlineProfile == null) {
                    android.util.Log.w("Profile", "No profile found in Room or Firebase")
                }
            }
        }
    }

    private fun displayUserProfile(profile: UserProfile) {
        findViewById<TextView>(R.id.value_name).text = profile.name
        findViewById<TextView>(R.id.value_email).text = profile.email
        findViewById<TextView>(R.id.value_batch).text = profile.batch
        findViewById<TextView>(R.id.value_degree).text = profile.degree
        findViewById<TextView>(R.id.value_section).text = profile.section

        if (profile.profileImageUrl.isNotEmpty()) {
            if (profile.profileImageUrl.startsWith("http", ignoreCase = true)) {
                com.squareup.picasso.Picasso.get()
                    .load(profile.profileImageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(profileImage)
            } else {
                val localFile = File(profile.profileImageUrl)
                if (localFile.exists()) {
                    com.squareup.picasso.Picasso.get()
                        .load(localFile)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .into(profileImage)
                } else {
                    profileImage.setImageResource(R.drawable.ic_profile_placeholder)
                }
            }
        } else {
            profileImage.setImageResource(R.drawable.ic_profile_placeholder)
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

                        // Save to offline database
                        CoroutineScope(Dispatchers.IO).launch {
                            val currentOffline = AppDatabase.getDatabase(this@Profile).userProfileDao().getUserProfile(uid)
                            // If it's a web URL (e.g. Google photo URL), preserve it; only decode Base64 if not HTTP
                            val localImagePath = if (profileImageUrl.isNotEmpty() && !profileImageUrl.startsWith("http", ignoreCase = true)) {
                                saveBase64ImageLocally(uid, profileImageUrl)
                            } else if (profileImageUrl.isNotEmpty()) {
                                profileImageUrl
                            } else {
                                currentOffline?.profileImageUrl ?: ""
                            }

                            val profile = UserProfile(
                                uid = uid,
                                name = name,
                                email = email,
                                batch = batch,
                                degree = degree,
                                section = section,
                                profileImageUrl = localImagePath
                            )

                            AppDatabase.getDatabase(this@Profile).userProfileDao()
                                .insertUserProfile(profile)

                            withContext(Dispatchers.Main) {
                                displayUserProfile(profile)
                                android.util.Log.d("Profile", "Synced from Firebase")
                            }
                        }
                    } else {
                        android.util.Log.w("Profile", "User data doesn't exist in Firebase")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e("Profile", "Error loading from Firebase: ${error.message}")
                }
            })
    }

    private fun showEditDialog() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        CoroutineScope(Dispatchers.IO).launch {
            val offlineProfile = AppDatabase.getDatabase(this@Profile).userProfileDao().getAnyUserProfile()
            val uid = currentUser?.uid ?: offlineProfile?.uid ?: return@launch

            withContext(Dispatchers.Main) {
                android.util.Log.d("Profile", "Opening edit dialog for uid: $uid")

                val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
                val etBatch = dialogView.findViewById<EditText>(R.id.etEditBatch)
                val etDegree = dialogView.findViewById<EditText>(R.id.etEditDegree)
                val etSection = dialogView.findViewById<EditText>(R.id.etEditSection)

                if (offlineProfile != null) {
                    etBatch.setText(offlineProfile.batch)
                    etDegree.setText(offlineProfile.degree)
                    etSection.setText(offlineProfile.section)
                    android.util.Log.d("Profile", "Loaded values from offline DB")
                }

                AlertDialog.Builder(this@Profile)
                    .setTitle("Edit Profile")
                    .setView(dialogView)
                    .setPositiveButton("Save") { dialog, which ->
                        val batch = etBatch.text.toString().trim()
                        val degree = etDegree.text.toString().trim()
                        val section = etSection.text.toString().trim()

                        if (batch.isEmpty() || degree.isEmpty() || section.isEmpty()) {
                            Toast.makeText(this@Profile, "All fields are required", Toast.LENGTH_SHORT).show()
                        } else {
                            updateUserProfile(uid, batch, degree, section)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun updateUserProfile(uid: String, batch: String, degree: String, section: String) {
        android.util.Log.d("Profile", "Updating profile: uid=$uid, batch=$batch, degree=$degree, section=$section")

        // First update offline database (instant)
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.getDatabase(this@Profile).userProfileDao()
                .updateProfileFields(uid, batch, degree, section)

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

        database.child("users").child(uid).updateChildren(updates)
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
        val currentUser = FirebaseAuth.getInstance().currentUser
        CoroutineScope(Dispatchers.IO).launch {
            val offlineProfile = AppDatabase.getDatabase(this@Profile).userProfileDao().getAnyUserProfile()
            val uid = currentUser?.uid ?: offlineProfile?.uid ?: return@launch

            withContext(Dispatchers.Main) {
                try {
                    Toast.makeText(this@Profile, "Saving image...", Toast.LENGTH_SHORT).show()

                    // Use contentResolver.openInputStream instead of getBitmap
                    // to avoid SecurityException with scoped storage
                    val inputStream = contentResolver.openInputStream(imageUri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (bitmap == null) {
                        Toast.makeText(this@Profile, "Failed to load image", Toast.LENGTH_SHORT).show()
                        return@withContext
                    }

                    // Save to local storage
                    val directory = File(filesDir, "profile_images")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }

                    val filename = "$uid.jpg"
                    val file = File(directory, filename)

                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out)
                    }

                    val imagePath = file.absolutePath

                    // Convert to Base64 for Firebase storage (like a23i project)
                    val profileImageBase64 = com.example.fastable.utils.ImageUtils.bitmapToBase64(bitmap, 60)

                    // Update offline database first (instant)
                    CoroutineScope(Dispatchers.IO).launch {
                        AppDatabase.getDatabase(this@Profile).userProfileDao()
                            .updateProfileImage(uid, imagePath)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@Profile, "Profile image updated", Toast.LENGTH_SHORT).show()
                            com.squareup.picasso.Picasso.get()
                                .load(file)
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .error(R.drawable.ic_profile_placeholder)
                                .into(profileImage)
                        }

                        // Upload to MySQL server in background (optional backup)
                        try {
                            val uploadResponse = ProfileApiService.uploadProfilePicture(
                                userId = uid,
                                bitmap = bitmap
                            )

                            if (uploadResponse.success) {
                                android.util.Log.d("Profile", "Profile picture uploaded to MySQL: ${uploadResponse.message}")
                                android.util.Log.d("Profile", "Image URL: ${uploadResponse.data}")
                            } else {
                                android.util.Log.e("Profile", "MySQL upload failed: ${uploadResponse.message}")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("Profile", "Error uploading to MySQL: ${e.message}")
                        }
                    }

                    // Sync Base64 to Firebase (primary storage)
                    database.child("users").child(uid).child("profileImageUrl").setValue(profileImageBase64)
                        .addOnSuccessListener {
                            android.util.Log.d("Profile", "Image Base64 synced to Firebase")
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("Profile", "Image sync to Firebase failed: ${e.message}")
                        }
                } catch (e: SecurityException) {
                    Toast.makeText(this@Profile, "Permission denied to access this image", Toast.LENGTH_SHORT).show()
                    android.util.Log.e("Profile", "SecurityException: ${e.message}")
                } catch (e: Exception) {
                    Toast.makeText(this@Profile, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    android.util.Log.e("Profile", "Error saving image: ${e.message}")
                }
            }
        }
    }

    /**
     * Convert Base64 string to bitmap and save locally
     * Returns the local file path
     */
    private fun saveBase64ImageLocally(uid: String, base64String: String): String {
        return try {
            // Decode Base64 to bitmap
            val bytes = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            if (bitmap == null) {
                android.util.Log.e("Profile", "Failed to decode Base64 image")
                return ""
            }

            // Create directory for profile images
            val directory = File(filesDir, "profile_images")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            // Save to file
            val filename = "$uid.jpg"
            val file = File(directory, filename)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            // Verify file was saved
            if (file.exists() && file.length() > 0) {
                android.util.Log.d("Profile", "Profile image saved locally: ${file.absolutePath}")
                file.absolutePath
            } else {
                android.util.Log.e("Profile", "Failed to save profile image")
                ""
            }
        } catch (e: Exception) {
            android.util.Log.e("Profile", "Error saving Base64 image: ${e.message}", e)
            ""
        }
    }
}