package com.example.fastable

import android.os.Bundle
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fastable.adapters.SlotWiseFreeRoomsAdapter
import com.example.fastable.data.models.SlotWithFreeRooms
import com.example.fastable.viewmodel.FreeRoomsViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import java.util.Calendar
import kotlin.math.abs

class FreeRooms : AppCompatActivity() {

    private lateinit var viewModel: FreeRoomsViewModel
    private lateinit var slotWiseAdapter: SlotWiseFreeRoomsAdapter
    private lateinit var currentSlotAdapter: SlotWiseFreeRoomsAdapter
    private lateinit var nextSlotAdapter: SlotWiseFreeRoomsAdapter

    // Views
    private lateinit var tabLayoutDays: TabLayout
    private lateinit var tabLayoutRoomType: TabLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var rvSlotWise: RecyclerView
    private lateinit var scrollCurrentlyAvailable: android.widget.ScrollView
    private lateinit var containerCurrentlyAvailable: LinearLayout
    private lateinit var rvCurrentSlot: RecyclerView
    private lateinit var rvNextSlot: RecyclerView
    private lateinit var tvCurrentSlotHeader: TextView
    private lateinit var tvNextSlotHeader: TextView
    private lateinit var tvNoClassesNow: TextView
    private lateinit var btnTabBySlots: MaterialButton
    private lateinit var btnTabCurrentlyAvailable: MaterialButton

