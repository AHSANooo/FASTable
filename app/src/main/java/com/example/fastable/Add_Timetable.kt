package com.example.fastable

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fastable.adapters.CourseAdapter
import com.example.fastable.adapters.TimetableAdapter
import com.example.fastable.viewmodel.CustomTimetableViewModel
import com.google.android.material.tabs.TabLayout

class Add_Timetable : AppCompatActivity() {

    private lateinit var viewModel: CustomTimetableViewModel
    private lateinit var courseAdapter: CourseAdapter
    private lateinit var timetableAdapter: TimetableAdapter
    private lateinit var rvCourses: RecyclerView
    private lateinit var rvTimetable: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var btnGenerate: Button
    private lateinit var btnClear: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var tvNoData: TextView
    private lateinit var tvSelectedCount: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var llCourseSelection: LinearLayout
    private lateinit var llTimetableView: LinearLayout

    private val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
    private var showingTimetable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_timetable_new)

        initViews()
        setupViewModel()
        setupRecyclerViews()
        setupListeners()

        // Handle back press
        onBackPressedDispatcher.addCallback(this) {
            if (showingTimetable) {
                showCourseSelection()
            } else {
                finish()
            }
        }
    }

    private fun initViews() {
        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            if (showingTimetable) {
                showCourseSelection()
            } else {
                finish()
            }
        }

        findViewById<TextView>(R.id.tvBatchTimetable).setOnClickListener {
            val intent = Intent(this, CustomTimetable::class.java)
            startActivity(intent)
        }

        rvCourses = findViewById(R.id.rvCourses)
        rvTimetable = findViewById(R.id.rvTimetable)
        searchView = findViewById(R.id.searchView)
        btnGenerate = findViewById(R.id.btnGenerate)
        btnClear = findViewById(R.id.btnClear)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)
        tvNoData = findViewById(R.id.tvNoData)
        tvSelectedCount = findViewById(R.id.tvSelectedCount)
        tabLayout = findViewById(R.id.tabLayout)
        llCourseSelection = findViewById(R.id.llCourseSelection)
        llTimetableView = findViewById(R.id.llTimetableView)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[CustomTimetableViewModel::class.java]

        // Observe courses
        viewModel.courses.observe(this) { courses ->
            courseAdapter.submitList(courses)
        }

        // Observe selected courses
        viewModel.selectedCourses.observe(this) { selected ->
            tvSelectedCount.text = "Selected: ${selected.size} courses"
            btnGenerate.isEnabled = selected.isNotEmpty()
        }

        // Observe timetable
        viewModel.timetableSessions.observe(this) { sessions ->
            if (sessions.isNotEmpty()) {
                showTimetable()
                setupDayTabs()
                updateSessionsForDay(days[0])
            }
        }

        // Observe loading
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnGenerate.isEnabled = !isLoading
        }

        // Observe errors
        viewModel.errorMessage.observe(this) { error ->
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // Sync data
        viewModel.syncData()
    }

    private fun setupRecyclerViews() {
        courseAdapter = CourseAdapter { course ->
            viewModel.toggleCourseSelection(course)
        }
        rvCourses.layoutManager = LinearLayoutManager(this)
        rvCourses.adapter = courseAdapter

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
            Toast.makeText(this, "All selections cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDayTabs() {
        tabLayout.removeAllTabs()
        days.forEach { day ->
            tabLayout.addTab(tabLayout.newTab().setText(day))
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
        findViewById<TextView>(R.id.tvTitle).text = "Custom Timetable"
    }

    private fun showCourseSelection() {
        showingTimetable = false
        llCourseSelection.visibility = View.VISIBLE
        llTimetableView.visibility = View.GONE
        findViewById<TextView>(R.id.tvTitle).text = "Select Courses"
    }
}