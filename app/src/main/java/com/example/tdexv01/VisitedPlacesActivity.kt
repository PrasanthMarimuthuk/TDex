package com.example.tdexv01

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.android.material.bottomnavigation.BottomNavigationView

class VisitedPlacesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val visitedPlaces = mutableListOf<AddedPlacesActivity.VisitedPlace>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visited_places)
        supportActionBar?.hide()

        recyclerView = findViewById(R.id.visitedPlacesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.setHasFixedSize(true)

        // Load visited places from SharedPreferences
        loadVisitedPlaces()

        updateVisitedPlacesList()

        // Set up Clear History button
        val btnClearHistory = findViewById<Button>(R.id.btnClearHistory)
        btnClearHistory.setOnClickListener {
            showClearHistoryConfirmation()
        }

        // Bottom NavigationView Setup (Fixed at Bottom)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    Toast.makeText(this, "Home Clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.visited -> {
                    Toast.makeText(this, "Visited Clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.tovisit -> {
                    startActivity(Intent(this, AddedPlacesActivity::class.java))
                    Toast.makeText(this, "To visited Clicked", Toast.LENGTH_SHORT).show()
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

    private fun updateVisitedPlacesList() {
        val adapter = VisitedPlacesAdapter(visitedPlaces)
        recyclerView.adapter = adapter
    }

    private fun loadVisitedPlaces() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val json = prefs.getString("visited_places", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<AddedPlacesActivity.VisitedPlace>>() {}.type
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

    private fun clearVisitedPlaces() {
        visitedPlaces.clear()
        saveVisitedPlaces()
        updateVisitedPlacesList()
        Toast.makeText(this, "Visited history cleared", Toast.LENGTH_SHORT).show()
    }

    private fun showClearHistoryConfirmation() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Clear History")
            .setMessage("Are you sure you want to clear all visited places?")
            .setPositiveButton("Yes") { _, _ ->
                clearVisitedPlaces()
            }
            .setNegativeButton("No", null)
            .show()
    }
}
