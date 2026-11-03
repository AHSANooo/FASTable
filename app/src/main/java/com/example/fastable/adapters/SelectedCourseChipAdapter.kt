package com.example.fastable.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fastable.R
import com.example.fastable.data.models.Course

class SelectedCourseChipAdapter(
    private val onRemove: (Course) -> Unit
) : ListAdapter<Course, SelectedCourseChipAdapter.ChipViewHolder>(ChipDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_chip, parent, false)
        return ChipViewHolder(view, onRemove)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChipViewHolder(
        itemView: View,
        private val onRemove: (Course) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvChipTitle: TextView = itemView.findViewById(R.id.tvChipTitle)
        private val btnRemoveChip: ImageButton = itemView.findViewById(R.id.btnRemoveChip)

        fun bind(course: Course) {
            val shortName = if (course.name.length > 20) course.name.substring(0, 18) + "…" else course.name
            tvChipTitle.text = "$shortName (${course.section})"
            btnRemoveChip.setOnClickListener {
                onRemove(course)
            }
        }
    }

    private class ChipDiffCallback : DiffUtil.ItemCallback<Course>() {
        override fun areItemsTheSame(oldItem: Course, newItem: Course): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Course, newItem: Course): Boolean {
            return oldItem == newItem
        }
    }
}

