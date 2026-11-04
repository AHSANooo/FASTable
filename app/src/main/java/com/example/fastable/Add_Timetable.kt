package com.example.fastable

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.example.fastable.data.local.AppDatabase
import com.example.fastable.utils.RosterSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fastable.adapters.CourseGroup
import com.example.fastable.adapters.GroupedCourseAdapter
import com.example.fastable.adapters.SelectedCourseChipAdapter
import com.example.fastable.adapters.TimetableAdapter
import com.example.fastable.data.models.Course
import com.example.fastable.utils.TimeParser
import com.example.fastable.viewmodel.CustomTimetableViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import kotlin.math.abs

class Add_Timetable : AppCompatActivity() {

    private lateinit var viewModel: CustomTimetableViewModel
    private lateinit var groupedCourseAdapter: GroupedCourseAdapter
    private lateinit var timetableAdapter: TimetableAdapter
    private lateinit var chipAdapter: SelectedCourseChipAdapter

    private lateinit var rvCourses: RecyclerView
    private lateinit var rvTimetable: RecyclerView
    private lateinit var rvSelectedChips: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var chipGroupDepartment: ChipGroup
    private lateinit var btnGenerate: Button
    private lateinit var btnClear: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var tvNoData: TextView
    private lateinit var tvSelectedCount: TextView
    private lateinit var tvToggleHint: TextView
    private lateinit var layoutExpandedDock: LinearLayout
    private lateinit var btnToggleDock: View
    private lateinit var tabLayout: TabLayout
    private lateinit var llCourseSelection: View
    private lateinit var llTimetableView: LinearLayout
    private lateinit var btnSaveToDashboard: Button
    private lateinit var topBar: MaterialToolbar
    private var progressDialog: android.app.ProgressDialog? = null

    private val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private var showingTimetable = false
    private var isDockExpanded = false

