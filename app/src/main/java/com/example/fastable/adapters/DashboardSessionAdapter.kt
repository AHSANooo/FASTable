package com.example.fastable.adapters

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
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
        private val tvSection: TextView = itemView.findViewById(R.id.tvSection)
        private val tvSessionType: TextView = itemView.findViewById(R.id.tvSessionType)
        private val tvRoom: TextView = itemView.findViewById(R.id.tvRoom)
        private val tvTimeSlot: TextView = itemView.findViewById(R.id.tvTimeSlot)
        private val colorIndicator: View = itemView.findViewById(R.id.colorIndicator)
        private val btnMore: ImageButton = itemView.findViewById(R.id.btnMore)

        fun bind(session: DashboardSession) {
            val context = itemView.context
            val isCancelled = session.courseName.contains("Cancelled", ignoreCase = true)

            tvCourseName.text = session.courseName
            tvSessionType.text = if (isCancelled) "Cancelled" else session.sessionType
            tvRoom.text = session.room.ifEmpty { "TBA" }
            tvTimeSlot.text = session.timeSlot

            // Display Section badge if available
            val sectionDisplay = when {
                session.section.isNotEmpty() && session.batch.isNotEmpty() -> "${session.batch}-${session.section}"
                session.section.isNotEmpty() -> "Sec ${session.section}"
                session.batch.isNotEmpty() -> session.batch
                else -> ""
            }

            if (sectionDisplay.isNotEmpty()) {
                tvSection.text = sectionDisplay
                tvSection.visibility = View.VISIBLE
            } else {
                tvSection.visibility = View.GONE
            }

            // Apply cancelled styling
            if (isCancelled) {
                tvCourseName.paintFlags = tvCourseName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                colorIndicator.setBackgroundColor(context.getColor(R.color.cancelled_color))
                tvSessionType.setBackgroundColor(context.getColor(R.color.cancelled_color))
            } else {
                tvCourseName.paintFlags = tvCourseName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

                // Session type badge color
                if (session.sessionType.equals("Lab", ignoreCase = true)) {
                    tvSessionType.setBackgroundColor(context.getColor(R.color.lab_color))
                } else {
                    tvSessionType.setBackgroundColor(context.getColor(R.color.class_color))
                }

                // Color indicator bar
                if (session.colorCode.isNotEmpty() && session.colorCode != "1.001.001.00") {
                    try {
                        val colorStr = session.colorCode
                        if (colorStr.length >= 12) {
                            val r = (colorStr.substring(0, 4).toFloat() * 255).toInt()
                            val g = (colorStr.substring(4, 8).toFloat() * 255).toInt()
                            val b = (colorStr.substring(8, 12).toFloat() * 255).toInt()
                            colorIndicator.setBackgroundColor(Color.rgb(r, g, b))
                        } else {
                            colorIndicator.setBackgroundColor(context.getColor(R.color.primary_orange))
                        }
                    } catch (e: Exception) {
                        colorIndicator.setBackgroundColor(context.getColor(R.color.primary_orange))
                    }
                } else {
                    colorIndicator.setBackgroundColor(context.getColor(R.color.primary_orange))
                }
            }

            // 3-dot overflow menu
            btnMore.setOnClickListener { view ->
                val popup = PopupMenu(context, view)
                popup.menu.add(0, 1, 0, "Remove from Dashboard")
                popup.setOnMenuItemClickListener { menuItem ->
                    if (menuItem.itemId == 1) {
                        onLongClick(session)
                        true
                    } else false
                }
                popup.show()
            }

            // Long click to remove
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
