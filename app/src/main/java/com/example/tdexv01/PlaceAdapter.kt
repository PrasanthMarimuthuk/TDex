package com.example.tdexv01 // Match your project's package name

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class PlaceAdapter(
    private val places: List<MainActivity.Place>,
    private val currentLatitude: Double,
    private val currentLongitude: Double,
    private val context: MainActivity
) : RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>() {

    class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeImage: ImageView = itemView.findViewById(R.id.placeImage)
        val placeName: TextView = itemView.findViewById(R.id.placeName)
        val placeLocation: TextView = itemView.findViewById(R.id.placeLocation)
        val placeDistance: TextView = itemView.findViewById(R.id.placeDistance)
        val btnAddToMap: Button = itemView.findViewById(R.id.btnAddToMap)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.place_card_item, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = places[position]
        val distance = calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude)

        holder.placeImage.setImageResource(place.image1) // Use first image for preview
        holder.placeName.text = place.name
        holder.placeLocation.text = place.location
        holder.placeDistance.text = "${String.format("%.1f", distance)} Km"

        // Click on card to open details page
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, TempleDetailActivity::class.java)
            intent.putExtra("temple_name", place.name)
            intent.putExtra("temple_location", place.location)
            intent.putExtra("temple_distance", "${String.format("%.1f", distance)} Km")
            intent.putExtra("temple_description", place.description)
            intent.putExtra("temple_image1", place.image1)
            intent.putExtra("temple_image2", place.image2)
            intent.putExtra("temple_image3", place.image3)
            intent.putExtra("temple_image4", place.image4)
            holder.itemView.context.startActivity(intent)
        }

        // Click on "Add" button to add to list in AddedPlacesActivity
        holder.btnAddToMap.setOnClickListener {
            context.addPlaceToList(place)
            // Optionally, update AddedPlacesActivity (not implemented here as it’s a separate activity)
        }
    }

    override fun getItemCount(): Int = places.size

    // Use the existing Haversine formula from MainActivity
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