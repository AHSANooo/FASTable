package com.example.fastable.adapters

import android.graphics.Color
import android.graphics.Paint
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
            // Check if class is cancelled
            val isCancelled = session.courseName.contains("Cancelled", ignoreCase = true)

            tvCourseName.text = session.courseName
            tvSessionType.text = if (isCancelled) "Cancelled" else session.sessionType
            tvRoom.text = session.room
            tvTimeSlot.text = session.timeSlot

            // Apply cancelled styling
            if (isCancelled) {
                // Strikethrough effect on course name
                tvCourseName.paintFlags = tvCourseName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                tvCourseName.setTextColor(Color.parseColor("#999999"))

                // Gray out other text
                tvRoom.setTextColor(Color.parseColor("#AAAAAA"))
                tvTimeSlot.setTextColor(Color.parseColor("#AAAAAA"))

                // Red indicator for cancelled
                colorIndicator.setBackgroundColor(Color.parseColor("#FF4444"))

                // Red background for session type badge
                tvSessionType.setBackgroundColor(Color.parseColor("#FF4444"))
            } else {
                // Reset to normal styling
                tvCourseName.paintFlags = tvCourseName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                tvCourseName.setTextColor(Color.parseColor("#0D2A5C"))
                tvRoom.setTextColor(Color.parseColor("#666666"))
                tvTimeSlot.setTextColor(Color.parseColor("#666666"))

                // Set session type background based on type
                tvSessionType.setBackgroundResource(R.drawable.session_type_bg)

                // Set color on the indicator bar - convert from Google Sheets RGB format to Android Color
                if (session.colorCode.isNotEmpty() && session.colorCode != "1.001.001.00") {
                    try {
                        val colorStr = session.colorCode
                        if (colorStr.length >= 12) {
                            val r = (colorStr.substring(0, 4).toFloat() * 255).toInt()
                            val g = (colorStr.substring(4, 8).toFloat() * 255).toInt()
                            val b = (colorStr.substring(8, 12).toFloat() * 255).toInt()
                            colorIndicator.setBackgroundColor(Color.rgb(r, g, b))
                        } else {
                            colorIndicator.setBackgroundColor(Color.parseColor("#0D2A5C"))
                        }
                    } catch (e: Exception) {
                        colorIndicator.setBackgroundColor(Color.parseColor("#0D2A5C"))
                    }
                } else {
                    colorIndicator.setBackgroundColor(Color.parseColor("#0D2A5C"))
                }
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

