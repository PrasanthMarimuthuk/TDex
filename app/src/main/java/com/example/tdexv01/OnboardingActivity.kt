package com.example.tdexv01

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import android.widget.Button
import android.widget.TextView
import com.example.tdexv01.adapter.OnboardingAdapter

class OnboardingActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var exploreButton: Button
    private lateinit var welcomeText: TextView
    private val handler = Handler(Looper.getMainLooper())

    private val imageList = listOf(
        R.drawable.heriage_1,  // Ensure these drawables exist
        R.drawable.heriage_2,
        R.drawable.heriage_3,
        R.drawable.heriage_4,
        R.drawable.heritage_5,
        R.drawable.heriage_6
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        supportActionBar?.hide()

        viewPager = findViewById(R.id.viewPager)
        exploreButton = findViewById(R.id.btnExplore)
        welcomeText = findViewById(R.id.welcomeText) // Ensure TextView is in XML

        // Set up ViewPager adapter
        val adapter = OnboardingAdapter(imageList)
        viewPager.adapter = adapter

        // Auto-Swipe Handler
        val runnable = object : Runnable {
            override fun run() {
                val nextItem = (viewPager.currentItem + 1) % imageList.size
                viewPager.setCurrentItem(nextItem, true)
                handler.postDelayed(this, 5000) // Auto-slide every 5 seconds
            }
        }
        handler.postDelayed(runnable, 5000)

        // Explore Now Button Click Event
        exploreButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java) // Change to actual main screen
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null) // Stop auto-slide when activity is destroyed
    }
}

