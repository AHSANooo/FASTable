package com.example.fastable

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fastable.adapters.NotificationAdapter
import com.example.fastable.data.local.AppDatabase
import kotlinx.coroutines.launch
import android.widget.TextView
import android.view.View

class Notification : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private lateinit var tvEmptyState: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            val intent = Intent(this, Home::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        recyclerView = findViewById(R.id.rvNotifications)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        setupRecyclerView()
        loadNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter { notification ->
            // Mark as read when clicked
            lifecycleScope.launch {
                AppDatabase.getDatabase(this@Notification).notificationDao()
                    .markAsRead(notification.id)
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadNotifications() {
        lifecycleScope.launch {
            // Only keep last 5 days
            val fiveDaysAgo = System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000L)
            AppDatabase.getDatabase(this@Notification).notificationDao()
                .deleteOldNotifications(fiveDaysAgo)

            // Load notifications
            AppDatabase.getDatabase(this@Notification).notificationDao()
                .getRecentNotifications()
                .collect { notifications ->
                    if (notifications.isEmpty()) {
                        tvEmptyState.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        tvEmptyState.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        adapter.submitList(notifications)
                    }
                }
        }
    }
}