package com.example.fastable

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.example.fastable.adapters.FacultyAdapter
import com.example.fastable.data.models.FacultyMember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.Sheet

class FacultyOffices : AppCompatActivity() {

    private lateinit var adapter: FacultyAdapter
    private var facultyList = listOf<FacultyMember>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faculty_offices)

        setupToolbar()
        setupRecyclerView()
        setupSearchBar()
        loadFacultyData()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Faculty Offices"
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.rvFaculty)
        adapter = FacultyAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupSearchBar() {
        val searchBar = findViewById<TextInputEditText>(R.id.etSearchFaculty)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterFaculty(s.toString())
            }
        })
    }

    private fun loadFacultyData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Try to open from assets folder
                val inputStream: InputStream = try {
                    assets.open("Faculty Offices-School of Computing.xlsx")
                } catch (e: Exception) {
                    android.util.Log.e("FacultyOffices", "File not found in assets folder: ${e.message}")
                    throw Exception("Excel file not found. Please ensure 'Faculty Offices-School of Computing.xlsx' is in the assets folder.", e)
                }

                // Using Apache POI to read Excel file
                val workbook = WorkbookFactory.create(inputStream)
                val sheet: Sheet = workbook.getSheetAt(0)

                val facultyMembers = mutableListOf<FacultyMember>()

                for (i in 0..sheet.lastRowNum) {
                    val row = sheet.getRow(i) ?: continue

                    // Read columns
                    val colA = row.getCell(0)?.toString()?.replace("\t", " ")?.replace("\n", " ")?.trim() ?: ""
                    val colB = row.getCell(1)?.toString()?.replace("\t", " ")?.replace("\n", " ")?.trim() ?: ""
                    val designation = row.getCell(2)?.toString()?.replace("\t", " ")?.replace("\n", " ")?.trim() ?: ""
                    val email = row.getCell(3)?.toString()?.replace("\t", " ")?.replace("\n", " ")?.trim() ?: ""
                    val office = row.getCell(4)?.toString()?.replace("\t", " ")?.replace("\n", " ")?.trim() ?: ""

                    // Determine name: prefer colB, fallback to colA if colB is empty
                    val name = colB.ifEmpty { colA }

                    if (name.isEmpty()) continue

                    // Skip headers, titles, and section dividers
                    val lowerName = name.lowercase()
                    if (lowerName.startsWith("faculty offices") ||
                        lowerName.startsWith("faculty department") ||
                        lowerName.startsWith("sr #") ||
                        lowerName == "name"
                    ) {
                        continue
                    }

                    // Skip if name is just a serial number (e.g. "1.0")
                    if (name.toDoubleOrNull() != null) continue

                    facultyMembers.add(
                        FacultyMember(
                            name = name,
                            designation = designation,
                            office = office,
                            email = email
                        )
                    )
                }

                workbook.close()
                inputStream.close()

                withContext(Dispatchers.Main) {
                    facultyList = facultyMembers
                    adapter.submitList(facultyMembers)
                    android.util.Log.d("FacultyOffices", "Successfully loaded ${facultyMembers.size} faculty members")
                }
            } catch (e: Exception) {
                android.util.Log.e("FacultyOffices", "Error loading faculty data: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    com.google.android.material.snackbar.Snackbar.make(
                        findViewById(android.R.id.content),
                        "Error loading faculty data: ${e.message}",
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun filterFaculty(query: String) {
        val filtered = if (query.isEmpty()) {
            facultyList
        } else {
            facultyList.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.designation.contains(query, ignoreCase = true) ||
                it.office.contains(query, ignoreCase = true) ||
                it.email.contains(query, ignoreCase = true)
            }
        }
        adapter.submitList(filtered)
    }
}

