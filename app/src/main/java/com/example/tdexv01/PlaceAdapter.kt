package com.example.tdexv01

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class PlaceAdapter(
    private val places: List<MainActivity.Place>,
    private val currentLatitude: Double,
    private val currentLongitude: Double,
    private val context: Context
) : RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>() {

    class PlaceViewHolder(itemView: View, private val context: Context) : RecyclerView.ViewHolder(itemView) {
        val placeName: TextView = itemView.findViewById(R.id.placeName)
        val placeLocation: TextView = itemView.findViewById(R.id.placeLocation)
        val placeDistance: TextView = itemView.findViewById(R.id.placeDistance)
        val placeImage: ImageView = itemView.findViewById(R.id.placeImage)
        val addButton: Button = itemView.findViewById(R.id.btnAddToMap) // Match the ID in place_card_item.xml

        fun bind(place: MainActivity.Place, currentLatitude: Double, currentLongitude: Double, calculateDistance: (Double, Double, Double, Double) -> Double) {
            // Set text using localized strings from the Place object
            placeName.text = place.name // Should already be localized via Place.getAllPlaces()
            placeLocation.text = place.location // Should already be localized
            val distance = calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude).roundToInt()
            placeDistance.text = "${distance} ${context.getString(R.string.km)}" // Use localized "km"

            // Set image
            placeImage.setImageResource(place.image1) // Use the first image for the list item
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.place_card_item, parent, false)
        return PlaceViewHolder(view, context) // Pass the context to PlaceViewHolder
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = places[position]

        // Log the current locale and strings for debugging
        val currentLocale = Locale.getDefault()
        Log.d("PlaceAdapter", "Current Locale: ${currentLocale.language}, Name: ${place.name}, Location: ${place.location}")

        // Bind place data using the calculateDistance function
        holder.bind(place, currentLatitude, currentLongitude) { lat1, lon1, lat2, lon2 ->
            calculateDistance(lat1, lon1, lat2, lon2)
        }

        // Set click listener for the item view (to open TempleDetailActivity)
        holder.itemView.setOnClickListener {
            val intent = Intent(context, TempleDetailActivity::class.java)
            intent.putExtra("place", place)
            context.startActivity(intent)
        }

        // Set click listener for the Add button to add the place to AddedPlacesActivity
        holder.addButton.setOnClickListener {
            // Cast context to MainActivity to call addPlaceToList
            (context as? MainActivity)?.addPlaceToList(place) ?: run {
                // Fallback: Start AddedPlacesActivity directly if context isn’t MainActivity
                val intent = Intent(context, AddedPlacesActivity::class.java)
                intent.putExtra("place", place)
                context.startActivity(intent)
            }
            Toast.makeText(context, context.getString(R.string.place_added_to_visit_locations, place.name), Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = places.size

    // Function to calculate distance using Haversine formula (in kilometers)
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Radius of Earth in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}