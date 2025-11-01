package com.example.fastable

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class Add_Timetable : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_timetable)
        val ivBack=findViewById<ImageView>(R.id.ivBack)
        val ivMenu=findViewById<ImageView>(R.id.ivMenu)
        ivBack.setOnClickListener {
            finish()
        }
        ivMenu.setOnClickListener {
            val intent = Intent(this, Menu::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
        var customTimetable = findViewById<TextView>(R.id.tvCustomTimetable)
        customTimetable.setOnClickListener {
            val intent = Intent(this, CustomTimetable::class.java)
            startActivity(intent)
            finish()
        }


    }
}