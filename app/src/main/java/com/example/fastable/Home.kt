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
import com.example.fastable.services.TimetableSyncWorker
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
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import kotlin.math.abs

class Home : AppCompatActivity() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: DashboardSessionAdapter
    private lateinit var allTimetableAdapter: DashboardSessionAdapter
    private lateinit var database: DatabaseReference
    private lateinit var loadingOverlay: FrameLayout
    private val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private var currentSelectedDay = "Monday"

    // Permission launcher
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize notification permission launcher
        notificationPermissionLauncher = PermissionManager.createNotificationPermissionLauncher(
            activity = this,
            onGranted = {},
            onDenied = {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Enable notifications in settings to get class reminders",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        )

        database = FirebaseDatabase.getInstance().reference
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        setupRecyclerView()
        setupAllTimetableView()
        setupTabs()
        setupTipBanner()
        observeViewModel()

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        setupDrawer(drawerLayout)
        setupFab()
        loadUserProfile()
        viewModel.refreshDashboardOnStart()
        requestNotificationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (!PermissionManager.hasNotificationPermission(this)) {
            val shouldShowRationale = PermissionManager.shouldShowNotificationRationale(this)
            PermissionManager.requestNotificationPermission(
                activity = this,
                launcher = notificationPermissionLauncher,
                showRationale = shouldShowRationale
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDashboardSessions()
        loadUserProfile()
        TimetableSyncWorker.runImmediateSync(this)
    }

    private fun setupTipBanner() {
        val cardTipBanner = findViewById<View>(R.id.cardTipBanner)
        val btnDismissTip = findViewById<ImageButton>(R.id.btnDismissTip)
        val prefs = getSharedPreferences("fastable_prefs", MODE_PRIVATE)
        val isDismissed = prefs.getBoolean("tip_banner_dismissed", false)

        cardTipBanner.visibility = if (isDismissed) View.GONE else View.VISIBLE
        btnDismissTip.setOnClickListener {
            prefs.edit().putBoolean("tip_banner_dismissed", true).apply()
            cardTipBanner.visibility = View.GONE
        }
    }

    private fun setupFab() {
        val fab = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddTimetable)
        fab.setOnClickListener {
            val intent = Intent(this, Add_Timetable::class.java)
            startActivity(intent)
        }

        val fabRefresh = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabRefresh)
        fabRefresh.setOnClickListener {
            viewModel.manualRefresh()
        }
    }

    private fun setupTabs() {
        val btnTabToday = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnTabToday)
        val btnTabAllTimetable = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnTabAllTimetable)
        val scrollViewToday = findViewById<android.widget.ScrollView>(R.id.scrollViewToday)
        val containerAllTimetable = findViewById<LinearLayout>(R.id.containerAllTimetable)

        btnTabToday.setOnClickListener {
            btnTabToday.backgroundTintList = null
            btnTabToday.setBackgroundResource(R.drawable.selected_tab_left)
            btnTabToday.setTextColor(getColor(R.color.white))

            btnTabAllTimetable.backgroundTintList = null
            btnTabAllTimetable.setBackgroundResource(R.drawable.unselected_tab_right)
            btnTabAllTimetable.setTextColor(getColor(R.color.secondary_navy))

            scrollViewToday.visibility = View.VISIBLE
            containerAllTimetable.visibility = View.GONE
        }

        btnTabAllTimetable.setOnClickListener {
            btnTabAllTimetable.backgroundTintList = null
            btnTabAllTimetable.setBackgroundResource(R.drawable.selected_tab_right)
            btnTabAllTimetable.setTextColor(getColor(R.color.white))

            btnTabToday.backgroundTintList = null
            btnTabToday.setBackgroundResource(R.drawable.unselected_tab_left)
            btnTabToday.setTextColor(getColor(R.color.secondary_navy))

            scrollViewToday.visibility = View.GONE
            containerAllTimetable.visibility = View.VISIBLE

            viewModel.loadAllSessionsForDay(currentSelectedDay)
        }
    }

    private fun setupAllTimetableView() {
        val rvAllTimetable = findViewById<RecyclerView>(R.id.rvAllTimetable)
        allTimetableAdapter = DashboardSessionAdapter(
            onLongClick = { session ->
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Remove Class")
                    .setMessage("Remove ${session.courseName} from your dashboard?")
                    .setPositiveButton("Remove") { _, _ ->
                        viewModel.deleteDashboardSession(session)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        rvAllTimetable.layoutManager = LinearLayoutManager(this)
        rvAllTimetable.adapter = allTimetableAdapter

        val tabLayoutDays = findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayoutDays)
        tabLayoutDays.removeAllTabs()
        days.forEach { day ->
            tabLayoutDays.addTab(tabLayoutDays.newTab().setText(day))
        }

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
                        if (currentIndex < days.size - 1) {
                            tabLayoutDays.getTabAt(currentIndex + 1)?.select()
                        }
                    } else {
                        if (currentIndex > 0) {
                            tabLayoutDays.getTabAt(currentIndex - 1)?.select()
                        }
                    }
                    return true
                }
                return false
            }
        })

        rvAllTimetable.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
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
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Remove Class")
                    .setMessage("Remove ${session.courseName} from your dashboard?")
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
        viewModel.currentDay.observe(this) { day ->
            findViewById<TextView>(R.id.tvCurrentDay).text = day
        }

        viewModel.dashboardSessions.observe(this) {
            updateEmptyStates()
        }

        viewModel.todaysSessions.observe(this) { sessions ->
            adapter.submitList(sessions)
            updateEmptyStates()
        }

        viewModel.allDaySessions.observe(this) { sessions ->
            allTimetableAdapter.submitList(sessions)
            updateEmptyStates()
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Snackbar.make(findViewById(android.R.id.content), it, Snackbar.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        viewModel.isRefreshing.observe(this) { isRefreshing ->
            loadingOverlay.visibility = if (isRefreshing) View.VISIBLE else View.GONE
        }

        // Empty state button handlers
        findViewById<Button>(R.id.btnAddCoursesToday)?.setOnClickListener {
            startActivity(Intent(this, Add_Timetable::class.java))
        }
        findViewById<Button>(R.id.btnAddCoursesAll)?.setOnClickListener {
            startActivity(Intent(this, Add_Timetable::class.java))
        }
    }

    private fun updateEmptyStates() {
        val totalCourses = viewModel.dashboardSessions.value?.size ?: 0
        val todaysCourses = viewModel.todaysSessions.value?.size ?: 0
        val allDayCourses = viewModel.allDaySessions.value?.size ?: 0

        val layoutEmptyToday = findViewById<LinearLayout>(R.id.layoutEmptyToday)
        val tvEmptyTitleToday = findViewById<TextView>(R.id.tvEmptyTitleToday)
        val tvEmptySubtitleToday = findViewById<TextView>(R.id.tvEmptySubtitleToday)
        val btnAddCoursesToday = findViewById<Button>(R.id.btnAddCoursesToday)

        val layoutEmptyAll = findViewById<LinearLayout>(R.id.layoutEmptyAll)
        val tvEmptyTitleAll = findViewById<TextView>(R.id.tvEmptyTitleAll)
        val tvEmptySubtitleAll = findViewById<TextView>(R.id.tvEmptySubtitleAll)
        val btnAddCoursesAll = findViewById<Button>(R.id.btnAddCoursesAll)

        if (todaysCourses == 0) {
            layoutEmptyToday?.visibility = View.VISIBLE
            if (totalCourses == 0) {
                tvEmptyTitleToday?.text = "No Courses Added Yet"
                tvEmptySubtitleToday?.text = "Please add your courses to see your daily schedule."
                btnAddCoursesToday?.text = "Add Courses"
            } else {
                tvEmptyTitleToday?.text = "No Classes Today! 🎉"
                tvEmptySubtitleToday?.text = "Enjoy your day off! Check All Timetable to view upcoming classes."
                btnAddCoursesToday?.text = "Manage Courses"
            }
        } else {
            layoutEmptyToday?.visibility = View.GONE
        }

        if (allDayCourses == 0) {
            layoutEmptyAll?.visibility = View.VISIBLE
            if (totalCourses == 0) {
                tvEmptyTitleAll?.text = "No Courses Added Yet"
                tvEmptySubtitleAll?.text = "Add your courses from Batch or Custom Timetable."
                btnAddCoursesAll?.text = "Add Courses"
            } else {
                tvEmptyTitleAll?.text = "No Classes on $currentSelectedDay"
                tvEmptySubtitleAll?.text = "You have no classes scheduled for this day."
                btnAddCoursesAll?.text = "Add More Courses"
            }
        } else {
            layoutEmptyAll?.visibility = View.GONE
        }
    }

    private fun setupDrawer(drawerLayout: DrawerLayout) {
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

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

        val topBarMenu = findViewById<Toolbar>(R.id.top_bar_menu)
        topBarMenu.setNavigationOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        findViewById<Button>(R.id.btnBatchTimetable).setOnClickListener {
            startActivity(Intent(this, CustomTimetable::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        findViewById<Button>(R.id.btnCustomTimetable).setOnClickListener {
            startActivity(Intent(this, Add_Timetable::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        findViewById<Button>(R.id.btnFacultyOffices).setOnClickListener {
            startActivity(Intent(this, FacultyOffices::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        findViewById<Button>(R.id.btnAboutUs).setOnClickListener {
            startActivity(Intent(this, AboutUs::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        findViewById<Button>(R.id.btnUserManual).setOnClickListener {
            startActivity(Intent(this, UserManual::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Clash Detector - links to requested official URL
        findViewById<Button>(R.id.btnClashDetector).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://clashes-detector.vercel.app/"))
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        findViewById<Button>(R.id.btnMap).setOnClickListener {
            startActivity(Intent(this, CampusMap::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        findViewById<Button>(R.id.btnFreeRooms).setOnClickListener {
            startActivity(Intent(this, FreeRooms::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out of FASTable?")
                .setPositiveButton("Log Out") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, Login::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        findViewById<LinearLayout>(R.id.profileSection).setOnClickListener {
            startActivity(Intent(this, Profile::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun loadUserProfile() {
        CoroutineScope(Dispatchers.IO).launch {
            val offlineProfile = AppDatabase.getDatabase(this@Home).userProfileDao().getAnyUserProfile()
            withContext(Dispatchers.Main) {
                if (offlineProfile != null) {
                    displayUserProfile(offlineProfile)
                }
            }

            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                syncProfileFromFirebase(currentUser.uid)
            }
        }
    }

    private fun displayUserProfile(profile: UserProfile) {
        findViewById<TextView>(R.id.tvName)?.text = profile.name
        findViewById<TextView>(R.id.tvEmail)?.text = profile.email

        val profileImage = findViewById<CircleImageView>(R.id.profileImage) ?: return
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
                    if (snapshot.exists()) {
                        val name = snapshot.child("name").getValue(String::class.java) ?: ""
                        val email = snapshot.child("email").getValue(String::class.java) ?: ""
                        val batch = snapshot.child("batch").getValue(String::class.java) ?: ""
                        val degree = snapshot.child("degree").getValue(String::class.java) ?: ""
                        val section = snapshot.child("section").getValue(String::class.java) ?: ""
                        val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java) ?: ""

                        CoroutineScope(Dispatchers.IO).launch {
                            val localImagePath = if (profileImageUrl.isNotEmpty() && !profileImageUrl.startsWith("http")) {
                                saveBase64ImageLocally(uid, profileImageUrl)
                            } else {
                                profileImageUrl
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

                            AppDatabase.getDatabase(this@Home).userProfileDao().insertUserProfile(profile)

                            withContext(Dispatchers.Main) {
                                displayUserProfile(profile)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun saveBase64ImageLocally(userId: String, base64String: String): String {
        return try {
            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return ""
            val profileImagesDir = File(filesDir, "profile_images")
            if (!profileImagesDir.exists()) profileImagesDir.mkdirs()
            val file = File(profileImagesDir, "profile_$userId.jpg")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            ""
        }
    }
}
