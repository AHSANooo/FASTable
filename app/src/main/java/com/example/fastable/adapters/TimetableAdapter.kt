package com.example.fastable.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fastable.R
import com.example.fastable.data.models.TimetableSession

class TimetableAdapter : ListAdapter<TimetableSession, TimetableAdapter.TimetableViewHolder>(TimetableDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimetableViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_timetable_session, parent, false)
        return TimetableViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimetableViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TimetableViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timeSlot: TextView = itemView.findViewById(R.id.tvTimeSlot)
        private val courseName: TextView = itemView.findViewById(R.id.tvCourseName)
        private val room: TextView = itemView.findViewById(R.id.tvRoom)
        private val sessionType: TextView = itemView.findViewById(R.id.tvSessionType)

        fun bind(session: TimetableSession) {
            timeSlot.text = session.timeSlot
            courseName.text = session.courseName
            room.text = "Room: ${session.room}"
            sessionType.text = session.sessionType

            // Set color based on session type
            val context = itemView.context
            if (session.sessionType == "Lab") {
                sessionType.setBackgroundColor(context.getColor(R.color.lab_color))
            } else {
                sessionType.setBackgroundColor(context.getColor(R.color.class_color))
            }
        }
    }

    class TimetableDiffCallback : DiffUtil.ItemCallback<TimetableSession>() {
        override fun areItemsTheSame(oldItem: TimetableSession, newItem: TimetableSession): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TimetableSession, newItem: TimetableSession): Boolean {
            return oldItem == newItem
        }
    }
}

