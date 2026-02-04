package com.example.fastable

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fastable.adapters.MapLocationAdapter
import com.example.fastable.data.models.MapLocation

class CampusMap : AppCompatActivity() {

    private lateinit var adapter: MapLocationAdapter
    private lateinit var rvMapItems: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var tvEmptyState: TextView

    private val allLocations = mutableListOf<MapLocation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_campus_map)

        setupToolbar()
        initViews()
        setupRecyclerView()
        loadMapData()
        setupSearch()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.campus_map)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initViews() {
        rvMapItems = findViewById(R.id.rvMapItems)
        etSearch = findViewById(R.id.etSearch)
        tvEmptyState = findViewById(R.id.tvEmptyState)
    }

    private fun setupRecyclerView() {
        adapter = MapLocationAdapter()
        rvMapItems.layoutManager = LinearLayoutManager(this)
        rvMapItems.adapter = adapter
    }

    private fun loadMapData() {
        // Block C - Green
        allLocations.add(MapLocation("C-301 to C-311", "3rd Floor, Block-C", R.color.block_c))
        allLocations.add(MapLocation("C-401-C-410", "4th Floor, Block-C", R.color.block_c))
        allLocations.add(MapLocation("C-110", "1st Floor, Block-C", R.color.block_c))

        // Block D - Blue
        allLocations.add(MapLocation("D-301 to D-316", "3rd Floor, Block-D", R.color.block_d))
        allLocations.add(MapLocation("D-401 to D-416", "4th Floor, Block-D", R.color.block_d))
        allLocations.add(MapLocation("D-501 to D-506", "5th Floor, Block-D", R.color.block_d))
        allLocations.add(MapLocation("D-IT Labs (1 to 4)", "2nd Floor, Block-D", R.color.block_d))

        // Margala Labs - Block C (different floor)
        allLocations.add(MapLocation("Margala Labs (1 to 4)", "2nd Floor, Block-C", R.color.block_c))

        // Rawal Labs - Block C
        allLocations.add(MapLocation("Rawal Labs (1 & 4)", "5th Floor, Block-C", R.color.block_c))
        allLocations.add(MapLocation("GPU Lab", "5th Floor, Block-C", R.color.block_c))

        // Block B - Orange/Yellow
        allLocations.add(MapLocation("Rawal Lab (3)", "2nd Floor, Block-B", R.color.block_b))

        // Block A - Pink/Magenta
        allLocations.add(MapLocation("Mehran Labs (1 & 2)", "3rd Floor, Block-A", R.color.block_a))

        // DLD Lab - Block B
        allLocations.add(MapLocation("DLD Lab", "2nd Floor, Block-B", R.color.block_b))

        // Call Labs - Red
        allLocations.add(MapLocation("Call Labs (1 & 2)", "2nd Floor, Block-A", R.color.block_call_lab))
        allLocations.add(MapLocation("Call Lab (3)", "3rd Floor, Block A", R.color.block_call_lab))

        // Khyber Labs - Yellow
        allLocations.add(MapLocation("Khyber Labs", "2nd Flr, Block-A", R.color.block_khyber))

        // Auditorium - Dark Green
        allLocations.add(MapLocation("Auditorium", "Ground Floor, Block-A", R.color.block_auditorium))

        // Submit the list to adapter
        adapter.submitList(allLocations.toList())
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterLocations(s?.toString() ?: "")
            }
        })
    }

    private fun filterLocations(query: String) {
        val filteredList = if (query.isEmpty()) {
            allLocations
        } else {
            allLocations.filter { location ->
                location.roomName.contains(query, ignoreCase = true) ||
                location.location.contains(query, ignoreCase = true)
            }
        }

        adapter.submitList(filteredList)

        if (filteredList.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            rvMapItems.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            rvMapItems.visibility = View.VISIBLE
        }
    }
}

