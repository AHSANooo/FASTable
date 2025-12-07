package com.example.fastable.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fastable.R
import com.example.fastable.data.model.FacultyMember

class FacultyOfficesAdapter(
    private var facultyList: List<FacultyMember>
) : RecyclerView.Adapter<FacultyOfficesAdapter.FacultyViewHolder>() {

    private var filteredList: List<FacultyMember> = facultyList

    inner class FacultyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.facultyName)
        val designation: TextView = itemView.findViewById(R.id.facultyDesignation)
        val email: TextView = itemView.findViewById(R.id.facultyEmail)
        val office: TextView = itemView.findViewById(R.id.facultyOffice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FacultyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_faculty_office, parent, false)
        return FacultyViewHolder(view)
    }

    override fun onBindViewHolder(holder: FacultyViewHolder, position: Int) {
        val faculty = filteredList[position]
        holder.name.text = faculty.name
        holder.designation.text = faculty.designation
        holder.email.text = faculty.email
        holder.office.text = faculty.office
    }

    override fun getItemCount(): Int = filteredList.size

    fun filter(query: String) {
        filteredList = if (query.isEmpty()) {
            facultyList
        } else {
            facultyList.filter { faculty ->
                faculty.name.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    fun updateData(newList: List<FacultyMember>) {
        facultyList = newList
        filteredList = newList
        notifyDataSetChanged()
    }
}

