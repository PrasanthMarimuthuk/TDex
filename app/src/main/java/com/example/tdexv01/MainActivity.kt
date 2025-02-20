package com.example.tdexv01

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log // Import Log
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONArray
import java.io.IOException
import java.io.InputStream

import java.util.ArrayList

import java.util.HashMap
import java.util.Locale
import kotlin.concurrent.read

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private lateinit var locationTextView: TextView
    private lateinit var places: ArrayList<HashMap<String, String>>
    private lateinit var coimbatorePlaces: List<String> // Changed to lateinit and will be initialized from JSON

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
        //val popularPlaces: ListView = findViewById(R.id.popularPlaces)
        locationTextView = findViewById(R.id.locationTextView)

        val searchButton: ImageView = findViewById(R.id.searchButton)
        val categoryAllButton: Button = findViewById(R.id.categoryAllButton)
        val category5kmButton: Button = findViewById(R.id.category5kmButton)
        val category10kmButton: Button = findViewById(R.id.category10kmButton)
        val category15kmButton: Button = findViewById(R.id.category15kmButton)
        val category20kmButton: Button = findViewById(R.id.category20kmButton)
        val popularPlacesCarousel: LinearLayout = findViewById(R.id.popularPlacesCarousel)
        val locationListView: ListView = findViewById(R.id.locationListView)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        // Load location names from JSON file
        //coimbatorePlaces = loadLocationsFromJson()
        //places =  loadPlacesFromJson()
        //Log.d("MainActivity", "coimbatorePlaces after loading: $coimbatorePlaces") // Log after loading JSON
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


//        val from = arrayOf("name")
//        val to = intArrayOf(android.R.id.text1)

        //val placesAdapter = SimpleAdapter(this,places, android.R.layout.simple_list_item_1,from,to)


        //popularPlaces.adapter = placesAdapter

//        popularPlaces.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
//            val selectedPlace = places[position]
//            val placeName = selectedPlace["name"]
//            searchEditText.setText(placeName)
//        }

        searchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (searchEditText.text.toString().isNotEmpty()) {
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
                Log.d("MainActivity", "Search text changed: $searchText") // Log search text

                adapter.clear()
                if (searchText.isNotEmpty()) {
                    val filteredPlaces = coimbatorePlaces.filter { it.contains(searchText, ignoreCase = true) }
                    Log.d("MainActivity", "Filtered places: $filteredPlaces") // Log filtered places
                    adapter.addAll(filteredPlaces)
                    if (searchEditText.hasFocus()) {
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

                            if (locality != null) {
                                locationTextView.text = "You are in $locality"
                            } else {
                                locationTextView.text = "Cannot get the current location"
                            }

                        } else {
                            locationTextView.text = "Cannot get the current location"
                        }
                    } catch (e: Exception) {
                        locationTextView.text = "Cannot get the current location"
                        e.printStackTrace()
                    }
                }
            }
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