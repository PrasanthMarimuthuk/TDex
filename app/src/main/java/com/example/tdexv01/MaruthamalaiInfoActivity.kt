package com.example.tdexv01

import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Matrix
import android.util.Log

class MaruthamalaiInfoActivity : BaseActivity() {
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var scaleFactor = 1.0f
    private val matrix = Matrix()
    private var lastX = 0f
    private var lastY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_maruthamalai_info)
            supportActionBar?.hide()

            // Find views with null checking
            val templeImage = findViewById<ImageView>(R.id.maruthamalaiImage)
            val templeDescription = findViewById<TextView>(R.id.maruthamalaiDescription)
            val distanceText = findViewById<TextView>(R.id.maruthamalaiDistance)

            if (templeImage == null || templeDescription == null || distanceText == null) {
                Log.e("MaruthamalaiInfo", "One or more views not found")
                finish()
                return
            }

            // Get distance from intent
            val distance = intent.getDoubleExtra("distance", -1.0)

            // Set temple content safely
            try {
                templeImage.setImageResource(R.drawable.thanjavur_carving  )
                templeDescription.text = getString(R.string.maruthamalai_pambatti_siddhar_temple_description)
                distanceText.text = if (distance >= 0) {
                    getString(R.string.distance_from_user, String.format("%.2f", distance))
                } else {
                    "Distance not available"
                }
            } catch (e: Exception) {
                Log.e("MaruthamalaiInfo", "Error setting content: ${e.message}")
                templeDescription.text = "Error loading content"
            }

            // Initialize zoom functionality
            scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    scaleFactor *= detector.scaleFactor
                    scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f))
                    matrix.setScale(scaleFactor, scaleFactor)
                    templeImage.imageMatrix = matrix
                    return true
                }
            })

            // Improved touch handling for zoom and pan
            templeImage.setOnTouchListener { _, event ->
                scaleGestureDetector.onTouchEvent(event)

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastX = event.x
                        lastY = event.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (scaleFactor > 1.0f) {
                            val dx = event.x - lastX
                            val dy = event.y - lastY
                            matrix.postTranslate(dx, dy)
                            templeImage.imageMatrix = matrix
                            lastX = event.x
                            lastY = event.y
                        }
                    }
                }
                true
            }
        } catch (e: Exception) {
            Log.e("MaruthamalaiInfo", "Activity creation failed: ${e.message}")
            finish()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return try {
            scaleGestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
        } catch (e: Exception) {
            Log.e("MaruthamalaiInfo", "Touch event error: ${e.message}")
            false
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onBackPressed(view: android.view.View) {
        finish()
    }
}