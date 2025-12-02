package com.example.fastable

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth

class Home : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Get reference to the drawer layout
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)

        // Top app bar - open drawer on menu icon click
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Handle notification icon click
        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.notification_icon -> {
                    val intent = Intent(this, Notification::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Drawer menu top bar - close drawer on menu icon click
        val topBarMenu = findViewById<Toolbar>(R.id.top_bar_menu)
        topBarMenu.setNavigationOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Batch Timetable button
        val btnBatchTimetable = findViewById<Button>(R.id.btnBatchTimetable)
        btnBatchTimetable.setOnClickListener {
            val intent = Intent(this, CustomTimetable::class.java)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Custom Timetable button
        val btnCustomTimetable = findViewById<Button>(R.id.btnCustomTimetable)
        btnCustomTimetable.setOnClickListener {
            val intent = Intent(this, Add_Timetable::class.java)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // About Us button
        val btnAboutUs = findViewById<Button>(R.id.btnAboutUs)
        btnAboutUs.setOnClickListener {
            val intent = Intent(this, AboutUs::class.java)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Logout button
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, Login::class.java))
            finish()
        }

        // Profile section
        val profile = findViewById<LinearLayout>(R.id.profileSection)
        profile.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }
}
