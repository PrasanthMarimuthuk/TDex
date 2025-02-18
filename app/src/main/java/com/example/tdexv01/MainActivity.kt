package com.example.tdexv01

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity



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
        val homeButton: Button = findViewById(R.id.homeButton)
        findViewById<LinearLayout>(R.id.popularPlacesCarousel)
        val locationListView: ListView = findViewById(R.id.locationListView)

        val coimbatorePlaces = listOf(
            "Marudamalai Temple",
            "Adiyogi Shiva Statue",
            "VOC Park and Zoo",
            "Brookefields Mall",
            "Kovai Kondattam",
            "Black Thunder",
            "Gedee Car Museum",
            "Isha Yoga Center",
            "Siruvani Waterfalls",
            "Monkey Falls",
            "Dhyanalinga",
            "Perur Pateeswarar Temple",
            "Eachanari Vinayagar Temple",
            "Kasthuri Sreenivasan Art Gallery and Textile Museum"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ArrayList<String>())
        locationListView.adapter = adapter
        locationListView.visibility = View.GONE

        searchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (searchEditText.text.toString().isNotEmpty()){
                    locationListView.visibility = View.VISIBLE
                }

            } else {
                locationListView.visibility = View.GONE
            }
        }


        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchText = s.toString().trim()
                adapter.clear()
                if (searchText.isNotEmpty()) {
                    val filteredPlaces = coimbatorePlaces.filter { it.contains(searchText, ignoreCase = true) }
                    adapter.addAll(filteredPlaces)
                    if(searchEditText.hasFocus()){
                        locationListView.visibility = View.VISIBLE
                    }

                } else {
                    locationListView.visibility = View.GONE
                }
                adapter.notifyDataSetChanged()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        searchButton.setOnClickListener {
            val searchText = searchEditText.text.toString().trim()
            if (searchText.isNotEmpty()) {
            }
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
        }

        homeButton.setOnClickListener {
            // Implement home action
        }

        locationListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedPlace = adapter.getItem(position) ?: ""
            searchEditText.setText(selectedPlace)
            locationListView.visibility = View.GONE

        }
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