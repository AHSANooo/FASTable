package com.example.fastable

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fastable.adapters.FloorRoomAdapter
import com.example.fastable.data.local.AppDatabase
import com.example.fastable.data.models.DashboardSession
import com.example.fastable.data.models.RoomOccupancyState
import com.example.fastable.data.models.RoomStatusInfo
import com.example.fastable.data.repository.CampusFloorRepository
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Deliverable D3 - Concept 4 (R3):
 * Live Campus Floor Plan Map for Block A, Block B, and Block C.
 *
 * Shows interactive floor layouts, live Free/Occupied room occupancy,
 * and highlights "★ Your Room" for classes matching the student's schedule.
 */
class CampusMap : AppCompatActivity() {

    private lateinit var toggleBlockGroup: MaterialButtonToggleGroup
    private lateinit var chipGroupFloors: ChipGroup
    private lateinit var rvFloorRooms: RecyclerView
    private lateinit var adapter: FloorRoomAdapter

    private var selectedBlock: String = "Block C"
    private var selectedFloor: String = "3rd Floor"
    private var userSessions: List<DashboardSession> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_campus_map)

        setupToolbar()
        initViews()
        setupRecyclerView()
        setupFloorChipsForBlock(selectedBlock)
        loadUserSessions()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Live Campus Floor Map"
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initViews() {
        toggleBlockGroup = findViewById(R.id.toggleBlockGroup)
        chipGroupFloors = findViewById(R.id.chipGroupFloors)
        rvFloorRooms = findViewById(R.id.rvFloorRooms)

        toggleBlockGroup.check(R.id.btnBlockC)
        selectedBlock = "Block C"

        toggleBlockGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedBlock = when (checkedId) {
                    R.id.btnBlockA -> "Block A"
                    R.id.btnBlockB -> "Block B"
                    R.id.btnBlockC -> "Block C"
                    else -> "Block C"
                }
                setupFloorChipsForBlock(selectedBlock)
            }
        }
    }

    private fun setupFloorChipsForBlock(block: String) {
        chipGroupFloors.removeAllViews()
        val floors = CampusFloorRepository.getFloorsForBlock(block)

        // Reset floor to default on block change
        selectedFloor = if (block == "Block C") "3rd Floor" else floors.firstOrNull() ?: "Ground Floor"

        floors.forEach { floorName ->
            val chip = Chip(this).apply {
                text = floorName
                isCheckable = true
                isClickable = true
                isChecked = (floorName == selectedFloor)
                chipCornerRadius = 20f
                setChipBackgroundColorResource(
                    if (isChecked) R.color.badge_nuces_bg else android.R.color.transparent
                )
                chipStrokeWidth = 2f
                chipStrokeColor = ColorStateList.valueOf(
                    if (isChecked) Color.parseColor("#FF6D05") else Color.parseColor("#CBD5E1")
                )
                setTextColor(
                    if (isChecked) Color.parseColor("#D85800") else Color.parseColor("#64748B")
                )

                setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        selectedFloor = floorName
                        updateChipStyles()
                        renderFloorRooms()
                    }
                }
            }
            chipGroupFloors.addView(chip)
        }

        renderFloorRooms()
    }

    private fun updateChipStyles() {
        for (i in 0 until chipGroupFloors.childCount) {
            val chip = chipGroupFloors.getChildAt(i) as? Chip ?: continue
            val isChecked = chip.isChecked
            chip.chipStrokeColor = ColorStateList.valueOf(
                if (isChecked) Color.parseColor("#FF6D05") else Color.parseColor("#CBD5E1")
            )
            chip.setTextColor(
                if (isChecked) Color.parseColor("#D85800") else Color.parseColor("#64748B")
            )
        }
    }

    private fun setupRecyclerView() {
        adapter = FloorRoomAdapter { roomStatus ->
            showRoomDetailsBottomSheet(roomStatus)
        }
        rvFloorRooms.layoutManager = LinearLayoutManager(this)
        rvFloorRooms.adapter = adapter
    }

    private fun loadUserSessions() {
        lifecycleScope.launch {
            val sessions = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@CampusMap)
                    .dashboardDao()
                    .getAllDashboardSessions()
                    .firstOrNull() ?: emptyList()
            }
            userSessions = sessions
            renderFloorRooms()
        }
    }

    private fun renderFloorRooms() {
        if (!::rvFloorRooms.isInitialized || !::adapter.isInitialized) return
        val cal = Calendar.getInstance()
        val currentDay = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            else -> "Monday"
        }
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val rooms = CampusFloorRepository.getRoomsForFloor(selectedBlock, selectedFloor)
        val roomStatuses = rooms.map { room ->
            CampusFloorRepository.evaluateRoomStatus(
                room = room,
                userSessions = userSessions,
                allCampusSessions = emptyList(),
                currentDay = currentDay,
                currentMinutes = currentMinutes
            )
        }

        adapter.submitList(roomStatuses)
    }

    private fun showRoomDetailsBottomSheet(status: RoomStatusInfo) {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_room_details, null)
        dialog.setContentView(view)

        val room = status.room
        val bsRoomTitle = view.findViewById<TextView>(R.id.bsRoomTitle)
        val bsRoomSubtitle = view.findViewById<TextView>(R.id.bsRoomSubtitle)
        val bsStatusBadge = view.findViewById<TextView>(R.id.bsStatusBadge)
        val bsBannerUserRoom = view.findViewById<View>(R.id.bsBannerUserRoom)
        val bsUserRoomInfo = view.findViewById<TextView>(R.id.bsUserRoomInfo)
        val bsLectureName = view.findViewById<TextView>(R.id.bsLectureName)
        val bsTimeSlot = view.findViewById<TextView>(R.id.bsTimeSlot)
        val bsAdditionalInfo = view.findViewById<TextView>(R.id.bsAdditionalInfo)
        val bsBtnClose = view.findViewById<MaterialButton>(R.id.bsBtnClose)

        bsRoomTitle.text = room.displayName
        bsRoomSubtitle.text = "${room.block} • ${room.floor} • ${room.wing}"
        bsAdditionalInfo.text = "Capacity: ${room.capacity} seats • Category: ${room.type.name}"

        when (status.state) {
            RoomOccupancyState.FREE -> {
                bsStatusBadge.text = "FREE FOR STUDY"
                bsStatusBadge.setTextColor(Color.parseColor("#10B981"))
                bsBannerUserRoom.visibility = View.GONE
                bsLectureName.text = "Vacant Study Space"
                bsTimeSlot.text = "No lecture scheduled • Ideal for quiet study"
            }
            RoomOccupancyState.OCCUPIED -> {
                bsStatusBadge.text = "OCCUPIED"
                bsStatusBadge.setTextColor(Color.parseColor("#EF4444"))
                bsBannerUserRoom.visibility = View.GONE
                bsLectureName.text = status.currentLecture ?: "Class in session"
                bsTimeSlot.text = "Time slot: ${status.timeSlot ?: "Ongoing"}"
            }
            RoomOccupancyState.STUDENT_CLASS -> {
                bsStatusBadge.text = "★ YOUR CLASS"
                bsStatusBadge.setTextColor(Color.parseColor("#C04600"))
                bsBannerUserRoom.visibility = View.VISIBLE
                bsUserRoomInfo.text = "${status.currentLecture} (${status.timeSlot})"
                bsLectureName.text = status.currentLecture ?: "Your enrolled class"
                bsTimeSlot.text = "Scheduled time: ${status.timeSlot ?: ""}"
            }
        }

        bsBtnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
