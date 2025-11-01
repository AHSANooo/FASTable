package com.example.fastable

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class Menu : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)
        val btnBatchTimetable=findViewById<Button>(R.id.btnBatchTimetable)
        val btnCustomTimetable=findViewById<Button>(R.id.btnCustomTimetable)
        btnBatchTimetable.setOnClickListener {
            val intent= Intent(this, CustomTimetable::class.java)
            startActivity(intent)
        }
        btnCustomTimetable.setOnClickListener {
            val intent= Intent(this, Add_Timetable::class.java)
            startActivity(intent)
        }
        val btnAboutUs=findViewById<Button>(R.id.btnAboutUs)

        btnAboutUs.setOnClickListener {
            val intent= Intent(this, AboutUs::class.java)
            startActivity(intent)
        }
        val btnLogout=findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            val intent= Intent(this, Login::class.java)
            startActivity(intent)
        }

    }
}