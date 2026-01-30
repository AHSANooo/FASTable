package com.example.fastable

import android.os.Bundle
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fastable.adapters.FreeRoomAdapter
import com.example.fastable.viewmodel.FreeRoomsViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlin.math.abs

class FreeRooms : AppCompatActivity() {

    private lateinit var viewModel: FreeRoomsViewModel
    private lateinit var adapter: FreeRoomAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tabLayoutDays: TabLayout
    private lateinit var tabLayoutRoomType: TabLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView

    private val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
    private var currentSelectedDay = "Monday"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_free_rooms)

        setupToolbar()
        initViews()
        setupViewModel()
        setupRecyclerView()
        setupDayTabs()
        setupRoomTypeTabs()

        // Load data for first day
        viewModel.loadFreeRoomsForDay(currentSelectedDay)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Free Rooms"
        toolbar.setNavigationOnClickListener { finish() }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_free_rooms, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                Toast.makeText(this, "Refreshing data...", Toast.LENGTH_SHORT).show()
                viewModel.refreshData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.rvFreeRooms)
        tabLayoutDays = findViewById(R.id.tabLayoutDays)
        tabLayoutRoomType = findViewById(R.id.tabLayoutRoomType)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[FreeRoomsViewModel::class.java]

        // Observe filtered rooms (based on Labs/Rooms selection)
        viewModel.filteredRooms.observe(this) { rooms ->
            adapter.submitList(rooms)

            if (rooms.isEmpty()) {
                tvEmptyState.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                tvEmptyState.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                tvEmptyState.visibility = View.GONE
            }
        }

        // Observe errors
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Snackbar.make(findViewById(android.R.id.content), it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = FreeRoomAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupDayTabs() {
        // Add day tabs
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

        recyclerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false // Allow RecyclerView to handle scrolling
        }

        // Tab selection listener
        tabLayoutDays.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    currentSelectedDay = days[it.position]
                    viewModel.loadFreeRoomsForDay(currentSelectedDay)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRoomTypeTabs() {
        // Add Rooms and Labs tabs
        tabLayoutRoomType.addTab(tabLayoutRoomType.newTab().setText("Rooms"))
        tabLayoutRoomType.addTab(tabLayoutRoomType.newTab().setText("Labs"))

        // Tab selection listener
        tabLayoutRoomType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    // false = Rooms (position 0), true = Labs (position 1)
                    viewModel.setRoomTypeFilter(it.position == 1)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
}
