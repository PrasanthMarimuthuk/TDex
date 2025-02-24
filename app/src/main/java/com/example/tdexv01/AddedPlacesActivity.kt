package com.example.tdexv01

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.tdexv01.MainActivity.Place
import java.util.Locale

class AddedPlacesActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private val addedPlaces = mutableListOf<Place>()
    private val visitedPlaces = mutableListOf<VisitedPlace>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_added_places)
        supportActionBar?.hide()

        recyclerView = findViewById(R.id.addedPlacesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.setHasFixedSize(true)

        // Load added and visited places from SharedPreferences
        loadAddedPlaces()
        loadVisitedPlaces()

        // Handle intent extra for adding a new place
        intent.getParcelableExtra<Place>("place")?.let { place ->
            addPlace(place)
        }

        updateAddedPlacesList()

        // Bottom NavigationView Setup (Fixed at Bottom)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    Toast.makeText(this, getString(R.string.home_clicked), Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.visited -> {
                    startActivity(Intent(this, VisitedPlacesActivity::class.java))
                    Toast.makeText(this, getString(R.string.visited_clicked), Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.tovisit -> {
                    Toast.makeText(this, getString(R.string.to_visit_clicked), Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.profile -> {
                    Toast.makeText(this, getString(R.string.profile_clicked), Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun updateAddedPlacesList() {
        val adapter = AddedPlacesAdapter(addedPlaces) { position ->
            // Handle delete or mark as visited actions
            showActionDialog(position)
        }
        recyclerView.adapter = adapter
    }

    private fun showActionDialog(position: Int) {
        val place = addedPlaces[position]
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.choose_action))
            .setItems(arrayOf(getString(R.string.mark_as_visited), getString(R.string.delete))) { _, which ->
                when (which) {
                    0 -> markAsVisited(position, place) // Mark as Visited
                    1 -> deletePlace(position) // Delete
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
        dialog.show()
    }

    private fun markAsVisited(position: Int, place: Place) {
        val dialog = VisitedPlaceDialogFragment.newInstance(place) { date ->
            val visitedPlace = VisitedPlace(place.name, place.location, place.image1, date)
            visitedPlaces.add(visitedPlace)
            saveVisitedPlaces()
            Log.d("AddedPlacesActivity", "Mark as Visited: ${place.name}, Date: $date, Locale: ${Locale.getDefault().language}")
        }
        dialog.show(supportFragmentManager, "VisitedPlaceDialog")
    }

    private fun deletePlace(position: Int) {
        if (position in 0 until addedPlaces.size) {
            val place = addedPlaces.removeAt(position)
            saveAddedPlaces()
            updateAddedPlacesList()
            Toast.makeText(this, getString(R.string.place_removed_from_visit_locations, place.name), Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAddedPlaces() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val json = prefs.getString("added_places", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Place>>() {}.type
            val rawPlaces = Gson().fromJson<MutableList<Place>>(json, type) ?: emptyList()
            addedPlaces.clear()
            // Re-localize each place using the current context
            rawPlaces.forEach { rawPlace ->
                val localizedPlace = MainActivity.Place.getAllPlaces(this).find { it.latitude == rawPlace.latitude && it.longitude == rawPlace.longitude }
                localizedPlace?.let { addedPlaces.add(it) }
            }
        }
    }

    private fun saveAddedPlaces() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val editor = prefs.edit()
        val json = Gson().toJson(addedPlaces.map { Place(it.name, it.location, it.staticDistance, it.description, it.latitude, it.longitude, it.image1, it.image2, it.image3, it.image4, it.openingHours, it.closingHours, it.operatingWeekdays) })
        editor.putString("added_places", json)
        editor.apply()
    }

    private fun loadVisitedPlaces() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val json = prefs.getString("visited_places", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<VisitedPlace>>() {}.type
            visitedPlaces.clear()
            visitedPlaces.addAll(Gson().fromJson(json, type) ?: emptyList())
        }
    }

    private fun saveVisitedPlaces() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val editor = prefs.edit()
        val json = Gson().toJson(visitedPlaces)
        editor.putString("visited_places", json)
        editor.apply()
    }

    fun addPlace(place: Place) {
        if (!addedPlaces.contains(place)) {
            addedPlaces.add(place)
            saveAddedPlaces()
            updateAddedPlacesList()
            Toast.makeText(this, getString(R.string.place_added_to_visit_locations, place.name), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1002 && resultCode == RESULT_OK) {
            data?.getParcelableExtra<Place>("place")?.let { place ->
                addPlace(place)
            }
        }
    }

    // Data class for visited places, using localized strings
    data class VisitedPlace(
        val name: String,
        val location: String,
        val imageRes: Int,
        val visitDate: String
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readInt(),
            parcel.readString() ?: ""
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(name)
            parcel.writeString(location)
            parcel.writeInt(imageRes)
            parcel.writeString(visitDate)
        }

        override fun describeContents(): Int = 0

        companion object CREATOR : Parcelable.Creator<VisitedPlace> {
            override fun createFromParcel(parcel: Parcel): VisitedPlace = VisitedPlace(parcel)
            override fun newArray(size: Int): Array<VisitedPlace?> = arrayOfNulls(size)
        }
    }
}