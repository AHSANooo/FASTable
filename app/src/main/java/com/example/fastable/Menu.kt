package com.example.fastable

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth

class Menu : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, Login::class.java))
                finish()
            }

        val profile = findViewById<LinearLayout>(R.id.profileSection)
        profile.setOnClickListener {
            val intent= Intent(this, Profile::class.java)
            startActivity(intent)
        }

        val topAppBar = findViewById<Toolbar>(R.id.top_bar_menu)
        topAppBar.setNavigationOnClickListener {
            val intent = Intent(this, Home::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

    }
}