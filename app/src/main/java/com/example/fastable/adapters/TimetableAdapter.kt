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
        private val tvSessionTag: TextView = itemView.findViewById(R.id.tvSessionTag)
        private val tvSection: TextView = itemView.findViewById(R.id.tvSection)

        fun bind(session: TimetableSession) {
            val context = itemView.context
            timeSlot.text = session.timeSlot
            courseName.text = session.courseName
            room.text = if (session.room.isNotEmpty()) "Room: ${session.room}" else "Room: TBA"
            sessionType.text = session.sessionType

            // Set color based on session type
            if (session.sessionType.equals("Lab", ignoreCase = true)) {
                sessionType.setBackgroundColor(context.getColor(R.color.lab_color))
            } else {
                sessionType.setBackgroundColor(context.getColor(R.color.secondary_navy))
            }

            // Preview tag (Existing vs New)
            if (session.previewTag.isNotEmpty()) {
                tvSessionTag.text = session.previewTag
                tvSessionTag.visibility = View.VISIBLE
                if (session.previewTag.equals("Existing", ignoreCase = true)) {
                    tvSessionTag.setBackgroundColor(context.getColor(R.color.secondary_navy))
                } else {
                    tvSessionTag.setBackgroundColor(context.getColor(R.color.primary_orange))
                }
            } else {
                tvSessionTag.visibility = View.GONE
            }

            // Section display
            if (session.section.isNotEmpty()) {
                tvSection.text = "Sec ${session.section}"
                tvSection.visibility = View.VISIBLE
            } else {
                tvSection.visibility = View.GONE
            }
        }
    }

    class TimetableDiffCallback : DiffUtil.ItemCallback<TimetableSession>() {
        override fun areItemsTheSame(oldItem: TimetableSession, newItem: TimetableSession): Boolean {
            return oldItem.id == newItem.id && oldItem.timeSlot == newItem.timeSlot && oldItem.day == newItem.day
        }

        override fun areContentsTheSame(oldItem: TimetableSession, newItem: TimetableSession): Boolean {
            return oldItem == newItem && oldItem.previewTag == newItem.previewTag
        }
    }
}