    private val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private var currentSelectedDay = "Monday"
    private var isSlotWiseViewActive = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_free_rooms)

        // Initialize to current day
        initializeCurrentDay()

        setupToolbar()
        initViews()
        setupViewModel()
        setupRecyclerViews()
        setupMainTabs()
        setupDayTabs()
        setupRoomTypeTabs()

        // Load data for current day
        viewModel.loadSlotWiseFreeRooms(currentSelectedDay)
    }

    private fun initializeCurrentDay() {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        currentSelectedDay = when (dayOfWeek) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            else -> "Monday" // Default to Monday on Sunday
        }
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
        tabLayoutDays = findViewById(R.id.tabLayoutDays)
        tabLayoutRoomType = findViewById(R.id.tabLayoutRoomType)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        rvSlotWise = findViewById(R.id.rvSlotWise)
        scrollCurrentlyAvailable = findViewById(R.id.scrollCurrentlyAvailable)
        containerCurrentlyAvailable = findViewById(R.id.containerCurrentlyAvailable)
        rvCurrentSlot = findViewById(R.id.rvCurrentSlot)
        rvNextSlot = findViewById(R.id.rvNextSlot)
        tvCurrentSlotHeader = findViewById(R.id.tvCurrentSlotHeader)
        tvNextSlotHeader = findViewById(R.id.tvNextSlotHeader)
        tvNoClassesNow = findViewById(R.id.tvNoClassesNow)
        btnTabBySlots = findViewById(R.id.btnTabBySlots)
        btnTabCurrentlyAvailable = findViewById(R.id.btnTabCurrentlyAvailable)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[FreeRoomsViewModel::class.java]

        // Observe slot-wise data
        viewModel.filteredSlotWiseRooms.observe(this) { slots ->
            slotWiseAdapter.submitList(slots)
            updateEmptyState(slots)
        }

        // Observe currently available data
        viewModel.filteredCurrentlyAvailable.observe(this) { slots ->
            updateCurrentlyAvailableView(slots)
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

    private fun setupRecyclerViews() {
        // Slot-wise adapter
        slotWiseAdapter = SlotWiseFreeRoomsAdapter { viewModel.isLabsFilterActive() }
        rvSlotWise.layoutManager = LinearLayoutManager(this)
        rvSlotWise.adapter = slotWiseAdapter

        // Current slot adapter
        currentSlotAdapter = SlotWiseFreeRoomsAdapter { viewModel.isLabsFilterActive() }
        rvCurrentSlot.layoutManager = LinearLayoutManager(this)
        rvCurrentSlot.adapter = currentSlotAdapter

        // Next slot adapter
        nextSlotAdapter = SlotWiseFreeRoomsAdapter { viewModel.isLabsFilterActive() }
        rvNextSlot.layoutManager = LinearLayoutManager(this)
        rvNextSlot.adapter = nextSlotAdapter
    }

    private fun setupMainTabs() {
        // Set initial state
        updateMainTabAppearance(true)

        btnTabBySlots.setOnClickListener {
            if (!isSlotWiseViewActive) {
                isSlotWiseViewActive = true
                updateMainTabAppearance(true)
                showSlotWiseView()
            }
        }

        btnTabCurrentlyAvailable.setOnClickListener {
            if (isSlotWiseViewActive) {
                isSlotWiseViewActive = false
                updateMainTabAppearance(false)
                showCurrentlyAvailableView()
            }
        }
    }

    private fun updateMainTabAppearance(slotWiseActive: Boolean) {
        if (slotWiseActive) {
            btnTabBySlots.setBackgroundColor(ContextCompat.getColor(this, R.color.navy))
            btnTabBySlots.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            btnTabCurrentlyAvailable.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            btnTabCurrentlyAvailable.setTextColor(ContextCompat.getColor(this, R.color.navy))
        } else {
            btnTabCurrentlyAvailable.setBackgroundColor(ContextCompat.getColor(this, R.color.navy))
            btnTabCurrentlyAvailable.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            btnTabBySlots.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            btnTabBySlots.setTextColor(ContextCompat.getColor(this, R.color.navy))
        }
    }

    private fun showSlotWiseView() {
        rvSlotWise.visibility = View.VISIBLE
        scrollCurrentlyAvailable.visibility = View.GONE
        tabLayoutDays.visibility = View.VISIBLE
    }

    private fun showCurrentlyAvailableView() {
        rvSlotWise.visibility = View.GONE
        scrollCurrentlyAvailable.visibility = View.VISIBLE
        // Hide day tabs for currently available - it's always "today"
        tabLayoutDays.visibility = View.GONE

        // Reload for today's data
        initializeCurrentDay()
        viewModel.loadSlotWiseFreeRooms(currentSelectedDay)
    }

    private fun updateCurrentlyAvailableView(slots: List<SlotWithFreeRooms>) {
        val currentSlot = slots.filter { it.isCurrentSlot }
        val nextSlot = slots.filter { it.isNextSlot }

        if (currentSlot.isEmpty() && nextSlot.isEmpty()) {
            tvNoClassesNow.visibility = View.VISIBLE
            tvCurrentSlotHeader.visibility = View.GONE
            rvCurrentSlot.visibility = View.GONE
            tvNextSlotHeader.visibility = View.GONE
            rvNextSlot.visibility = View.GONE
        } else {
            tvNoClassesNow.visibility = View.GONE

            // Current slot
            if (currentSlot.isNotEmpty()) {
                tvCurrentSlotHeader.visibility = View.VISIBLE
                rvCurrentSlot.visibility = View.VISIBLE
                currentSlotAdapter.submitList(currentSlot)
            } else {
                tvCurrentSlotHeader.visibility = View.GONE
                rvCurrentSlot.visibility = View.GONE
            }

            // Next slot
            if (nextSlot.isNotEmpty()) {
                tvNextSlotHeader.visibility = View.VISIBLE
                rvNextSlot.visibility = View.VISIBLE
                nextSlotAdapter.submitList(nextSlot)
            } else {
                tvNextSlotHeader.visibility = View.GONE
                rvNextSlot.visibility = View.GONE
            }
        }
    }

    private fun updateEmptyState(slots: List<SlotWithFreeRooms>?) {
        if (isSlotWiseViewActive) {
            if (slots.isNullOrEmpty()) {
                tvEmptyState.visibility = View.VISIBLE
                rvSlotWise.visibility = View.GONE
            } else {
                tvEmptyState.visibility = View.GONE
                rvSlotWise.visibility = View.VISIBLE
            }
        }
    }

    private fun setupDayTabs() {
        // Add day tabs
        days.forEach { day ->
            tabLayoutDays.addTab(tabLayoutDays.newTab().setText(day))
        }

        // Select current day
        val currentDayIndex = days.indexOf(currentSelectedDay)
        if (currentDayIndex >= 0) {
            tabLayoutDays.getTabAt(currentDayIndex)?.select()
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

        rvSlotWise.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false // Allow RecyclerView to handle scrolling
        }

        // Tab selection listener
        tabLayoutDays.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    currentSelectedDay = days[it.position]
                    viewModel.loadSlotWiseFreeRooms(currentSelectedDay)
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
                    // Refresh the adapters to show updated filter
                    slotWiseAdapter.notifyDataSetChanged()
                    currentSlotAdapter.notifyDataSetChanged()
                    nextSlotAdapter.notifyDataSetChanged()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
}