    private var allLoadedCourses: List<Course> = emptyList()
    private var currentSelectedDept: String = "All"
    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_timetable_new)

        initViews()
        setupViewModel()
        setupRecyclerViews()
        setupListeners()
        setupRosterSync()

        onBackPressedDispatcher.addCallback(this) {
            if (showingTimetable) {
                showCourseSelection()
            } else {
                finish()
            }
        }
    }

    private fun initViews() {
        topBar = findViewById(R.id.topBar)
        topBar.setNavigationOnClickListener {
            if (showingTimetable) {
                showCourseSelection()
            } else {
                finish()
            }
        }

        findViewById<TextView>(R.id.tvBatchTimetable).setOnClickListener {
            val intent = Intent(this, CustomTimetable::class.java)
            startActivity(intent)
            finish()
        }

        rvCourses = findViewById(R.id.rvCourses)
        rvTimetable = findViewById(R.id.rvTimetable)
        rvSelectedChips = findViewById(R.id.rvSelectedChips)
        searchView = findViewById(R.id.searchView)
        chipGroupDepartment = findViewById(R.id.chipGroupDepartment)
        btnGenerate = findViewById(R.id.btnGenerate)
        btnClear = findViewById(R.id.btnClear)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)
        tvNoData = findViewById(R.id.tvNoData)
        tvSelectedCount = findViewById(R.id.tvSelectedCount)
        tvToggleHint = findViewById(R.id.tvToggleHint)
        layoutExpandedDock = findViewById(R.id.layoutExpandedDock)
        btnToggleDock = findViewById(R.id.btnToggleDock)
        tabLayout = findViewById(R.id.tabLayout)
        llCourseSelection = findViewById(R.id.llCourseSelection)
        llTimetableView = findViewById(R.id.llTimetableView)
        btnSaveToDashboard = findViewById(R.id.btnSaveToDashboard)

        // Initial loading state: hide courses until real courses are confirmed loaded
        rvCourses.visibility = View.GONE
        tvNoData.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        // Toggle Expandable dock
        btnToggleDock.setOnClickListener {
            val selectedCount = viewModel.selectedCourses.value?.size ?: 0
            if (selectedCount > 0) {
                isDockExpanded = !isDockExpanded
                layoutExpandedDock.visibility = if (isDockExpanded) View.VISIBLE else View.GONE
                tvToggleHint.text = if (isDockExpanded) "Tap to hide list ▼" else "Tap to view list ▲"
            }
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[CustomTimetableViewModel::class.java]

        viewModel.courses.observe(this) { courses ->
            allLoadedCourses = courses.filter { !it.name.equals("Placeholder", ignoreCase = true) }
            applyCourseFilters()
        }

        viewModel.selectedCourses.observe(this) { selected ->
            val count = selected.size
            tvSelectedCount.text = "$count course${if (count != 1) "s" else ""} selected"
            btnGenerate.isEnabled = count > 0

            chipAdapter.submitList(selected)

            if (count == 0) {
                isDockExpanded = false
                layoutExpandedDock.visibility = View.GONE
                tvToggleHint.text = "Select courses to begin"
            } else {
                tvToggleHint.text = if (isDockExpanded) "Tap to hide list ▼" else "Tap to view list ▲"
            }

            // Sync selection state on course groups
            applyCourseFilters()
        }

        viewModel.timetableSessions.observe(this) { sessions ->
            if (sessions.isNotEmpty()) {
                showTimetable()
                setupDayTabs()
                updateSessionsForDay(days[0])
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            btnGenerate.isEnabled = !isLoading && (viewModel.selectedCourses.value?.isNotEmpty() == true)
            applyCourseFilters()
        }

        viewModel.errorMessage.observe(this) { error ->
            if (error != null) {
                progressDialog?.dismiss()
                if (error == "Courses added to dashboard!") {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        val intent = Intent(this, Home::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(intent)
                        finish()
                    }, 400)
                } else {
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                    btnSaveToDashboard.isEnabled = true
                }
                viewModel.clearError()
            }
        }

        viewModel.loadOrSyncData(forceRefresh = false)
    }

    private fun applyCourseFilters() {
        val filteredGroups = groupCourses(allLoadedCourses, currentSelectedDept, currentQuery)
        groupedCourseAdapter.submitList(filteredGroups)

        val isLoading = viewModel.isLoading.value ?: false
        if (isLoading && filteredGroups.isEmpty()) {
            progressBar.visibility = View.VISIBLE
            rvCourses.visibility = View.GONE
            tvNoData.visibility = View.GONE
        } else if (filteredGroups.isEmpty()) {
            progressBar.visibility = View.GONE
            rvCourses.visibility = View.GONE
            tvNoData.visibility = View.VISIBLE
        } else {
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            rvCourses.visibility = View.VISIBLE
            tvNoData.visibility = View.GONE
        }
    }

    private fun groupCourses(courses: List<Course>, deptFilter: String, query: String): List<CourseGroup> {
        val queryFiltered = if (query.isNotBlank()) {
            courses.filter { it.name.contains(query, ignoreCase = true) }
        } else {
            courses
        }

        val grouped = queryFiltered.groupBy { course ->
            val dept = if (course.department.isNotBlank()) {
                course.department.trim()
            } else {
                TimeParser.extractDepartmentFromBatch(course.batch).ifEmpty { "General" }
            }
            Pair(course.name.trim(), dept.uppercase())
        }

        val groups = grouped.map { (key, sections) ->
            CourseGroup(
                courseName = key.first,
                department = key.second,
                sections = sections.sortedBy { it.section }
            )
        }.sortedWith(compareBy({ it.department }, { it.courseName }))

        return if (deptFilter.equals("All", ignoreCase = true)) {
            groups
        } else {
            groups.filter { it.department.equals(deptFilter, ignoreCase = true) }
        }
    }

    private fun setupRecyclerViews() {
        groupedCourseAdapter = GroupedCourseAdapter { course ->
            viewModel.toggleCourseSelection(course)
        }
        rvCourses.layoutManager = LinearLayoutManager(this)
        rvCourses.adapter = groupedCourseAdapter

        // Selected chips horizontal list
        chipAdapter = SelectedCourseChipAdapter { courseToRemove ->
            viewModel.toggleCourseSelection(courseToRemove)
        }
        rvSelectedChips.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvSelectedChips.adapter = chipAdapter

        timetableAdapter = TimetableAdapter()
        rvTimetable.layoutManager = LinearLayoutManager(this)
        rvTimetable.adapter = timetableAdapter
    }

    private fun setupListeners() {
        chipGroupDepartment.setOnCheckedStateChangeListener { group, checkedIds ->
            val chipId = checkedIds.firstOrNull() ?: R.id.chipDeptAll
            val chip = group.findViewById<Chip>(chipId)
            currentSelectedDept = chip?.text?.toString() ?: "All"
            applyCourseFilters()
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentQuery = query.orEmpty().trim()
                applyCourseFilters()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText.orEmpty().trim()
                applyCourseFilters()
                return true
            }
        })

        btnGenerate.setOnClickListener {
            viewModel.generateCustomTimetable()
        }

        btnClear.setOnClickListener {
            viewModel.clearAllSelections()
            Toast.makeText(this, "Selections cleared", Toast.LENGTH_SHORT).show()
        }

        btnSaveToDashboard.setOnClickListener {
            progressDialog = android.app.ProgressDialog(this).apply {
                setMessage("Adding courses to dashboard...")
                setCancelable(false)
                show()
            }
            btnSaveToDashboard.isEnabled = false
            viewModel.addToDashboard()
        }
    }

    private fun setupDayTabs() {
        tabLayout.removeAllTabs()
        days.forEach { day ->
            tabLayout.addTab(tabLayout.newTab().setText(day))
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
                    val currentIndex = tabLayout.selectedTabPosition
                    if (diffX < 0) {
                        if (currentIndex < days.size - 1) {
                            tabLayout.getTabAt(currentIndex + 1)?.select()
                        }
                    } else {
                        if (currentIndex > 0) {
                            tabLayout.getTabAt(currentIndex - 1)?.select()
                        }
                    }
                    return true
                }
                return false
            }
        })

        rvTimetable.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    updateSessionsForDay(days[it.position])
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateSessionsForDay(day: String) {
        val sessions = viewModel.getSessionsByDay(day)
        timetableAdapter.submitList(sessions)
    }

    private fun showTimetable() {
        showingTimetable = true
        llCourseSelection.visibility = View.GONE
        llTimetableView.visibility = View.VISIBLE
        topBar.title = "Unified Preview"
    }

    private fun showCourseSelection() {
        showingTimetable = false
        llCourseSelection.visibility = View.VISIBLE
        llTimetableView.visibility = View.GONE
        topBar.title = "Custom Timetable"
    }
    private fun setupRosterSync() {
        val tvDetectedRollNo = findViewById<TextView>(R.id.tvDetectedRollNo) ?: return
        val btnQuickRosterSync = findViewById<View>(R.id.btnQuickRosterSync) ?: return

        lifecycleScope.launch {
            val userEmail = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@Add_Timetable).userProfileDao().getAnyUserProfile()?.email
                    ?: com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(this@Add_Timetable)?.email
            }
            val rollNo = RosterSyncManager.extractRollNoFromEmail(userEmail)
            tvDetectedRollNo.text = rollNo

            btnQuickRosterSync.setOnClickListener {
                val progress = android.app.ProgressDialog(this@Add_Timetable).apply {
                    setMessage("Syncing roster for $rollNo...")
                    setCancelable(false)
                    show()
                }
                lifecycleScope.launch {
                    val result = RosterSyncManager.syncRosterForStudent(this@Add_Timetable, rollNo)
                    progress.dismiss()
                    result.onSuccess { count ->
                        Toast.makeText(this@Add_Timetable, "Roster Synced! Loaded $count classes (CS-E)", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@Add_Timetable, Home::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(intent)
                        finish()
                    }.onFailure { ex ->
                        Toast.makeText(this@Add_Timetable, "Sync failed: ${ex.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}