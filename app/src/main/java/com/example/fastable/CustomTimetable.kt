package com.example.fastable

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fastable.adapters.TimetableAdapter
import com.example.fastable.viewmodel.BatchTimetableViewModel
import com.google.android.material.tabs.TabLayout

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

    private val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_timetable)

        initViews()
        setupViewModel()
        setupRecyclerView()
        setupTabLayout()
        setupListeners()

        viewModel.syncData()
    }

    private fun initViews() {
        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.tvBatchTimetable).setOnClickListener {
            val intent = Intent(this, Add_Timetable::class.java)
            startActivity(intent)
        }

        spinnerBatch = findViewById(R.id.spinnerBatch)
        etSection = findViewById(R.id.etSection)
        btnLoad = findViewById(R.id.btnLoadTimetable)
        recyclerView = findViewById(R.id.rvTimetable)
        tvError = findViewById(R.id.tvError)
        tvNoData = findViewById(R.id.tvNoData)
        tabLayout = findViewById(R.id.tabLayout)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[BatchTimetableViewModel::class.java]

        // Observe batches from database
        viewModel.batches.observe(this) { batches ->
            if (batches.isNotEmpty()) {
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, batches)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerBatch.adapter = adapter
            } else {
                // Show loading state
                val emptyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("Syncing data..."))
                emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerBatch.adapter = emptyAdapter
            }
        }

        // Observe timetable sessions
        viewModel.timetableSessions.observe(this) { sessions ->
            if (sessions.isEmpty()) {
                tvNoData.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                tvNoData.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                updateSessionsForDay(days[tabLayout.selectedTabPosition])
            }
        }

        // Observe errors
        viewModel.errorMessage.observe(this) { error ->
            if (error != null) {
                tvError.text = error
                tvError.visibility = View.VISIBLE
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

    private fun setupListeners() {
        btnLoad.setOnClickListener {
            val batch = spinnerBatch.selectedItem?.toString() ?: ""
            val section = etSection.text.toString().trim().uppercase()

            if (batch.isEmpty() || batch == "Syncing data...") {
                Toast.makeText(this, "Please wait for batches to load", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (section.isEmpty()) {
                Toast.makeText(this, "Please enter a section", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.loadTimetable(batch, section)
        }
    }

    private fun updateSessionsForDay(day: String) {
        val sessions = viewModel.getSessionsByDay(day)
        adapter.submitList(sessions)
    }
}