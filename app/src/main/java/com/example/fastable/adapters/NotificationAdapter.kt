package com.example.fastable.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fastable.R
import com.example.fastable.data.models.NotificationItem
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val onClick: (NotificationItem) -> Unit
) : ListAdapter<NotificationItem, NotificationAdapter.NotificationViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationViewHolder(
        itemView: View,
        private val onClick: (NotificationItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvTitle: TextView = itemView.findViewById(R.id.tvNotificationTitle)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvNotificationMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvNotificationTime)
        private val tvCourseName: TextView = itemView.findViewById(R.id.tvCourseName)
        private val tvRoom: TextView = itemView.findViewById(R.id.tvRoom)
        private val viewReadIndicator: View = itemView.findViewById(R.id.viewReadIndicator)

        fun bind(notification: NotificationItem) {
            tvTitle.text = notification.title
            tvMessage.text = notification.message
            tvCourseName.text = notification.courseName
            tvRoom.text = notification.room
            tvTime.text = formatTime(notification.timestamp)

            // Show/hide read indicator
            viewReadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE

            // Set background color for unread notifications
            itemView.setBackgroundResource(
                if (notification.isRead) R.color.white else R.color.notification_unread
            )

            itemView.setOnClickListener {
                onClick(notification)
            }
        }

        private fun formatTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000}m ago"
                diff < 86400_000 -> "${diff / 3600_000}h ago"
                diff < 172800_000 -> "Yesterday"
                else -> {
                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<NotificationItem>() {
        override fun areItemsTheSame(oldItem: NotificationItem, newItem: NotificationItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NotificationItem, newItem: NotificationItem): Boolean {
            return oldItem == newItem
        }
    }
}

