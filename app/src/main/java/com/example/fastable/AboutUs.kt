package com.example.fastable

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AboutUs : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)

        // Handle back button
        val backBtn = findViewById<ImageView>(R.id.backBtn)
        backBtn.setOnClickListener {
            finish()
        }

        // Person 1 - Muhammad Ahsan
        setupEmailClick(R.id.emailText1, "i230553@isb.nu.edu.pk")
        setupLinkedInClick(R.id.linkedinText1, R.id.linkedinIcon1, "muhammad-ahsan-7612701a7")

        // Person 2 - Ali Ahmad Riaz
        setupEmailClick(R.id.emailText2, "i232528@isb.nu.edu.pk")
        setupLinkedInClick(R.id.linkedinText2, R.id.linkedinIcon2, "aliahmadriaz")

        // Person 3 - Farzeen Khan Tareen
        setupEmailClick(R.id.emailText3, "i230721@isb.nu.edu.pk")
        setupLinkedInClick(R.id.linkedinText3, R.id.linkedinIcon3, "muhammad-farzeen-khan-tareen")
    }

    private fun setupEmailClick(textViewId: Int, email: String) {
        val emailTextView = findViewById<TextView>(textViewId)
        emailTextView.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$email")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupLinkedInClick(textViewId: Int, iconId: Int, linkedInUsername: String) {
        val linkedInTextView = findViewById<TextView>(textViewId)
        val linkedInIcon = findViewById<ImageView>(iconId)

        val clickListener = {
            try {
                // Try to open in LinkedIn app first
                val linkedInAppIntent = Intent(Intent.ACTION_VIEW, Uri.parse("linkedin://profile/$linkedInUsername"))
                linkedInAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(linkedInAppIntent)
            } catch (e: Exception) {
                // If LinkedIn app not installed, open in browser
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/$linkedInUsername"))
                    startActivity(browserIntent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Cannot open LinkedIn", Toast.LENGTH_SHORT).show()
                }
            }
        }

        linkedInTextView.setOnClickListener { clickListener() }
        linkedInIcon.setOnClickListener { clickListener() }
    }
}