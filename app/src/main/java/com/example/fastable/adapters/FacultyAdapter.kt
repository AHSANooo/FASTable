package com.example.fastable.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fastable.R
import com.example.fastable.data.models.FacultyMember

class FacultyAdapter : ListAdapter<FacultyMember, FacultyAdapter.FacultyViewHolder>(FacultyDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FacultyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_faculty, parent, false)
        return FacultyViewHolder(view)
    }

    override fun onBindViewHolder(holder: FacultyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FacultyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvFacultyName)
        private val tvDesignation: TextView = itemView.findViewById(R.id.tvDesignation)
        private val tvOffice: TextView = itemView.findViewById(R.id.tvOffice)
        private val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)

        fun bind(faculty: FacultyMember) {
            tvName.text = faculty.name
            tvDesignation.text = faculty.designation
            tvOffice.text = "Office: ${faculty.office}"
            tvEmail.text = faculty.email

            // Make email clickable
            tvEmail.setOnClickListener {
                if (faculty.email.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:${faculty.email}")
                    }
                    itemView.context.startActivity(intent)
                }
            }
        }
    }

    private class FacultyDiffCallback : DiffUtil.ItemCallback<FacultyMember>() {
        override fun areItemsTheSame(oldItem: FacultyMember, newItem: FacultyMember): Boolean {
            return oldItem.email == newItem.email
        }

        override fun areContentsTheSame(oldItem: FacultyMember, newItem: FacultyMember): Boolean {
            return oldItem == newItem
        }
    }
}

