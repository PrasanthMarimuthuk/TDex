package com.example.tdexv01

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.Locale
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView



class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private lateinit var greetingTextView: TextView

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLastLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val searchEditText: EditText = findViewById(R.id.searchEditText)
        greetingTextView = findViewById(R.id.greetingTextView)

        val searchButton: ImageView = findViewById(R.id.searchButton)
        val categoryAllButton: Button = findViewById(R.id.categoryAllButton)
        val category5kmButton: Button = findViewById(R.id.category5kmButton)
        val category10kmButton: Button = findViewById(R.id.category10kmButton)
        val category15kmButton: Button = findViewById(R.id.category15kmButton)
        val category20kmButton: Button = findViewById(R.id.category20kmButton)
        findViewById<LinearLayout>(R.id.popularPlacesCarousel)
        val locationListView: ListView = findViewById(R.id.locationListView)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            getLastLocation()
        }
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



        locationListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedPlace = adapter.getItem(position) ?: ""
            searchEditText.setText(selectedPlace)
            locationListView.visibility = View.GONE

        }

    }

    private fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    try {
                        val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                        if (addresses != null && addresses.isNotEmpty()) {
                            val address = addresses[0]
                            val locality = address.locality ?: address.subAdminArea ?: address.adminArea

                            if(locality!=null){
                                greetingTextView.text = "You are in $locality"
                            }else{
                                greetingTextView.text = "Cannot get the current location"
                            }

                        } else {
                            greetingTextView.text = "Cannot get the current location"
                        }
                    } catch (e: Exception) {
                        greetingTextView.text = "Cannot get the current location"
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val templeCard: CardView = findViewById(R.id.maruthamalaiCard) // Replace with actual ID
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

    private fun selectCategoryButton(selectedButton: Button, otherButtons: Array<Button>) {
        selectedButton.setBackgroundResource(R.drawable.category_button_selected)
        selectedButton.setTextColor(ContextCompat.getColor(this, R.color.white)) // or R.color.white if color resource is defined

        for (button in otherButtons) {
            button.setBackgroundResource(R.drawable.category_button_background)
            button.setTextColor(ContextCompat.getColor(this, R.color.black)) // or R.color.black if color resource is defined
        }
    }
}