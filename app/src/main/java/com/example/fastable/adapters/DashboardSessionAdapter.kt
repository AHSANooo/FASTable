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
import com.example.fastable.data.models.DashboardSession

class DashboardSessionAdapter(
    private val onLongClick: (DashboardSession) -> Unit
) : ListAdapter<DashboardSession, DashboardSessionAdapter.SessionViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course_card, parent, false)
        return SessionViewHolder(view, onLongClick)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SessionViewHolder(
        itemView: View,
        private val onLongClick: (DashboardSession) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvCourseName: TextView = itemView.findViewById(R.id.tvCourseName)
        private val tvSessionType: TextView = itemView.findViewById(R.id.tvSessionType)
        private val tvRoom: TextView = itemView.findViewById(R.id.tvRoom)
        private val tvTimeSlot: TextView = itemView.findViewById(R.id.tvTimeSlot)
        private val colorIndicator: View = itemView.findViewById(R.id.colorIndicator)

        fun bind(session: DashboardSession) {
            tvCourseName.text = session.courseName
            tvSessionType.text = session.sessionType
            tvRoom.text = session.room
            tvTimeSlot.text = session.timeSlot

            // Set color on the indicator bar - convert from Google Sheets RGB format to Android Color
            // Color format from Sheets is like "0.900.900.90" (concatenated "%.2f%.2f%.2f")
            if (session.colorCode.isNotEmpty() && session.colorCode != "1.001.001.00") {
                try {
                    // Parse color code - it's a concatenated string of 3 floats with 2 decimal places each
                    // Format: "0.900.900.90" means R=0.90, G=0.90, B=0.90
                    val colorStr = session.colorCode
                    if (colorStr.length >= 12) {
                        val r = (colorStr.substring(0, 4).toFloat() * 255).toInt()
                        val g = (colorStr.substring(4, 8).toFloat() * 255).toInt()
                        val b = (colorStr.substring(8, 12).toFloat() * 255).toInt()
                        colorIndicator.setBackgroundColor(Color.rgb(r, g, b))
                    } else {
                        // Fallback to default color
                        colorIndicator.setBackgroundColor(Color.parseColor("#0D2A5C"))
                    }
                } catch (e: Exception) {
                    // Use default color
                    colorIndicator.setBackgroundColor(Color.parseColor("#0D2A5C"))
                }
            } else {
                colorIndicator.setBackgroundColor(Color.parseColor("#0D2A5C"))
            }

            // Long click to delete
            itemView.setOnLongClickListener {
                onLongClick(session)
                true
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<DashboardSession>() {
        override fun areItemsTheSame(oldItem: DashboardSession, newItem: DashboardSession): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DashboardSession, newItem: DashboardSession): Boolean {
            return oldItem == newItem
        }
    }
}

