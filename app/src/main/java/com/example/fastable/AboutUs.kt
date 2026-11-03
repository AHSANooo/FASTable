package com.example.fastable

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class AboutUs : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.tvAppVersion)?.text = "Version ${BuildConfig.VERSION_NAME}"

        // Person 1 - Muhammad Ahsan
        setupEmailClick(R.id.emailText1, "i230553@isb.nu.edu.pk")
        setupLinkedInClick(R.id.btnLinkedIn1, "muhammad-ahsan-7612701a7")

        // Person 2 - Ali Ahmad Riaz
        setupEmailClick(R.id.emailText2, "i232528@isb.nu.edu.pk")
        setupLinkedInClick(R.id.btnLinkedIn2, "aliahmadriaz")

        // Person 3 - Farzeen Khan Tareen
        setupEmailClick(R.id.emailText3, "i230721@isb.nu.edu.pk")
        setupLinkedInClick(R.id.btnLinkedIn3, "muhammad-farzeen-khan-tareen")

        // GitHub Repository Link
        findViewById<Button>(R.id.btnGitHub)?.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/AHSANooo/FASTable"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open browser", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupEmailClick(textViewId: Int, email: String) {
        val emailTextView = findViewById<TextView>(textViewId) ?: return
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

    private fun setupLinkedInClick(buttonId: Int, linkedInUsername: String) {
        val button = findViewById<ImageButton>(buttonId) ?: return
        button.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/$linkedInUsername"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot open LinkedIn", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
