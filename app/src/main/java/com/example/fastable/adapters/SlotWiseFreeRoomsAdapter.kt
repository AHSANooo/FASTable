package com.example.fastable.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fastable.R
import com.example.fastable.data.models.SlotWithFreeRooms
import com.google.android.material.card.MaterialCardView

/**
 * Adapter for displaying time slots with their free rooms
 */
class SlotWiseFreeRoomsAdapter(
    private val showLabs: () -> Boolean
) : ListAdapter<SlotWithFreeRooms, SlotWiseFreeRoomsAdapter.SlotViewHolder>(SlotDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_slot_free_rooms, parent, false)
        return SlotViewHolder(view, showLabs)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SlotViewHolder(
        itemView: View,
        private val showLabs: () -> Boolean
    ) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardSlot)
        private val tvTimeSlot: TextView = itemView.findViewById(R.id.tvTimeSlot)
        private val tvSlotStatus: TextView = itemView.findViewById(R.id.tvSlotStatus)
        private val tvRoomCount: TextView = itemView.findViewById(R.id.tvRoomCount)
        private val tvRoomsList: TextView = itemView.findViewById(R.id.tvRoomsList)

        fun bind(slot: SlotWithFreeRooms) {
            tvTimeSlot.text = slot.timeSlot

            // Show status for current/next slot
            when {
                slot.isCurrentSlot -> {
                    tvSlotStatus.visibility = View.VISIBLE
                    tvSlotStatus.text = "NOW"
                    tvSlotStatus.setBackgroundResource(R.drawable.bg_current_slot)
                    cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.green_primary)
                    cardView.strokeWidth = 2
                }
                slot.isNextSlot -> {
                    tvSlotStatus.visibility = View.VISIBLE
                    tvSlotStatus.text = "NEXT"
                    tvSlotStatus.setBackgroundResource(R.drawable.bg_next_slot)
                    cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.orange_primary)
                    cardView.strokeWidth = 2
                }
                else -> {
                    tvSlotStatus.visibility = View.GONE
                    cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.divider_color)
                    cardView.strokeWidth = 1
                }
            }

            // Show rooms or labs based on filter
            val rooms = if (showLabs()) slot.freeLabs else slot.freeRooms
            val typeLabel = if (showLabs()) "Labs" else "Rooms"
            
            tvRoomCount.text = "${rooms.size} Free $typeLabel"
            
            if (rooms.isEmpty()) {
                tvRoomsList.text = "All $typeLabel occupied"
                tvRoomsList.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
            } else {
                // Display rooms in a compact grid format
                tvRoomsList.text = rooms.joinToString("  •  ")
                tvRoomsList.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
            }
        }
    }

    class SlotDiffCallback : DiffUtil.ItemCallback<SlotWithFreeRooms>() {
        override fun areItemsTheSame(oldItem: SlotWithFreeRooms, newItem: SlotWithFreeRooms): Boolean {
            return oldItem.timeSlot == newItem.timeSlot
        }

        override fun areContentsTheSame(oldItem: SlotWithFreeRooms, newItem: SlotWithFreeRooms): Boolean {
            return oldItem == newItem
        }
    }
}

