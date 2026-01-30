package com.example.fastable.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fastable.R
import com.example.fastable.data.models.FreeRoom

class FreeRoomAdapter : ListAdapter<FreeRoom, FreeRoomAdapter.FreeRoomViewHolder>(FreeRoomDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FreeRoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_free_room, parent, false)
        return FreeRoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: FreeRoomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FreeRoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRoomName: TextView = itemView.findViewById(R.id.tvRoomName)
        private val tvFreeSlots: TextView = itemView.findViewById(R.id.tvFreeSlots)

        fun bind(freeRoom: FreeRoom) {
            tvRoomName.text = freeRoom.roomName
            tvFreeSlots.text = freeRoom.getFreeSlotsSimple()
        }
    }

    class FreeRoomDiffCallback : DiffUtil.ItemCallback<FreeRoom>() {
        override fun areItemsTheSame(oldItem: FreeRoom, newItem: FreeRoom): Boolean {
            return oldItem.roomName == newItem.roomName
        }

        override fun areContentsTheSame(oldItem: FreeRoom, newItem: FreeRoom): Boolean {
            return oldItem == newItem
        }
    }
}

