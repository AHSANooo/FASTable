package com.example.fastable

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fastable.adapters.TimetableAdapter
import com.example.fastable.viewmodel.BatchTimetableViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import kotlin.math.abs

class CustomTimetable : AppCompatActivity() {

    private lateinit var viewModel: BatchTimetableViewModel
    private lateinit var adapter: TimetableAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tabLayout: TabLayout
    private lateinit var spinnerBatch: Spinner
    private lateinit var etSection: EditText
    private lateinit var btnLoad: Button
    private lateinit var tvError: TextView
    private lateinit var tvNoData: TextView
    private lateinit var btnSetAsDefault: Button
    private lateinit var progressBar: ProgressBar
    private var progressDialog: android.app.ProgressDialog? = null

    private val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private var currentBatch: String = ""
    private var currentSection: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_timetable)

        initViews()
        setupViewModel()
        setupRecyclerView()
        setupTabLayout()
        setupListeners()
    }

    private fun initViews() {
        val topBar = findViewById<MaterialToolbar>(R.id.topBar)
        topBar.setNavigationOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.tvCustomTimetable).setOnClickListener {
            val intent = Intent(this, Add_Timetable::class.java)
            startActivity(intent)
            finish()
        }

        spinnerBatch = findViewById(R.id.spinnerBatch)
        etSection = findViewById(R.id.etSection)
        btnLoad = findViewById(R.id.btnLoadTimetable)
        recyclerView = findViewById(R.id.rvTimetable)
        tvError = findViewById(R.id.tvError)
        tvNoData = findViewById(R.id.tvNoData)
        tabLayout = findViewById(R.id.tabLayout)
        btnSetAsDefault = findViewById(R.id.btnSetAsDefault)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[BatchTimetableViewModel::class.java]

        viewModel.batches.observe(this) { batches ->
            if (batches.isNotEmpty()) {
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, batches)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerBatch.adapter = adapter
            } else {
                val emptyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("Loading batches..."))
                emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerBatch.adapter = emptyAdapter
            }
        }

        viewModel.timetableSessions.observe(this) { sessions ->
            if (sessions.isEmpty()) {
                tvNoData.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                tabLayout.visibility = View.GONE
                btnSetAsDefault.visibility = View.GONE
            } else {
                tvNoData.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                tabLayout.visibility = View.VISIBLE
                btnSetAsDefault.visibility = View.VISIBLE
                updateSessionsForDay(days[tabLayout.selectedTabPosition])
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                tvNoData.visibility = View.GONE
                recyclerView.visibility = View.GONE
                tvError.visibility = View.GONE
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            progressDialog?.dismiss()
            btnSetAsDefault.isEnabled = true
            if (error != null) {
                if (error.contains("Default batch set to", ignoreCase = true) || error.contains("Batch set as default", ignoreCase = true)) {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        val intent = Intent(this, Home::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(intent)
                        finish()
                    }, 400)
                } else {
                    tvError.text = error
                    tvError.visibility = View.VISIBLE
                }
                viewModel.clearError()
            } else {
                tvError.visibility = View.GONE
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = TimetableAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupTabLayout() {
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

        recyclerView.setOnTouchListener { _, event ->
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

    private fun setupListeners() {
        btnLoad.setOnClickListener {
            val batch = spinnerBatch.selectedItem?.toString() ?: ""
            val section = etSection.text.toString().trim().uppercase()

            if (batch.isEmpty() || batch.startsWith("Loading")) {
                Toast.makeText(this, "Please wait for batches to load", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (section.isEmpty()) {
                etSection.error = "Enter section (e.g. A)"
                return@setOnClickListener
            }

            currentBatch = batch
            currentSection = section
            viewModel.loadTimetable(batch, section)
        }

        btnSetAsDefault.setOnClickListener {
            if (currentBatch.isEmpty() || currentSection.isEmpty()) {
                Toast.makeText(this, "Please load a timetable first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressDialog = android.app.ProgressDialog(this).apply {
                setMessage("Setting as default schedule...")
                setCancelable(false)
                show()
            }

            btnSetAsDefault.isEnabled = false
            viewModel.setDefaultBatch(currentBatch, currentSection)
        }
    }

    private fun updateSessionsForDay(day: String) {
        val sessions = viewModel.getSessionsByDay(day)
        adapter.submitList(sessions)
    }
}
