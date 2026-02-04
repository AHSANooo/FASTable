package com.example.fastable

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.example.fastable.adapters.DashboardSessionAdapter
import com.example.fastable.viewmodel.HomeViewModel
import de.hdodenhof.circleimageview.CircleImageView
import com.example.fastable.data.local.AppDatabase
import com.example.fastable.data.models.UserProfile
import com.example.fastable.utils.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import android.util.Base64
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.FrameLayout
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.activity.result.ActivityResultLauncher
import kotlin.math.abs

class Home : AppCompatActivity() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: DashboardSessionAdapter
    private lateinit var allTimetableAdapter: DashboardSessionAdapter
    private lateinit var database: DatabaseReference
    private lateinit var loadingOverlay: FrameLayout
    private val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
    private var currentSelectedDay = "Monday"

    // Permission launcher
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize notification permission launcher
        notificationPermissionLauncher = PermissionManager.createNotificationPermissionLauncher(
            activity = this,
            onGranted = {
                // Permission granted - notifications will work
            },
            onDenied = {
                // Show a gentle reminder that notifications are useful
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Enable notifications in settings to get class reminders",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        )

        // Initialize Realtime Database
        database = FirebaseDatabase.getInstance().reference

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        // Initialize RecyclerViews
        setupRecyclerView()
        setupAllTimetableView()

        // Setup tabs
        setupTabs()

        // Observe data
        observeViewModel()

        // Get reference to the drawer layout
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)

        // Initialize loading overlay
        loadingOverlay = findViewById(R.id.loadingOverlay)

        setupDrawer(drawerLayout)
        setupFab()

        // Load user profile
        loadUserProfile()

        // Refresh dashboard on app start to detect cancelled classes
        viewModel.refreshDashboardOnStart()

        // Request notification permission if not granted
        requestNotificationPermissionIfNeeded()
    }

    /**
     * Request notification permission (Android 13+) on first use
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (!PermissionManager.hasNotificationPermission(this)) {
            // Check if we should show rationale
            val shouldShowRationale = PermissionManager.shouldShowNotificationRationale(this)

            // Request with or without rationale
            PermissionManager.requestNotificationPermission(
                activity = this,
                launcher = notificationPermissionLauncher,
                showRationale = shouldShowRationale
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload dashboard sessions when activity comes to foreground
        viewModel.loadDashboardSessions()
        // Reload user profile to get latest data
        loadUserProfile()
    }

    private fun setupFab() {
        val fab = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddTimetable)
        fab.setOnClickListener {
            val intent = Intent(this, CustomTimetable::class.java)
            startActivity(intent)
        }
    }

    private fun setupTabs() {
        val btnTabToday = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnTabToday)
        val btnTabAllTimetable = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnTabAllTimetable)
        val scrollViewToday = findViewById<android.widget.ScrollView>(R.id.scrollViewToday)
        val containerAllTimetable = findViewById<LinearLayout>(R.id.containerAllTimetable)

        btnTabToday.setOnClickListener {
            // Update tab appearance
            btnTabToday.backgroundTintList = null
            btnTabToday.setBackgroundResource(R.drawable.selected_tab_left)
            btnTabToday.setTextColor(resources.getColor(android.R.color.white, null))

            btnTabAllTimetable.backgroundTintList = null
            btnTabAllTimetable.setBackgroundResource(R.drawable.unselected_tab_right)
            btnTabAllTimetable.setTextColor(resources.getColor(R.color.navy, null))

            // Show/hide views
            scrollViewToday.visibility = android.view.View.VISIBLE
            containerAllTimetable.visibility = android.view.View.GONE
        }

        btnTabAllTimetable.setOnClickListener {
            // Update tab appearance
            btnTabAllTimetable.backgroundTintList = null
            btnTabAllTimetable.setBackgroundResource(R.drawable.selected_tab_right)
            btnTabAllTimetable.setTextColor(resources.getColor(android.R.color.white, null))

            btnTabToday.backgroundTintList = null
            btnTabToday.setBackgroundResource(R.drawable.unselected_tab_left)
            btnTabToday.setTextColor(resources.getColor(R.color.navy, null))

            // Show/hide views
            scrollViewToday.visibility = android.view.View.GONE
            containerAllTimetable.visibility = android.view.View.VISIBLE

            // Load all timetable for current day
            viewModel.loadAllSessionsForDay(currentSelectedDay)
        }
    }

    private fun setupAllTimetableView() {
        val rvAllTimetable = findViewById<RecyclerView>(R.id.rvAllTimetable)
        allTimetableAdapter = DashboardSessionAdapter(
            onLongClick = { session ->
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Remove Session")
                    .setMessage("Remove ${session.courseName} from dashboard?")
                    .setPositiveButton("Remove") { _, _ ->
                        viewModel.deleteDashboardSession(session)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        rvAllTimetable.layoutManager = LinearLayoutManager(this)
        rvAllTimetable.adapter = allTimetableAdapter

        // Setup day tabs
        val tabLayoutDays = findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayoutDays)
        tabLayoutDays.removeAllTabs()
        days.forEach { day ->
            tabLayoutDays.addTab(tabLayoutDays.newTab().setText(day))
        }

        // Add swipe gesture to navigate between days
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false

                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                if (abs(diffX) > abs(diffY) && abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    val currentIndex = days.indexOf(currentSelectedDay)
                    if (diffX < 0) {
                        // Swipe left - go to next day
                        if (currentIndex < days.size - 1) {
                            tabLayoutDays.getTabAt(currentIndex + 1)?.select()
                        }
                    } else {
                        // Swipe right - go to previous day
                        if (currentIndex > 0) {
                            tabLayoutDays.getTabAt(currentIndex - 1)?.select()
                        }
                    }
                    return true
                }
                return false
            }
        })

        rvAllTimetable.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            false // Allow RecyclerView to handle scrolling
        }

        tabLayoutDays.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                tab?.let {
                    currentSelectedDay = days[it.position]
                    viewModel.loadAllSessionsForDay(currentSelectedDay)
                }
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.rvDashboardSessions)
        adapter = DashboardSessionAdapter(
            onLongClick = { session ->
                // Show confirmation and delete
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Remove Session")
                    .setMessage("Remove ${session.courseName} from dashboard?")
                    .setPositiveButton("Remove") { _, _ ->
                        viewModel.deleteDashboardSession(session)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        // Observe current day
        viewModel.currentDay.observe(this) { day ->
            findViewById<TextView>(R.id.tvCurrentDay).text = day
        }

        // Observe today's sessions
        viewModel.todaysSessions.observe(this) { sessions ->
            adapter.submitList(sessions)

            // Show/hide empty state
            findViewById<TextView>(R.id.tvEmptyState).visibility =
                if (sessions.isEmpty()) android.view.View.VISIBLE
                else android.view.View.GONE
        }

        // Observe all day sessions for All Timetable tab
        viewModel.allDaySessions.observe(this) { sessions ->
            allTimetableAdapter.submitList(sessions)

            // Show/hide empty state
            findViewById<TextView>(R.id.tvEmptyStateAll).visibility =
                if (sessions.isEmpty()) android.view.View.VISIBLE
                else android.view.View.GONE
        }

        // Observe error messages
        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Snackbar.make(findViewById(android.R.id.content), it, Snackbar.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        // Observe refresh state for loading overlay
        viewModel.isRefreshing.observe(this) { isRefreshing ->
            loadingOverlay.visibility = if (isRefreshing) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun setupDrawer(drawerLayout: DrawerLayout) {

        // Top app bar - open drawer on menu icon click
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Handle notification icon click
        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.notification_icon -> {
                    val intent = Intent(this, Notification::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Drawer menu top bar - close drawer on menu icon click
        val topBarMenu = findViewById<Toolbar>(R.id.top_bar_menu)
        topBarMenu.setNavigationOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Batch Timetable button
        val btnBatchTimetable = findViewById<Button>(R.id.btnBatchTimetable)
        btnBatchTimetable.setOnClickListener {
            val intent = Intent(this, CustomTimetable::class.java)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Custom Timetable button
        val btnCustomTimetable = findViewById<Button>(R.id.btnCustomTimetable)
        btnCustomTimetable.setOnClickListener {
            val intent = Intent(this, Add_Timetable::class.java)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Faculty Offices button
        val btnFacultyOffices = findViewById<Button>(R.id.btnFacultyOffices)
        btnFacultyOffices.setOnClickListener {
            startActivity(Intent(this, com.example.fastable.FacultyOffices::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // About Us button
        val btnAboutUs = findViewById<Button>(R.id.btnAboutUs)
        btnAboutUs.setOnClickListener {
            val intent = Intent(this, AboutUs::class.java)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // User Manual button
        val btnUserManual = findViewById<Button>(R.id.btnUserManual)
        btnUserManual.setOnClickListener {
            val intent = Intent(this, UserManual::class.java)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Clash Detector button - opens external link
        val btnClashDetector = findViewById<Button>(R.id.btnClashDetector)
        btnClashDetector.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://clashes-detector-dvam.vercel.app/"))
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Campus Map button
        val btnMap = findViewById<Button>(R.id.btnMap)
        btnMap.setOnClickListener {
            val intent = Intent(this, CampusMap::class.java)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Free Rooms button
        val btnFreeRooms = findViewById<Button>(R.id.btnFreeRooms)
        btnFreeRooms.setOnClickListener {
            val intent = Intent(this, FreeRooms::class.java)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Logout button
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Profile section
        val profile = findViewById<LinearLayout>(R.id.profileSection)
        profile.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }


    private fun loadUserProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Load from offline database first (instant)
            CoroutineScope(Dispatchers.IO).launch {
                val offlineProfile = AppDatabase.getDatabase(this@Home).userProfileDao()
                    .getUserProfile(currentUser.uid)

                withContext(Dispatchers.Main) {
                    if (offlineProfile != null) {
                        // Display offline data immediately
                        displayUserProfile(offlineProfile)
                        android.util.Log.d("Home", "Loaded profile from offline DB")
                    }
                }

                // Then sync with Firebase in background (for updates)
                syncProfileFromFirebase(currentUser.uid)
            }
        }
    }

    private fun displayUserProfile(profile: UserProfile) {
        findViewById<TextView>(R.id.tvName).text = profile.name
        findViewById<TextView>(R.id.tvEmail).text = profile.email

        val profileImage = findViewById<CircleImageView>(R.id.profileImage)
        if (profile.profileImageUrl.isNotEmpty()) {
            val localFile = File(profile.profileImageUrl)
            if (localFile.exists()) {
                // Use Picasso for loading local files
                com.squareup.picasso.Picasso.get()
                    .load(localFile)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(profileImage)
            } else {
                profileImage.setImageResource(R.drawable.ic_profile_placeholder)
            }
        } else {
            profileImage.setImageResource(R.drawable.ic_profile_placeholder)
        }
    }

    private fun syncProfileFromFirebase(uid: String) {
        database.child("users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val name = snapshot.child("name").getValue(String::class.java) ?: ""
                        val email = snapshot.child("email").getValue(String::class.java) ?: ""
                        val batch = snapshot.child("batch").getValue(String::class.java) ?: ""
                        val degree = snapshot.child("degree").getValue(String::class.java) ?: ""
                        val section = snapshot.child("section").getValue(String::class.java) ?: ""
                        // This will be Base64 string from Firebase
                        val profileImageBase64 = snapshot.child("profileImageUrl").getValue(String::class.java) ?: ""

                        CoroutineScope(Dispatchers.IO).launch {
                            // Convert Base64 to local file path
                            val localImagePath = if (profileImageBase64.isNotEmpty()) {
                                saveBase64ImageLocally(uid, profileImageBase64)
                            } else {
                                ""
                            }

                            // Save to offline database
                            val profile = UserProfile(
                                uid = uid,
                                name = name,
                                email = email,
                                batch = batch,
                                degree = degree,
                                section = section,
                                profileImageUrl = localImagePath  // Store local path
                            )

                            AppDatabase.getDatabase(this@Home).userProfileDao()
                                .insertUserProfile(profile)

                            withContext(Dispatchers.Main) {
                                // Update UI with synced data
                                displayUserProfile(profile)
                                android.util.Log.d("Home", "Synced profile from Firebase")
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e("Home", "Error syncing profile: ${error.message}")
                }
            })
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
                android.util.Log.e("Home", "Failed to decode Base64 image")
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
                android.util.Log.d("Home", "Profile image saved locally: ${file.absolutePath}")
                file.absolutePath
            } else {
                android.util.Log.e("Home", "Failed to save profile image")
                ""
            }
        } catch (e: Exception) {
            android.util.Log.e("Home", "Error saving Base64 image: ${e.message}", e)
            ""
        }
    }
}
