package com.example.fastable

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class CustomTimetable : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_custom_timetable)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)


        var batchtimetable=findViewById<MaterialButton>(R.id.btnBatchTimetable)
        batchtimetable.setOnClickListener {
            val intent = Intent(this, Add_Timetable::class.java)
            startActivity(intent)
            finish()
        }

        // Handle back navigation
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Handle notification icon if present
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.notification_icon -> {
                    val intent = Intent(this, Notification::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }
}