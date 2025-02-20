package com.example.tdexv01

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val searchEditText: EditText = findViewById(R.id.searchEditText)
        val searchButton: ImageView = findViewById(R.id.searchButton)
        val categoryAllButton: Button = findViewById(R.id.categoryAllButton)
        val category5kmButton: Button = findViewById(R.id.category5kmButton)
        val category10kmButton: Button = findViewById(R.id.category10kmButton)
        val category15kmButton: Button = findViewById(R.id.category15kmButton)
        val category20kmButton: Button = findViewById(R.id.category20kmButton)

        val popularPlacesCarousel: LinearLayout = findViewById(R.id.popularPlacesCarousel)

        searchButton.setOnClickListener {
            val searchText = searchEditText.text.toString()
            Toast.makeText(this, "Search for: $searchText", Toast.LENGTH_SHORT).show()
        }

        categoryAllButton.setOnClickListener {
            selectCategoryButton(categoryAllButton, arrayOf(category5kmButton, category10kmButton, category15kmButton, category20kmButton))
            Toast.makeText(this, "Category: All", Toast.LENGTH_SHORT).show()
        }

        category5kmButton.setOnClickListener {
            selectCategoryButton(category5kmButton, arrayOf(categoryAllButton, category10kmButton, category15kmButton, category20kmButton))
            Toast.makeText(this, "Category: 5 Km", Toast.LENGTH_SHORT).show()
        }

        category10kmButton.setOnClickListener {
            selectCategoryButton(category10kmButton, arrayOf(categoryAllButton, category5kmButton, category15kmButton, category20kmButton))
            Toast.makeText(this, "Category: 10 Km", Toast.LENGTH_SHORT).show()
        }

        category15kmButton.setOnClickListener {
            selectCategoryButton(category15kmButton, arrayOf(categoryAllButton, category5kmButton, category10kmButton, category20kmButton))
            Toast.makeText(this, "Category: 15 Km", Toast.LENGTH_SHORT).show()
        }

        category20kmButton.setOnClickListener {
            selectCategoryButton(category20kmButton, arrayOf(categoryAllButton, category5kmButton, category10kmButton, category15kmButton))
            Toast.makeText(this, "Category: 20 Km", Toast.LENGTH_SHORT).show()
        }

        val templeCard: CardView = findViewById(R.id.BrahidheerwararTemple) // Replace with actual ID
        templeCard.setOnClickListener {
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("temple_name", "Maruthamalai Temple")
            intent.putExtra("temple_location", "Maruthamalai, Tamil Nadu")
            intent.putExtra("temple_distance", "20 Km")
            intent.putExtra("temple_description", "Maruthamalai temple is Nestled on a serene hill in Coimbatore, the Maruthamalai Murugan Temple is a divine 12th-century marvel dedicated to Lord Murugan. Ascend 837 steps to witness the stunning 180-meter tall gopuram adorned with intricate carvings. This pilgrimage site is famed for its self-manifested idol, medicinal springs, and the vibrant annual Thaipoosam festival. The temple’s breathtaking views and serene atmosphere make it an unmissable spiritual haven for travelers.")

            // Assuming drawable resources exist for these images
            intent.putExtra("temple_image1", R.drawable.maruthamalai_1)
            intent.putExtra("temple_image2", R.drawable.maruthamalai_2) // Use different image if available
            intent.putExtra("temple_image3", R.drawable.maruthamalai_3) // Use different image if available
            intent.putExtra("temple_image4", R.drawable.marudhamalai_4) // Use different image if available
            startActivity(intent)


        }

        // Bottom NavigationView Setup
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    Toast.makeText(this, "Home Clicked", Toast.LENGTH_SHORT).show()
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

    private fun selectCategoryButton(selectedButton: Button, otherButtons: Array<Button>) {
        selectedButton.setBackgroundResource(R.drawable.category_button_selected)
        selectedButton.setTextColor(getColor(R.color.white))
        for (button in otherButtons) {
            button.setBackgroundResource(R.drawable.category_button_background)
            button.setTextColor(getColor(R.color.black))
        }
    }
}