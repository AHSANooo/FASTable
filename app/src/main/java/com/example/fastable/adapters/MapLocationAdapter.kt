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
import com.example.fastable.data.models.MapLocation

class MapLocationAdapter : ListAdapter<MapLocation, MapLocationAdapter.MapLocationViewHolder>(MapLocationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapLocationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_map_location, parent, false)
        return MapLocationViewHolder(view)
    }

    override fun onBindViewHolder(holder: MapLocationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MapLocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewColorIndicator: View = itemView.findViewById(R.id.viewColorIndicator)
        private val tvRoomName: TextView = itemView.findViewById(R.id.tvRoomName)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)

        fun bind(location: MapLocation) {
            tvRoomName.text = location.roomName
            tvLocation.text = location.location
            viewColorIndicator.setBackgroundColor(
                ContextCompat.getColor(itemView.context, location.colorCode)
            )
        }
    }

    class MapLocationDiffCallback : DiffUtil.ItemCallback<MapLocation>() {
        override fun areItemsTheSame(oldItem: MapLocation, newItem: MapLocation): Boolean {
            return oldItem.roomName == newItem.roomName
        }

        override fun areContentsTheSame(oldItem: MapLocation, newItem: MapLocation): Boolean {
            return oldItem == newItem
        }
    }
}

