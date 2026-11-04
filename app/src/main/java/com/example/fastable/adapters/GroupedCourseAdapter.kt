package com.example.fastable.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fastable.R
import com.example.fastable.data.models.Course

data class CourseGroup(
    val courseName: String,
    val department: String,
    val sections: List<Course>,
    var selectedSectionPosition: Int = 0
)

class GroupedCourseAdapter(
    private val onToggleSelection: (Course) -> Unit
) : ListAdapter<CourseGroup, GroupedCourseAdapter.GroupViewHolder>(GroupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course_group, parent, false)
        return GroupViewHolder(view, onToggleSelection)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class GroupViewHolder(
        itemView: View,
        private val onToggleSelection: (Course) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvCourseName: TextView = itemView.findViewById(R.id.tvCourseName)
        private val tvCourseDept: TextView = itemView.findViewById(R.id.tvCourseDept)
        private val cbCourseSelect: CheckBox = itemView.findViewById(R.id.cbCourseSelect)
        private val spinnerSection: Spinner = itemView.findViewById(R.id.spinnerSection)

        fun bind(group: CourseGroup) {
            tvCourseName.text = group.courseName
            tvCourseDept.text = if (group.department.isNotBlank()) group.department else "GENERAL"

            val sectionNames = group.sections.map {
                val sec = if (it.section.isNotBlank()) "Section ${it.section}" else "Default Section"
                val yr = it.getYear()
                if (yr.isNotBlank() && yr != it.section) "$sec ($yr)" else sec
            }

            val spinnerAdapter = ArrayAdapter(
                itemView.context,
                android.R.layout.simple_spinner_item,
                sectionNames
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            spinnerSection.adapter = spinnerAdapter

            val selectedIndex = group.sections.indexOfFirst { it.isSelected }
            val activePos = if (selectedIndex >= 0) selectedIndex else group.selectedSectionPosition
            val clampedPos = activePos.coerceIn(0, (group.sections.size - 1).coerceAtLeast(0))
            group.selectedSectionPosition = clampedPos
            spinnerSection.setSelection(clampedPos)

            val currentlySelectedCourse = group.sections.getOrNull(clampedPos)
            cbCourseSelect.isChecked = currentlySelectedCourse?.isSelected == true

            spinnerSection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    group.selectedSectionPosition = position
                    val activeCourse = group.sections.getOrNull(position)
                    cbCourseSelect.isChecked = activeCourse?.isSelected == true
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            cbCourseSelect.setOnClickListener {
                val activeCourse = group.sections.getOrNull(group.selectedSectionPosition)
                if (activeCourse != null) {
                    onToggleSelection(activeCourse)
                }
            }

            itemView.setOnClickListener {
                val activeCourse = group.sections.getOrNull(group.selectedSectionPosition)
                if (activeCourse != null) {
                    onToggleSelection(activeCourse)
                }
            }
        }
    }

    class GroupDiffCallback : DiffUtil.ItemCallback<CourseGroup>() {
        override fun areItemsTheSame(oldItem: CourseGroup, newItem: CourseGroup): Boolean {
            return oldItem.courseName == newItem.courseName && oldItem.department == newItem.department
        }

        override fun areContentsTheSame(oldItem: CourseGroup, newItem: CourseGroup): Boolean {
            return oldItem == newItem
        }
    }
}

