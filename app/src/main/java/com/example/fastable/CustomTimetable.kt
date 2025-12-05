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
    private lateinit var btnSetAsDefault: Button
    private lateinit var progressBar: ProgressBar
    private var progressDialog: android.app.ProgressDialog? = null

    private val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
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

        viewModel.syncData()
    }

    private fun initViews() {
        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.tvBatchTimetable).setOnClickListener {
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
                btnSetAsDefault.visibility = View.GONE
            } else {
                tvNoData.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                btnSetAsDefault.visibility = View.VISIBLE
                updateSessionsForDay(days[tabLayout.selectedTabPosition])
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            // Hide other views when loading
            if (isLoading) {
                tvNoData.visibility = View.GONE
                recyclerView.visibility = View.GONE
                tvError.visibility = View.GONE
            }
        }

        // Observe errors and success messages
        viewModel.errorMessage.observe(this) { error ->
            if (error != null) {
                if (error == "Batch set as default!") {
                    // Success - navigate to Home
                    progressDialog?.dismiss()
                    Toast.makeText(this, "Successfully set as default! Navigating to Dashboard...", Toast.LENGTH_SHORT).show()

                    // Navigate to Home after a short delay
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        val intent = Intent(this, Home::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish()
                    }, 500)
                } else {
                    // Error - show and re-enable button
                    progressDialog?.dismiss()
                    tvError.text = error
                    tvError.visibility = View.VISIBLE
                    btnSetAsDefault.isEnabled = true
                }
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

            currentBatch = batch
            currentSection = section
            viewModel.loadTimetable(batch, section)
        }

        btnSetAsDefault.setOnClickListener {
            if (currentBatch.isEmpty() || currentSection.isEmpty()) {
                Toast.makeText(this, "Please load a timetable first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show progress dialog
            progressDialog = android.app.ProgressDialog(this).apply {
                setMessage("Setting as default...\nPlease wait")
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