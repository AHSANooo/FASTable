package com.example.fastable

import android.content.Intent
import android.os.Bundle
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
import com.example.fastable.adapters.CourseAdapter
import com.example.fastable.adapters.SelectedCourseChipAdapter
import com.example.fastable.adapters.TimetableAdapter
import com.example.fastable.viewmodel.CustomTimetableViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import kotlin.math.abs

class Add_Timetable : AppCompatActivity() {

    private lateinit var viewModel: CustomTimetableViewModel
    private lateinit var courseAdapter: CourseAdapter
    private lateinit var timetableAdapter: TimetableAdapter
    private lateinit var chipAdapter: SelectedCourseChipAdapter

    private lateinit var rvCourses: RecyclerView
    private lateinit var rvTimetable: RecyclerView
    private lateinit var rvSelectedChips: RecyclerView
    private lateinit var searchView: SearchView
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_timetable_new)

        initViews()
        setupViewModel()
        setupRecyclerViews()
        setupListeners()

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
            val realCourses = courses.filter { !it.name.equals("Placeholder", ignoreCase = true) }
            courseAdapter.submitList(realCourses)
            updateCourseVisibility(realCourses, viewModel.isLoading.value ?: false)
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
            updateCourseVisibility(viewModel.courses.value, isLoading)
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

    private fun updateCourseVisibility(courses: List<com.example.fastable.data.models.Course>?, isLoading: Boolean) {
        val realCourses = courses?.filter { !it.name.equals("Placeholder", ignoreCase = true) } ?: emptyList()
        if (isLoading && realCourses.isEmpty()) {
            // Keep loading active and courses hidden while courses are being fetched
            progressBar.visibility = View.VISIBLE
            rvCourses.visibility = View.GONE
            tvNoData.visibility = View.GONE
        } else if (realCourses.isEmpty()) {
            progressBar.visibility = View.GONE
            rvCourses.visibility = View.GONE
            tvNoData.visibility = View.VISIBLE
        } else {
            // Content appears once fully fetched and cached
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            rvCourses.visibility = View.VISIBLE
            tvNoData.visibility = View.GONE
        }
    }

    private fun setupRecyclerViews() {
        courseAdapter = CourseAdapter { course ->
            viewModel.toggleCourseSelection(course)
        }
        rvCourses.layoutManager = LinearLayoutManager(this)
        rvCourses.adapter = courseAdapter

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
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchCourses(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { viewModel.searchCourses(it) }
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
}
