package com.example.tdexv01
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val gifImageView = findViewById<ImageView>(R.id.gifImageView)

        // Load GIF using Glide
        Glide.with(this)
            .asGif()  // Ensure it's loaded as a GIF
            .load(R.drawable.app_logo)  // Replace 'your_gif' with actual GIF file name in 'res/drawable'
            .diskCacheStrategy(DiskCacheStrategy.NONE) // Prevent caching issues
            .into(gifImageView)

        // Animate the progress bar
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val animation = ObjectAnimator.ofInt(progressBar, "progress", 0, 100)
        animation.duration = 3000 // 3 seconds
        animation.interpolator = DecelerateInterpolator()
        animation.start()

        // Delay for 3 seconds and move to OnboardingActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish() // Close Splash Screen
        }, 3000) // 3000 ms = 3 seconds
    }
}
