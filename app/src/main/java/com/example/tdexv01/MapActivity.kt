package com.example.tdexv01

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView


class MapActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.map_activity)

        val searchEditText: EditText = findViewById(R.id.searchEditText)
        val searchButton: ImageView = findViewById(R.id.searchButton)
        val categoryAllButton: Button = findViewById(R.id.categoryAllButton)
        val category5kmButton: Button = findViewById(R.id.category5kmButton)
        val category10kmButton: Button = findViewById(R.id.category10kmButton)
        val category15kmButton: Button = findViewById(R.id.category15kmButton)
        val category20kmButton: Button = findViewById(R.id.category20kmButton)
        val homeButton: Button = findViewById(R.id.homeButton)
        val popularPlacesCarousel: LinearLayout = findViewById(R.id.popularPlacesCarousel)

        searchButton.setOnClickListener {
            val searchText = searchEditText.text.toString()
            Toast.makeText(this, "Search for: $searchText", Toast.LENGTH_SHORT).show()
            // Implement search functionality here
        }

        categoryAllButton.setOnClickListener {
            selectCategoryButton(categoryAllButton, arrayOf(category5kmButton, category10kmButton, category15kmButton, category20kmButton))
            Toast.makeText(this, "Category: All", Toast.LENGTH_SHORT).show()
            // Implement filter functionality for All category
        }

        category5kmButton.setOnClickListener {
            selectCategoryButton(category5kmButton, arrayOf(categoryAllButton, category10kmButton, category15kmButton, category20kmButton))
            Toast.makeText(this, "Category: 5 Km", Toast.LENGTH_SHORT).show()
            // Implement filter functionality for 5 Km category
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

        homeButton.setOnClickListener {
            Toast.makeText(this, "Home button clicked", Toast.LENGTH_SHORT).show()
            // Implement home action
        }

        // You can dynamically add more popular place items to the carousel here if needed.
        // For example, using data from an API or local data source.
    }

    private fun selectCategoryButton(selectedButton: Button, otherButtons: Array<Button>) {
        selectedButton.setBackgroundResource(R.drawable.category_button_selected)
        selectedButton.setTextColor(getColor(R.color.white)) // or R.color.white if color resource is defined

        for (button in otherButtons) {
            button.setBackgroundResource(R.drawable.category_button_background)
            button.setTextColor(getColor(R.color.black)) // or R.color.black if color resource is defined
        }
    }
}