package com.example.tdexv01

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AutocompleteAdapter(
    private var places: List<MainActivity.Place>,
    private val onItemClick: (MainActivity.Place) -> Unit
) : RecyclerView.Adapter<AutocompleteAdapter.AutocompleteViewHolder>() {

    class AutocompleteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val autocompleteText: TextView = itemView.findViewById(R.id.autocompleteText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AutocompleteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.autocomplete_item, parent, false)
        return AutocompleteViewHolder(view)
    }

    override fun onBindViewHolder(holder: AutocompleteViewHolder, position: Int) {
        val place = places[position]
        holder.autocompleteText.text = place.name // Already localized via Place.getAllPlaces()
        holder.itemView.setOnClickListener {
            onItemClick(place)
        }
    }

    override fun getItemCount(): Int = places.size

    fun updatePlaces(newPlaces: List<MainActivity.Place>) {
        places = newPlaces
        notifyDataSetChanged()
    }
}