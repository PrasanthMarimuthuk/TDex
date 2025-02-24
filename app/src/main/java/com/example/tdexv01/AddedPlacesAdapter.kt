package com.example.tdexv01

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tdexv01.MainActivity.Place

class AddedPlacesAdapter(private val places: List<Place>, private val onActionClick: (Int) -> Unit) : RecyclerView.Adapter<AddedPlacesAdapter.AddedPlaceViewHolder>() {

    class AddedPlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val addedPlaceImage: ImageView = itemView.findViewById(R.id.addedPlaceImage)
        val addedPlaceName: TextView = itemView.findViewById(R.id.addedPlaceName)
        val addedPlaceLocation: TextView = itemView.findViewById(R.id.addedPlaceLocation)
        val btnMarkVisited: Button = itemView.findViewById(R.id.btnMarkVisited)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddedPlaceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.added_place_item, parent, false)
        return AddedPlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddedPlaceViewHolder, position: Int) {
        val place = places[position]
        holder.addedPlaceImage.setImageResource(place.image1) // Use first image for preview
        holder.addedPlaceName.text = place.name
        holder.addedPlaceLocation.text = place.location

        // Click on card to open details page with the full Place object
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, TempleDetailActivity::class.java)
            intent.putExtra("place", place) // Pass the entire Place object with all fields
            holder.itemView.context.startActivity(intent)
        }

        holder.btnMarkVisited.setOnClickListener { onActionClick(position) }
        holder.btnDelete.setOnClickListener { onActionClick(position) }
    }

    override fun getItemCount(): Int = places.size
}