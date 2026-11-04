package com.example.fastable.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fastable.R
import com.example.fastable.data.models.RoomOccupancyState
import com.example.fastable.data.models.RoomStatusInfo
import com.google.android.material.card.MaterialCardView

class FloorRoomAdapter(
    private val onRoomClick: (RoomStatusInfo) -> Unit
) : ListAdapter<RoomStatusInfo, FloorRoomAdapter.RoomViewHolder>(RoomDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_floor_room_card, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.bind(getItem(position), onRoomClick)
    }

    class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardFloorRoom: MaterialCardView = itemView.findViewById(R.id.cardFloorRoom)
        private val tvRoomName: TextView = itemView.findViewById(R.id.tvRoomName)
        private val tvStatusBadge: TextView = itemView.findViewById(R.id.tvStatusBadge)
        private val dotStatus: View = itemView.findViewById(R.id.dotStatus)
        private val tvWingAndType: TextView = itemView.findViewById(R.id.tvWingAndType)
        private val tvCapacity: TextView = itemView.findViewById(R.id.tvCapacity)
        private val tvCurrentLectureInfo: TextView = itemView.findViewById(R.id.tvCurrentLectureInfo)
        private val bannerUserClass: View = itemView.findViewById(R.id.bannerUserClass)
        private val tvUserClassDetail: TextView = itemView.findViewById(R.id.tvUserClassDetail)

        fun bind(status: RoomStatusInfo, onRoomClick: (RoomStatusInfo) -> Unit) {
            val room = status.room
            tvRoomName.text = room.displayName
            tvWingAndType.text = "${room.type.name.lowercase().replaceFirstChar { it.uppercase() }} • ${room.wing}"
            tvCapacity.text = "${room.capacity} seats"

            when (status.state) {
                RoomOccupancyState.FREE -> {
                    tvStatusBadge.text = "Free for Study"
                    tvStatusBadge.setTextColor(Color.parseColor("#10B981"))
                    dotStatus.setBackgroundResource(R.drawable.bg_dot_green)
                    tvCurrentLectureInfo.text = "Vacant study spot • Quiet area"
                    bannerUserClass.visibility = View.GONE
                    cardFloorRoom.strokeWidth = 2
                    cardFloorRoom.strokeColor = Color.parseColor("#E2E8F0")
                }
                RoomOccupancyState.OCCUPIED -> {
                    tvStatusBadge.text = "Occupied"
                    tvStatusBadge.setTextColor(Color.parseColor("#EF4444"))
                    dotStatus.setBackgroundResource(R.drawable.bg_dot_red)
                    tvCurrentLectureInfo.text = "${status.currentLecture ?: "Class"} • ${status.timeSlot ?: ""}"
                    bannerUserClass.visibility = View.GONE
                    cardFloorRoom.strokeWidth = 2
                    cardFloorRoom.strokeColor = Color.parseColor("#E2E8F0")
                }
                RoomOccupancyState.STUDENT_CLASS -> {
                    tvStatusBadge.text = "★ Your Room"
                    tvStatusBadge.setTextColor(Color.parseColor("#C04600"))
                    dotStatus.setBackgroundResource(R.drawable.bg_dot_blue)
                    tvCurrentLectureInfo.text = "${status.currentLecture} (${status.timeSlot})"
                    bannerUserClass.visibility = View.VISIBLE
                    tvUserClassDetail.text = "★ YOUR CLASS: ${status.currentLecture} (${status.timeSlot})"
                    cardFloorRoom.strokeWidth = 4
                    cardFloorRoom.strokeColor = Color.parseColor("#FF6D05")
                }
            }

            cardFloorRoom.setOnClickListener {
                onRoomClick(status)
            }
        }
    }

    class RoomDiffCallback : DiffUtil.ItemCallback<RoomStatusInfo>() {
        override fun areItemsTheSame(oldItem: RoomStatusInfo, newItem: RoomStatusInfo): Boolean {
            return oldItem.room.id == newItem.room.id
        }

        override fun areContentsTheSame(oldItem: RoomStatusInfo, newItem: RoomStatusInfo): Boolean {
            return oldItem == newItem
        }
    }
}
