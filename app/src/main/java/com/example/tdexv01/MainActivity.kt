package com.example.tdexv01

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tdexv01.PlaceAdapter
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import android.os.Parcel
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val LOCATION_REQUEST_CODE = 1001
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private lateinit var placesRecyclerView: RecyclerView
    private lateinit var autocompleteRecyclerView: RecyclerView
    private val places = listOf(

        Place("Maruthamalai Temple", "Maruthamalai, Tamil Nadu", "20 Km", "Maruthamalai temple is Nestled on a serene hill in Coimbatore, the Maruthamalai Murugan Temple is a divine 12th-century marvel dedicated to Lord Murugan. Ascend 837 steps to witness the stunning 180-meter tall gopuram adorned with intricate carvings. This pilgrimage site is famed for its self-manifested idol, medicinal springs, and the vibrant annual Thaipoosam festival. The temple’s breathtaking views and serene atmosphere make it an unmissable spiritual haven for travelers.", 11.0139, 76.8028, R.drawable.maruthamalai_1, R.drawable.maruthamalai_2, R.drawable.maruthamalai_3, R.drawable.marudhamalai_4),
        Place("Isha Yoga Centre", "Coimbatore, Tamil Nadu", "35 Km", "The Isha Yoga Center situated at the foothills of Velliangiri, on the outskirts of Coimbatore, is the headquarters for Isha Foundation. Isha is a sacred space for self-transformation, where you can come dedicate time towards your inner growth. The center offers all four major paths of yoga – kriya (energy), gnana (knowledge), karma (action), and bhakti (devotion), drawing people from all over the world.\n\nThe Center is dedicated to fostering inner transformation and creating an established state of wellbeing in individuals. The large residential facility houses an active international community of brahmacharis, full-time volunteers and visitors. Isha Yoga Center provides a supportive environment for you to shift to healthier lifestyles, seek a higher level of self-fulfillment and realize your full potential.", 10.9711, 76.8259, R.drawable.isha1, R.drawable.isha2, R.drawable.isha3, R.drawable.isha4),
        Place("Velligiri Hills", "Coimbatore, Tamil Nadu", "40 Km", "The Velliangiri Mountains, a part of the Nilgiri Biosphere Reserve, are situated in the Western Ghats of Coimbatore district, Tamil Nadu. Known as the \"Sapthagiri, 7 Hills - Seven Mountains,\" these mountains are revered on par with Mount Kailash, the legendary abode of Shiva, considered one of the most spiritually powerful places on the planet. At the top of the Velliangiri Mountains, Shiva is worshipped as Swayambhu, one who is self-created, and in this form, he graces the devotees.", 10.9722, 76.8317, R.drawable.velliangiri1, R.drawable.velliangiri2, R.drawable.velliangiri3, R.drawable.velliangiri4),
        Place("Gass Forest Museum", "R.S.Puram, Coimbatore", "20 Km", "The Gass Forest Museum, located in Coimbatore, Tamil Nadu, is a captivating natural history museum that was established in 1902. It is named in honor of Horace Archibald Gass, a prominent figure in forestry during that era. The museum serves as a window into the rich biodiversity of the Western Ghats, showcasing an impressive and diverse collection of preserved specimens.\n\nVisitors can explore a variety of exhibits that include plants, animals, and insects native to the region. The museum also offers insightful displays on forestry practices and conservation efforts, highlighting the importance of preserving our natural heritage. For those with a passion for nature, the Gass Forest Museum provides an enriching experience that combines education with admiration for the environment.\n\nWhether you are a nature enthusiast, a student, or simply curious about the natural world, the Gass Forest Museum is a must-visit destination.", 11.0078, 76.9544, R.drawable.gassmus1, R.drawable.gassmus2, R.drawable.gassmus3, R.drawable.gassmuss4),
        Place("Kaviyaruvi Waterfalls", "Aalliyar Reserve Forest, Coimbatore", "20 Km", "Monkey Falls, located near Pollachi in Tamil Nadu, is a natural waterfall nestled in the Anaimalai Hills. The falls are part of the Indira Gandhi Wildlife Sanctuary and National Park. Historically, the area around Monkey Falls was a significant center for tea and coffee plantations during the British colonial era. The falls are named due to the large presence of bonnet macaques in the vicinity. Over time, Monkey Falls has become a popular tourist destination, known for its scenic beauty and refreshing waters. The falls are also referred to as \"Kaviyaruvi,\" a name rooted in ancient Sangam literature.", 10.5783, 76.8639, R.drawable.monkeyfalls1, R.drawable.monkeyfalls2, R.drawable.monkeyfalls3, R.drawable.monkeyfalls4),
        Place("Gedee Museum", "Race Course, Coimbatore", "20 Km", "The Gedee Car Museum in Coimbatore, Tamil Nadu, is a unique attraction showcasing a remarkable collection of vintage and classic cars. Established by the G D Naidu Charities, the museum features rare automobiles from around the world, highlighting the evolution of automotive engineering. Visitors can marvel at the meticulously restored vehicles and learn about the history and innovation behind each model. It's a must-visit for car enthusiasts and history buffs alike.", 11.0156, 76.9583, R.drawable.gdmus1, R.drawable.gdmus2, R.drawable.gdmus3, R.drawable.gdmus4),
        Place("Sathyamangalam \n" + "Wildlife Sanctuary", "Near Coimbatore", "20 Km", "Located at the confluence of the Western and Eastern Ghats in Tamil Nadu, the Sathyamangalam Wildlife Sanctuary is a sprawling wilderness known for its rich biodiversity and stunning landscapes. Spread over 1,411 square kilometers, it is a vital tiger reserve and part of the Nilgiri Biosphere Reserve. The sanctuary is home to a wide variety of flora and fauna, including tigers, elephants, leopards, gaurs, and over 200 species of birds. Its dense forests, rivers, and grasslands make it a paradise for nature enthusiasts, wildlife photographers, and eco-tourists. The sanctuary also holds historical significance, as it was once a route used by the legendary Tipu Sultan.", 11.5064, 77.2411, R.drawable.wildlifesakthi1, R.drawable.wildlifesakthi2, R.drawable.wildlifesakthi3, R.drawable.wildlifesakthi4),
        Place("Government Museum \n" +"of Coimbatore", "Gopalapuram, Coimbatore", "20 Km", "The Government Museum of Coimbatore, located in the heart of the city, is a treasure trove of art, history, and culture. Established in 1975, this museum showcases a diverse collection of artifacts, including ancient sculptures, coins, inscriptions, and traditional handicrafts. It also features exhibits on anthropology, botany, and geology, offering visitors a fascinating insight into Tamil Nadu's rich heritage and natural history. The museum's highlight is its gallery of bronze statues, which reflect the exquisite craftsmanship of the Chola period. A visit to this museum is a journey through time, perfect for history buffs, students, and curious travelers.", 11.0139, 76.9611, R.drawable.govmus1, R.drawable.govmus2, R.drawable.govmus3, R.drawable.govmus4),
        Place("Valparai Hill Station", "Valparai, Coimbatore", "20 Km", "Valparai is Nestled in the lush Anamalai Hills of Tamil Nadu, Valparai is a tranquil hill station known for its breathtaking landscapes, dense tea and coffee plantations, and rich biodiversity. Situated at an altitude of 3,500 feet, it offers a cool climate and stunning views of mist-covered valleys, waterfalls, and wildlife. Valparai is part of the Anamalai Tiger Reserve and is home to rare species like the Nilgiri tahr, lion-tailed macaque, and elephants. Popular attractions include the Sholayar Dam, Aliyar Dam, and the serene Nallamudi Poonjolai viewpoint. Away from the hustle and bustle of city life, Valparai is a perfect retreat for nature lovers and those seeking peace amidst pristine surroundings.", 10.3236, 76.9478, R.drawable.valparai1, R.drawable.valparai2, R.drawable.valparai3, R.drawable.valparai4),
        Place("Perur Pateeswarar Temple", "Perur, Coimbatore", "20 Km", "Perur Pateeswarar Temple is a Hindu temple dedicated to Lord Shiva located at Perur, in the western part of Coimbatore in the state of Tamil Nadu in India. The temple was built by Karikala Chola in the 2nd century CE. The temple is located on the bank of the Noyyal River and has been patronized by poets like Arunagirinathar and Kachiappa Munivar. Patteeswarar (Shiva) is the presiding deity of this temple together with his consort Pachainayaki (Parvati). The main deity is a Swayambu Lingam.", 10.9944, 76.9289, R.drawable.perurpateeswarartemple1, R.drawable.perurpateeswarartemple2, R.drawable.perurpateeswarartemple3, R.drawable.perurpateeswarartemple4)
    )
    private val addedPlaces = mutableListOf<Place>()
    private lateinit var autocompleteAdapter: AutocompleteAdapter
    private var currentCategoryFilter: ((Place) -> Boolean)? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check if user is signed in; if not, redirect to SignInActivity
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        val searchEditText: EditText = findViewById(R.id.searchEditText)
        val searchButton: ImageView = findViewById(R.id.searchButton)
        val categoryAllButton: Button = findViewById(R.id.categoryAllButton)
        val category5kmButton: Button = findViewById(R.id.category5kmButton)
        val category10kmButton: Button = findViewById(R.id.category10kmButton)
        val category15kmButton: Button = findViewById(R.id.category15kmButton)
        val category20kmButton: Button = findViewById(R.id.category20kmButton)
        val noPlacesText: TextView = findViewById(R.id.noPlacesTextView)

        placesRecyclerView = findViewById(R.id.placesRecyclerView)
        placesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        placesRecyclerView.setHasFixedSize(true)

        autocompleteRecyclerView = findViewById(R.id.autocompleteRecyclerView) // Add this ID to activity_main.xml if needed
        autocompleteRecyclerView.layoutManager = LinearLayoutManager(this)
        autocompleteRecyclerView.setHasFixedSize(true)
        autocompleteAdapter = AutocompleteAdapter(emptyList()) { selectedPlace ->
            // Handle item click in autocomplete (open TempleDetailActivity)
            val distance = calculateDistance(currentLatitude, currentLongitude, selectedPlace.latitude, selectedPlace.longitude)
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("temple_name", selectedPlace.name)
            intent.putExtra("temple_location", selectedPlace.location)
            intent.putExtra("temple_distance", "${String.format("%.1f", distance)} Km")
            intent.putExtra("temple_description", selectedPlace.description)
            intent.putExtra("temple_image1", selectedPlace.image1)
            intent.putExtra("temple_image2", selectedPlace.image2)
            intent.putExtra("temple_image3", selectedPlace.image3)
            intent.putExtra("temple_image4", selectedPlace.image4)
            startActivity(intent)
            searchEditText.text.clear() // Clear search text after selection
        }
        autocompleteRecyclerView.adapter = autocompleteAdapter


        val popularPlacesCarousel: LinearLayout = findViewById(R.id.popularPlacesCarousel)

        // Request location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
        } else {
            getLocation()
        }

        updatePlacesList(places)

        // Autocomplete listener for search bar
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim()?.lowercase() ?: ""
                if (query.isNotEmpty()) {
                    val filteredPlaces = places.filter { it.name.lowercase().contains(query) }
                    autocompleteAdapter.updatePlaces(filteredPlaces)
                    autocompleteRecyclerView.visibility = if (filteredPlaces.isNotEmpty()) View.VISIBLE else View.GONE
                } else {
                    autocompleteRecyclerView.visibility = View.GONE
                }
            }
        })


        // Search functionality
        searchButton.setOnClickListener {
            val searchText = searchEditText.text.toString().trim().lowercase()
            if (searchText.isEmpty()) {
                Toast.makeText(this, "Please enter a place name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val matchingPlace = places.find { place ->
                place.name.lowercase().contains(searchText) ||
                        place.location.lowercase().contains(searchText)
            }

            if (matchingPlace != null) {
                val distance = calculateDistance(currentLatitude, currentLongitude, matchingPlace.latitude, matchingPlace.longitude)
                val intent = Intent(this, TempleDetailActivity::class.java)
                intent.putExtra("temple_name", matchingPlace.name)
                intent.putExtra("temple_location", matchingPlace.location)
                intent.putExtra("temple_distance", "${String.format("%.1f", distance)} Km")
                intent.putExtra("temple_description", matchingPlace.description)
                intent.putExtra("temple_image1", matchingPlace.image1)
                intent.putExtra("temple_image2", matchingPlace.image2)
                intent.putExtra("temple_image3", matchingPlace.image3)
                intent.putExtra("temple_image4", matchingPlace.image4)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Place not found: $searchText", Toast.LENGTH_SHORT).show()
            }
        }

        categoryAllButton.setOnClickListener {
            selectCategoryButton(categoryAllButton, arrayOf(category5kmButton, category10kmButton, category15kmButton, category20kmButton))
            if (places.isNotEmpty()) {
                updatePlacesList(places)
                noPlacesText.visibility = View.GONE
                placesRecyclerView.visibility = View.VISIBLE
            } else {
                noPlacesText.visibility = View.VISIBLE
                placesRecyclerView.visibility = View.GONE
            }

            currentCategoryFilter = null
            updatePlacesList(places)
            Toast.makeText(this, "Category: All", Toast.LENGTH_SHORT).show()
        }

        category5kmButton.setOnClickListener {
            selectCategoryButton(category5kmButton, arrayOf(categoryAllButton, category10kmButton, category15kmButton, category20kmButton))
            val filteredPlaces = places.filter { place ->
                val distance = calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude)
                distance <= 5
            }
            if (filteredPlaces.isNotEmpty()) {
                updatePlacesList(filteredPlaces)
                noPlacesText.visibility = View.GONE
                placesRecyclerView.visibility = View.VISIBLE
            } else {
                noPlacesText.visibility = View.VISIBLE
                placesRecyclerView.visibility = View.GONE
            }
            Toast.makeText(this, "Category: 5 Km", Toast.LENGTH_SHORT).show()
            currentCategoryFilter = { place -> calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude) <= 5 }
            updatePlacesList(places.filter { currentCategoryFilter?.invoke(it) ?: true })
        }

        category10kmButton.setOnClickListener {
            selectCategoryButton(category10kmButton, arrayOf(categoryAllButton, category5kmButton, category15kmButton, category20kmButton))
            val filteredPlaces = places.filter { place ->
                val distance = calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude)
                distance <= 10
            }
            if (filteredPlaces.isNotEmpty()) {
                updatePlacesList(filteredPlaces)
                noPlacesText.visibility = View.GONE
                placesRecyclerView.visibility = View.VISIBLE
            } else {
                noPlacesText.visibility = View.VISIBLE
                placesRecyclerView.visibility = View.GONE
            }

            Toast.makeText(this, "Category: 10 Km", Toast.LENGTH_SHORT).show()
            currentCategoryFilter = { place -> calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude) <= 10 }
            updatePlacesList(places.filter { currentCategoryFilter?.invoke(it) ?: true })
        }

        category15kmButton.setOnClickListener {
            selectCategoryButton(category15kmButton, arrayOf(categoryAllButton, category5kmButton, category10kmButton, category20kmButton))
            val filteredPlaces = places.filter { place ->
                val distance = calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude)
                distance <= 15
            }
            if (filteredPlaces.isNotEmpty()) {
                updatePlacesList(filteredPlaces)
                noPlacesText.visibility = View.GONE
                placesRecyclerView.visibility = View.VISIBLE
            } else {
                noPlacesText.visibility = View.VISIBLE
                placesRecyclerView.visibility = View.GONE
            }

            Toast.makeText(this, "Category: 15 Km", Toast.LENGTH_SHORT).show()
            currentCategoryFilter = { place -> calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude) <= 15 }
            updatePlacesList(places.filter { currentCategoryFilter?.invoke(it) ?: true })
        }

        category20kmButton.setOnClickListener {
            selectCategoryButton(category20kmButton, arrayOf(categoryAllButton, category5kmButton, category10kmButton, category15kmButton))
             val filteredPlaces = places.filter { place ->
                 val distance = calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude)
                 distance <= 20
             }
            if (filteredPlaces.isNotEmpty()) {
                updatePlacesList(filteredPlaces)
                noPlacesText.visibility = View.GONE
                placesRecyclerView.visibility = View.VISIBLE
            } else {
                noPlacesText.visibility = View.VISIBLE
                placesRecyclerView.visibility = View.GONE
            }
            Toast.makeText(this, "Category: 20 Km", Toast.LENGTH_SHORT).show()
            currentCategoryFilter = { place -> calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude) <= 20 }
            updatePlacesList(places.filter { currentCategoryFilter?.invoke(it) ?: true })
        }

        // Brahidheerwarar Temple Card Click
        val templeCard1: CardView = findViewById(R.id.BrahidheerwararTemple)
        templeCard1.setOnClickListener {
            val place = places[0] // Maruthamalai Temple
            val distance = calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude)
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("temple_name", place.name)
            intent.putExtra("temple_location", place.location)
            intent.putExtra("temple_distance", "${String.format("%.1f", distance)} Km")
            intent.putExtra("temple_description", place.description)
            intent.putExtra("temple_image1", place.image1)
            intent.putExtra("temple_image2", place.image2)
            intent.putExtra("temple_image3", place.image3)
            intent.putExtra("temple_image4", place.image4)
            startActivity(intent)
        }

        // Isha Yoga Centre Card Click
        val templeCard2: CardView = findViewById(R.id.MeenakshiTemple)
        templeCard2.setOnClickListener {
            val place = places[1] // Isha Yoga Centre
            val distance = calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude)
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("temple_name", place.name)
            intent.putExtra("temple_location", place.location)
            intent.putExtra("temple_distance", "${String.format("%.1f", distance)} Km")
            intent.putExtra("temple_description", place.description)
            intent.putExtra("temple_image1", place.image1)
            intent.putExtra("temple_image2", place.image2)
            intent.putExtra("temple_image3", place.image3)
            intent.putExtra("temple_image4", place.image4)
            startActivity(intent)
        }

        // Velligiri Hills Card Click
        val templeCard3: CardView = findViewById(R.id.Velligirihills)
        templeCard3.setOnClickListener {
            val place = places[2] // Velligiri Hills
            val distance = calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude)
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("temple_name", place.name)
            intent.putExtra("temple_location", place.location)
            intent.putExtra("temple_distance", "${String.format("%.1f", distance)} Km")
            intent.putExtra("temple_description", place.description)
            intent.putExtra("temple_image1", place.image1)
            intent.putExtra("temple_image2", place.image2)
            intent.putExtra("temple_image3", place.image3)
            intent.putExtra("temple_image4", place.image4)
            startActivity(intent)
        }

        // Gass Forest Museum Card Click
        val templeCard4: CardView = findViewById(R.id.GassMuseum)
        templeCard4.setOnClickListener {
            val place = places[3] // Gass Forest Museum
            val distance = calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude)
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("temple_name", place.name)
            intent.putExtra("temple_location", place.location)
            intent.putExtra("temple_distance", "${String.format("%.1f", distance)} Km")
            intent.putExtra("temple_description", place.description)
            intent.putExtra("temple_image1", place.image1)
            intent.putExtra("temple_image2", place.image2)
            intent.putExtra("temple_image3", place.image3)
            intent.putExtra("temple_image4", place.image4)
            startActivity(intent)
        }

        // Kaviyaruvi Waterfalls Card Click
        val templeCard5: CardView = findViewById(R.id.MoneyFalls)
        templeCard5.setOnClickListener {
            val place = places[4] // Kaviyaruvi Waterfalls
            val distance = calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude)
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("temple_name", place.name)
            intent.putExtra("temple_location", place.location)
            intent.putExtra("temple_distance", "${String.format("%.1f", distance)} Km")
            intent.putExtra("temple_description", place.description)
            intent.putExtra("temple_image1", place.image1)
            intent.putExtra("temple_image2", place.image2)
            intent.putExtra("temple_image3", place.image3)
            intent.putExtra("temple_image4", place.image4)
            startActivity(intent)
        }

        // Gedee Museum Card Click
        val templeCard6: CardView = findViewById(R.id.GDmuseum)
        templeCard6.setOnClickListener {
            val place = places[5] // Gedee Museum
            val distance = calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude)
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("temple_name", place.name)
            intent.putExtra("temple_location", place.location)
            intent.putExtra("temple_distance", "${String.format("%.1f", distance)} Km")
            intent.putExtra("temple_description", place.description)
            intent.putExtra("temple_image1", place.image1)
            intent.putExtra("temple_image2", place.image2)
            intent.putExtra("temple_image3", place.image3)
            intent.putExtra("temple_image4", place.image4)
            startActivity(intent)
        }

        // Sathyamangalam Wildlife Sanctuary Card Click
        val templeCard7: CardView = findViewById(R.id.WildlifeSathi)
        templeCard7.setOnClickListener {
            val place = places[6] // Sathyamangalam Wildlife Sanctuary
            val distance = calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude)
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("temple_name", place.name)
            intent.putExtra("temple_location", place.location)
            intent.putExtra("temple_distance", "${String.format("%.1f", distance)} Km")
            intent.putExtra("temple_description", place.description)
            intent.putExtra("temple_image1", place.image1)
            intent.putExtra("temple_image2", place.image2)
            intent.putExtra("temple_image3", place.image3)
            intent.putExtra("temple_image4", place.image4)
            startActivity(intent)
        }

        // Government Museum of Coimbatore Card Click
        val templeCard8: CardView = findViewById(R.id.GovmentMuseum)
        templeCard8.setOnClickListener {
            val place = places[7] // Government Museum of Coimbatore
            val distance = calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude)
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("temple_name", place.name)
            intent.putExtra("temple_location", place.location)
            intent.putExtra("temple_distance", "${String.format("%.1f", distance)} Km")
            intent.putExtra("temple_description", place.description)
            intent.putExtra("temple_image1", place.image1)
            intent.putExtra("temple_image2", place.image2)
            intent.putExtra("temple_image3", place.image3)
            intent.putExtra("temple_image4", place.image4)
            startActivity(intent)
        }

        // Valparai Hill Station Card Click
        val templeCard9: CardView = findViewById(R.id.Valparai)
        templeCard9.setOnClickListener {
            val place = places[8] // Valparai Hill Station
            val distance = calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude)
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("temple_name", place.name)
            intent.putExtra("temple_location", place.location)
            intent.putExtra("temple_distance", "${String.format("%.1f", distance)} Km")
            intent.putExtra("temple_description", place.description)
            intent.putExtra("temple_image1", place.image1)
            intent.putExtra("temple_image2", place.image2)
            intent.putExtra("temple_image3", place.image3)
            intent.putExtra("temple_image4", place.image4)
            startActivity(intent)
        }

        // Perur Pateeswarar Temple Card Click
        val templeCard10: CardView = findViewById(R.id.PerurEeswaran)
        templeCard10.setOnClickListener {
            val place = places[9] // Perur Pateeswarar Temple
            val distance = calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude)
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("temple_name", place.name)
            intent.putExtra("temple_location", place.location)
            intent.putExtra("temple_distance", "${String.format("%.1f", distance)} Km")
            intent.putExtra("temple_description", place.description)
            intent.putExtra("temple_image1", place.image1)
            intent.putExtra("temple_image2", place.image2)
            intent.putExtra("temple_image3", place.image3)
            intent.putExtra("temple_image4", place.image4)
            startActivity(intent)
        }


        // Bottom NavigationView Setup
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.menu.findItem(R.id.home).setChecked(true)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    Toast.makeText(this, "Home Clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.visited -> {
                    startActivity(Intent(this, VisitedPlacesActivity::class.java))
                    Toast.makeText(this, "Visited Clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.tovisit -> {
                    Toast.makeText(this, " To Visit Clicked", Toast.LENGTH_SHORT).show()
                    // Navigate to AddedPlacesActivity (list of added places)
                    startActivity(Intent(this, AddedPlacesActivity::class.java))
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
        selectedButton.setTextColor(getColor(R.color.white))
        for (button in otherButtons) {
            button.setBackgroundResource(R.drawable.category_button_background)
            button.setTextColor(getColor(R.color.black))
        }
    }

    private fun updatePlacesList(filteredPlaces: List<Place>) {
        // Check if a category filter is applied and apply it
        val placesToDisplay = currentCategoryFilter?.let { filter ->
            filteredPlaces.filter(filter)
        } ?: filteredPlaces
        val adapter = PlaceAdapter(placesToDisplay, currentLatitude, currentLongitude, this)
        placesRecyclerView.adapter = adapter
    }

    // Data class to hold place information with latitude and longitude
    data class Place(
        val name: String,
        val location: String,
        val staticDistance: String, // Keep static distance for reference
        val description: String,
        val latitude: Double, // Destination latitude
        val longitude: Double, // Destination longitude
        val image1: Int,
        val image2: Int,
        val image3: Int,
        val image4: Int
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readDouble(),
            parcel.readDouble(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(name)
            parcel.writeString(location)
            parcel.writeString(staticDistance)
            parcel.writeString(description)
            parcel.writeDouble(latitude)
            parcel.writeDouble(longitude)
            parcel.writeInt(image1)
            parcel.writeInt(image2)
            parcel.writeInt(image3)
            parcel.writeInt(image4)
        }

        override fun describeContents(): Int = 0

        companion object CREATOR : Parcelable.Creator<Place> {
            override fun createFromParcel(parcel: Parcel): Place = Place(parcel)
            override fun newArray(size: Int): Array<Place?> = arrayOfNulls(size)
        }
    }


    private fun getLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = LocationRequest.create().apply {
            interval = 5000 // 5 seconds
            fastestInterval = 2000 // 2seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    Log.d("MainActivity", "Location updated: Lat=${currentLatitude}, Lon=${currentLongitude}")
                     val filteredPlaces = currentCategoryFilter?.let { filter ->
                         places.filter(filter) } ?: places
                         updatePlacesList(filteredPlaces)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    // Calculate distance using Haversine formula (in kilometers)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation()
            } else {
                Toast.makeText(this, "Location permission denied. Distances will be static.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun addPlaceToList(place: Place) {
        (application as? AddedPlacesActivity)?.addPlace(place) ?: run {
            // Fallback: Start AddedPlacesActivity to add the place
            val intent = Intent(this, AddedPlacesActivity::class.java)
            intent.putExtra("place", place) // Pass place as a serializable parcelable (update Place to implement Parcelable if needed)
            startActivityForResult(intent, 1002)
        }
        Toast.makeText(this, "${place.name} added to Visit Locations", Toast.LENGTH_SHORT).show()
    }
}

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
        holder.autocompleteText.text = place.name
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