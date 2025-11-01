package com.example.fastable

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
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
            val intent= Intent(this, Menu::class.java)
            startActivity(intent)
        }
        ivMenu.setOnClickListener {
            val intent= Intent(this, Menu::class.java)
            startActivity(intent)
        }


    }
}