package com.example.tdexv01

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.fragment.app.DialogFragment

class TempleDetailActivity : AppCompatActivity() {

    private lateinit var templeImages: Array<ImageView>
    private lateinit var templeName: TextView
    private lateinit var templeLocation: TextView
    private lateinit var templeDistance: TextView
    private lateinit var templeDescription: TextView
    private lateinit var btnAdd: Button
    private lateinit var btnDirection: Button
    private lateinit var bottomNavigationView: com.google.android.material.bottomnavigation.BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temple_detail)
        supportActionBar?.hide()

        // Initialize UI components
        templeImages = arrayOf(
            findViewById(R.id.templeImage1),
            findViewById(R.id.templeImage2),
            findViewById(R.id.templeImage3),
            findViewById(R.id.templeImage4)
        )
        templeName = findViewById(R.id.templeName)
        templeLocation = findViewById(R.id.templeLocation)
        templeDistance = findViewById(R.id.templeDistance)
        templeDescription = findViewById(R.id.templeDescription)
        btnAdd = findViewById(R.id.btnAdd)
        btnDirection = findViewById(R.id.btnDirection)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        // Get Data from Intent
        val intent = intent
        if (intent != null) {
            templeName.text = intent.getStringExtra("temple_name") ?: "Unknown Temple"
            templeLocation.text = intent.getStringExtra("temple_location") ?: "Unknown Location"
            templeDistance.text = intent.getStringExtra("temple_distance") ?: "Distance Unavailable"
            templeDescription.text = intent.getStringExtra("temple_description") ?: "No Description"

            // Assuming we pass four image resources
            val imageRes1 = intent.getIntExtra("temple_image1", R.drawable.maruthamalai_1)
            val imageRes2 = intent.getIntExtra("temple_image2", R.drawable.maruthamalai_2)
            val imageRes3 = intent.getIntExtra("temple_image3", R.drawable.maruthamalai_3)
            val imageRes4 = intent.getIntExtra("temple_image4", R.drawable.marudhamalai_4)

            templeImages[0].setImageResource(imageRes1)
            templeImages[1].setImageResource(imageRes2)
            templeImages[2].setImageResource(imageRes3)
            templeImages[3].setImageResource(imageRes4)

            // Set click listeners for each image to expand
            templeImages.forEachIndexed { index, imageView ->
                imageView.setOnClickListener {
                    showFullScreenImage(index, imageRes1, imageRes2, imageRes3, imageRes4)
                }
            }
        }

        // Handle Direction Button Click
        btnDirection.setOnClickListener {
            val location = templeLocation.text.toString()
            if (location.isNotEmpty()) {
                val gmmIntentUri = Uri.parse("geo:0,0?q=$location")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                startActivity(mapIntent)
            }
        }

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.visited -> {
                    Toast.makeText(this, "Visited Clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.map -> {
                    Toast.makeText(this, "Map Clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.profile -> {
                    Toast.makeText(this, "Profile Clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    private fun showFullScreenImage(index: Int, res1: Int, res2: Int, res3: Int, res4: Int) {
        val imageRes = when (index) {
            0 -> res1
            1 -> res2
            2 -> res3
            3 -> res4
            else -> R.drawable.maruthamalai_1 // Default
        }

        // Create and show a DialogFragment for full-screen image
        val dialog = FullScreenImageDialogFragment.newInstance(imageRes)
        dialog.show(supportFragmentManager, "FullScreenImageDialog")
    }
}

// Update FullScreenImageDialogFragment
class FullScreenImageDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_IMAGE_RES = "image_res"

        fun newInstance(imageRes: Int): FullScreenImageDialogFragment {
            val fragment = FullScreenImageDialogFragment()
            val args = Bundle()
            args.putInt(ARG_IMAGE_RES, imageRes)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val imageRes = arguments?.getInt(ARG_IMAGE_RES) ?: R.drawable.maruthamalai_1
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_full_screen_image)

        val imageView = dialog.findViewById<ImageView>(R.id.fullScreenImage)
        val closeButton = dialog.findViewById<Button>(R.id.btnClose)

        imageView.setImageResource(imageRes)

        // Handle close button click
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        // Optional: Dismiss on tap anywhere on the screen
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        return dialog
    }
    
}