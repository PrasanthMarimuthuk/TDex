package com.example.tdexv01

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VisitedPlacesAdapter(private val places: List<AddedPlacesActivity.VisitedPlace>) : RecyclerView.Adapter<VisitedPlacesAdapter.VisitedPlaceViewHolder>() {

    class VisitedPlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val visitedPlaceImage: ImageView = itemView.findViewById(R.id.visitedPlaceImage)
        val visitedPlaceName: TextView = itemView.findViewById(R.id.visitedPlaceName)
        val visitedPlaceLocation: TextView = itemView.findViewById(R.id.visitedPlaceLocation)
        val visitedPlaceDate: TextView = itemView.findViewById(R.id.visitedPlaceDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisitedPlaceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.visited_place_item, parent, false)
        return VisitedPlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: VisitedPlaceViewHolder, position: Int) {
        val place = places[position]
        holder.visitedPlaceImage.setImageResource(place.imageRes)
        holder.visitedPlaceName.text = place.name
        holder.visitedPlaceLocation.text = place.location
        holder.visitedPlaceDate.text = "Visited on: ${place.visitDate}"
    }

    override fun getItemCount(): Int = places.size
}