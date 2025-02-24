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
import com.google.firebase.auth.FirebaseAuth

class OnboardingActivity : BaseActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var btnSignIn: Button
    private lateinit var btnSignUp: Button
    private lateinit var welcomeText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("OnboardingPrefs", MODE_PRIVATE) }

    private val imageList = listOf(
        R.drawable.heriage_1,  // Fixed incorrect drawable names
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

        // Check if onboarding has already been shown or user is logged in
        val hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding", false)
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (hasSeenOnboarding && currentUser != null) {
            // Skip onboarding and go to MainActivity if user is logged in and has seen onboarding
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        viewPager = findViewById(R.id.viewPager)
        btnSignIn = findViewById(R.id.btnSignedIn)
        btnSignUp = findViewById(R.id.btnSignUp)
        welcomeText = findViewById(R.id.welcomeText)

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

        // Navigate to Sign-In Screen and mark onboarding as seen
        btnSignIn.setOnClickListener {
            prefs.edit().putBoolean("has_seen_onboarding", true).apply()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }

        // Navigate to Sign-Up Screen and mark onboarding as seen
        btnSignUp.setOnClickListener {
            prefs.edit().putBoolean("has_seen_onboarding", true).apply()
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null) // Stop auto-slide when activity is destroyed
    }
}