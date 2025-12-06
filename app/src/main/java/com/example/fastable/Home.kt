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
import com.example.fastable.adapters.DashboardSessionAdapter
import com.example.fastable.viewmodel.HomeViewModel

class Home : AppCompatActivity() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: DashboardSessionAdapter
    private lateinit var allTimetableAdapter: DashboardSessionAdapter
    private val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
    private var currentSelectedDay = "Monday"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

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

        setupDrawer(drawerLayout)
        setupFab()
    }

    override fun onResume() {
        super.onResume()
        // Reload dashboard sessions when activity comes to foreground
        viewModel.loadDashboardSessions()
    }

    private fun setupFab() {
        val fab = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddTimetable)
        fab.setOnClickListener {
            val intent = Intent(this, CustomTimetable::class.java)
            startActivity(intent)
        }
    }

    private fun setupTabs() {
        val btnTabToday = findViewById<Button>(R.id.btnTabToday)
        val btnTabAllTimetable = findViewById<Button>(R.id.btnTabAllTimetable)
        val scrollViewToday = findViewById<android.widget.ScrollView>(R.id.scrollViewToday)
        val containerAllTimetable = findViewById<LinearLayout>(R.id.containerAllTimetable)

        btnTabToday.setOnClickListener {
            // Update tab appearance
            btnTabToday.setBackgroundResource(R.drawable.selected_tab_left)
            btnTabToday.setTextColor(resources.getColor(android.R.color.white, null))
            btnTabAllTimetable.setBackgroundResource(R.drawable.unselected_tab_right)
            btnTabAllTimetable.setTextColor(resources.getColor(R.color.navy, null))

            // Show/hide views
            scrollViewToday.visibility = android.view.View.VISIBLE
            containerAllTimetable.visibility = android.view.View.GONE
        }

        btnTabAllTimetable.setOnClickListener {
            // Update tab appearance
            btnTabAllTimetable.setBackgroundResource(R.drawable.selected_tab_right)
            btnTabAllTimetable.setTextColor(resources.getColor(android.R.color.white, null))
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

        // About Us button
        val btnAboutUs = findViewById<Button>(R.id.btnAboutUs)
        btnAboutUs.setOnClickListener {
            val intent = Intent(this, AboutUs::class.java)
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
}
