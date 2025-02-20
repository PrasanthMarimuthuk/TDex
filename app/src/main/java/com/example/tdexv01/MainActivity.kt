package com.example.tdexv01

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance() // For signed-in state check

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

        val popularPlacesCarousel: LinearLayout = findViewById(R.id.popularPlacesCarousel)

        // List of places in Coimbatore (simplified data structure)
        val places = listOf(
            Place("Maruthamalai Temple", "Maruthamalai, Tamil Nadu", "20 Km", "Maruthamalai temple is Nestled on a serene hill in Coimbatore, the Maruthamalai Murugan Temple is a divine 12th-century marvel dedicated to Lord Murugan. Ascend 837 steps to witness the stunning 180-meter tall gopuram adorned with intricate carvings. This pilgrimage site is famed for its self-manifested idol, medicinal springs, and the vibrant annual Thaipoosam festival. The temple’s breathtaking views and serene atmosphere make it an unmissable spiritual haven for travelers.", R.drawable.maruthamalai_1, R.drawable.maruthamalai_2, R.drawable.maruthamalai_3, R.drawable.marudhamalai_4),
            Place("Isha Yoga Centre", "Coimbatore, Tamil Nadu", "35 Km", "The Isha Yoga Center situated at the foothills of Velliangiri, on the outskirts of Coimbatore, is the headquarters for Isha Foundation. Isha is a sacred space for self-transformation, where you can come dedicate time towards your inner growth. The center offers all four major paths of yoga – kriya (energy), gnana (knowledge), karma (action), and bhakti (devotion), drawing people from all over the world.\n\nThe Center is dedicated to fostering inner transformation and creating an established state of wellbeing in individuals. The large residential facility houses an active international community of brahmacharis, full-time volunteers and visitors. Isha Yoga Center provides a supportive environment for you to shift to healthier lifestyles, seek a higher level of self-fulfillment and realize your full potential.", R.drawable.isha1, R.drawable.isha2, R.drawable.isha3, R.drawable.isha4),
            Place("Velligiri Hills", "Coimbatore, Tamil Nadu", "40 Km", "The Velliangiri Mountains, a part of the Nilgiri Biosphere Reserve, are situated in the Western Ghats of Coimbatore district, Tamil Nadu. Known as the \"Sapthagiri, 7 Hills - Seven Mountains,\" these mountains are revered on par with Mount Kailash, the legendary abode of Shiva, considered one of the most spiritually powerful places on the planet. At the top of the Velliangiri Mountains, Shiva is worshipped as Swayambhu, one who is self-created, and in this form, he graces the devotees.", R.drawable.velliangiri1, R.drawable.velliangiri2, R.drawable.velliangiri3, R.drawable.velliangiri4),
            Place("Gass Forest Museum", "R.S.Puram, Coimbatore", "20 Km", "The Gass Forest Museum, located in Coimbatore, Tamil Nadu, is a captivating natural history museum that was established in 1902. It is named in honor of Horace Archibald Gass, a prominent figure in forestry during that era. The museum serves as a window into the rich biodiversity of the Western Ghats, showcasing an impressive and diverse collection of preserved specimens.\n\nVisitors can explore a variety of exhibits that include plants, animals, and insects native to the region. The museum also offers insightful displays on forestry practices and conservation efforts, highlighting the importance of preserving our natural heritage. For those with a passion for nature, the Gass Forest Museum provides an enriching experience that combines education with admiration for the environment.\n\nWhether you are a nature enthusiast, a student, or simply curious about the natural world, the Gass Forest Museum is a must-visit destination.", R.drawable.gassmus1, R.drawable.gassmus2, R.drawable.gassmus3, R.drawable.gassmuss4),
            Place("Kaviyaruvi Waterfalls", "Aalliyar Reserve Forest, Coimbatore", "20 Km", "Monkey Falls, located near Pollachi in Tamil Nadu, is a natural waterfall nestled in the Anaimalai Hills. The falls are part of the Indira Gandhi Wildlife Sanctuary and National Park. Historically, the area around Monkey Falls was a significant center for tea and coffee plantations during the British colonial era. The falls are named due to the large presence of bonnet macaques in the vicinity. Over time, Monkey Falls has become a popular tourist destination, known for its scenic beauty and refreshing waters. The falls are also referred to as \"Kaviyaruvi,\" a name rooted in ancient Sangam literature.", R.drawable.monkeyfalls1, R.drawable.monkeyfalls2, R.drawable.monkeyfalls3, R.drawable.monkeyfalls4),
            Place("Gedee Museum", "Race Course, Coimbatore", "20 Km", "The Gedee Car Museum in Coimbatore, Tamil Nadu, is a unique attraction showcasing a remarkable collection of vintage and classic cars. Established by the G D Naidu Charities, the museum features rare automobiles from around the world, highlighting the evolution of automotive engineering. Visitors can marvel at the meticulously restored vehicles and learn about the history and innovation behind each model. It's a must-visit for car enthusiasts and history buffs alike.", R.drawable.gdmus1, R.drawable.gdmus2, R.drawable.gdmus3, R.drawable.gdmus4),
            Place("Sathyamangalam Wildlife\n" + "Sanctuary", "Near Coimbatore", "20 Km", "Located at the confluence of the Western and Eastern Ghats in Tamil Nadu, the Sathyamangalam Wildlife Sanctuary is a sprawling wilderness known for its rich biodiversity and stunning landscapes. Spread over 1,411 square kilometers, it is a vital tiger reserve and part of the Nilgiri Biosphere Reserve. The sanctuary is home to a wide variety of flora and fauna, including tigers, elephants, leopards, gaurs, and over 200 species of birds. Its dense forests, rivers, and grasslands make it a paradise for nature enthusiasts, wildlife photographers, and eco-tourists. The sanctuary also holds historical significance, as it was once a route used by the legendary Tipu Sultan.", R.drawable.wildlifesakthi1, R.drawable.wildlifesakthi2, R.drawable.wildlifesakthi3, R.drawable.wildlifesakthi4),
            Place("Government Museum of \n" +"Coimbatore", "Gopalapuram, Coimbatore", "20 Km", "The Government Museum of Coimbatore, located in the heart of the city, is a treasure trove of art, history, and culture. Established in 1975, this museum showcases a diverse collection of artifacts, including ancient sculptures, coins, inscriptions, and traditional handicrafts. It also features exhibits on anthropology, botany, and geology, offering visitors a fascinating insight into Tamil Nadu's rich heritage and natural history. The museum's highlight is its gallery of bronze statues, which reflect the exquisite craftsmanship of the Chola period. A visit to this museum is a journey through time, perfect for history buffs, students, and curious travelers.", R.drawable.govmus1, R.drawable.govmus2, R.drawable.govmus3, R.drawable.govmus4),
            Place("Valparai Hill Station", "Valparai, Coimbatore", "20 Km", "Valparai is Nestled in the lush Anamalai Hills of Tamil Nadu, Valparai is a tranquil hill station known for its breathtaking landscapes, dense tea and coffee plantations, and rich biodiversity. Situated at an altitude of 3,500 feet, it offers a cool climate and stunning views of mist-covered valleys, waterfalls, and wildlife. Valparai is part of the Anamalai Tiger Reserve and is home to rare species like the Nilgiri tahr, lion-tailed macaque, and elephants. Popular attractions include the Sholayar Dam, Aliyar Dam, and the serene Nallamudi Poonjolai viewpoint. Away from the hustle and bustle of city life, Valparai is a perfect retreat for nature lovers and those seeking peace amidst pristine surroundings.", R.drawable.valparai1, R.drawable.valparai2, R.drawable.valparai3, R.drawable.valparai4),
            Place("Perur Pateeswarar Temple", "Perur, Coimbatore", "20 Km", "Perur Pateeswarar Temple is a Hindu temple dedicated to Lord Shiva located at Perur, in the western part of Coimbatore in the state of Tamil Nadu in India. The temple was built by Karikala Chola in the 2nd century CE. The temple is located on the bank of the Noyyal River and has been patronized by poets like Arunagirinathar and Kachiappa Munivar. Patteeswarar (Shiva) is the presiding deity of this temple together with his consort Pachainayaki (Parvati). The main deity is a Swayambu Lingam.", R.drawable.perurpateeswarartemple1, R.drawable.perurpateeswarartemple2, R.drawable.perurpateeswarartemple3, R.drawable.perurpateeswarartemple4)
        )

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
                val intent = Intent(this, TempleDetailActivity::class.java)
                intent.putExtra("temple_name", matchingPlace.name)
                intent.putExtra("temple_location", matchingPlace.location)
                intent.putExtra("temple_distance", matchingPlace.distance)
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
            Toast.makeText(this, "Category: All", Toast.LENGTH_SHORT).show()
        }

        category5kmButton.setOnClickListener {
            selectCategoryButton(category5kmButton, arrayOf(categoryAllButton, category10kmButton, category15kmButton, category20kmButton))
            Toast.makeText(this, "Category: 5 Km", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Category: 20 Km", Toast.LENGTH_SHORT).show()
        }

        // Brahidheerwarar Temple Card Click
        val templeCard1: CardView = findViewById(R.id.BrahidheerwararTemple)
        templeCard1.setOnClickListener {
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("temple_name", "Maruthamalai Temple")
            intent.putExtra("temple_location", "Maruthamalai, Tamil Nadu")
            intent.putExtra("temple_distance", "20 Km")
            intent.putExtra("temple_description", "Maruthamalai temple is Nestled on a serene hill in Coimbatore, the Maruthamalai Murugan Temple is a divine 12th-century marvel dedicated to Lord Murugan. Ascend 837 steps to witness the stunning 180-meter tall gopuram adorned with intricate carvings. This pilgrimage site is famed for its self-manifested idol, medicinal springs, and the vibrant annual Thaipoosam festival. The temple’s breathtaking views and serene atmosphere make it an unmissable spiritual haven for travelers.")
            intent.putExtra("temple_image1", R.drawable.maruthamalai_1)
            intent.putExtra("temple_image2", R.drawable.maruthamalai_2)
            intent.putExtra("temple_image3", R.drawable.maruthamalai_3)
            intent.putExtra("temple_image4", R.drawable.marudhamalai_4)
            startActivity(intent)
        }

        // Isha Yoga Centre Card Click
        val templeCard2: CardView = findViewById(R.id.MeenakshiTemple)
        templeCard2.setOnClickListener {
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("temple_name", "Isha Yoga Centre")
            intent.putExtra("temple_location", "Coimbatore, Tamil Nadu")
            intent.putExtra("temple_distance", "35 Km")
            intent.putExtra("temple_description", "The Isha Yoga Center situated at the foothills of Velliangiri, on the outskirts of Coimbatore, is the headquarters for Isha Foundation. Isha is a sacred space for self-transformation, where you can come dedicate time towards your inner growth. The center offers all four major paths of yoga – kriya (energy), gnana (knowledge), karma (action), and bhakti (devotion), drawing people from all over the world.\n\nThe Center is dedicated to fostering inner transformation and creating an established state of wellbeing in individuals. The large residential facility houses an active international community of brahmacharis, full-time volunteers and visitors. Isha Yoga Center provides a supportive environment for you to shift to healthier lifestyles, seek a higher level of self-fulfillment and realize your full potential.")
            intent.putExtra("temple_image1", R.drawable.isha1)
            intent.putExtra("temple_image2", R.drawable.isha2)
            intent.putExtra("temple_image3", R.drawable.isha3)
            intent.putExtra("temple_image4", R.drawable.isha4)
            startActivity(intent)
        }

        // Velligiri Hills Card Click
        val templeCard3: CardView = findViewById(R.id.Velligirihills)
        templeCard3.setOnClickListener {
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("temple_name", "Velligiri Hills")
            intent.putExtra("temple_location", "Coimbatore, Tamil Nadu")
            intent.putExtra("temple_distance", "40 Km")
            intent.putExtra("temple_description", "The Velliangiri Mountains, a part of the Nilgiri Biosphere Reserve, are situated in the Western Ghats of Coimbatore district, Tamil Nadu. Known as the \"Sapthagiri, 7 Hills - Seven Mountains,\" these mountains are revered on par with Mount Kailash, the legendary abode of Shiva, considered one of the most spiritually powerful places on the planet. At the top of the Velliangiri Mountains, Shiva is worshipped as Swayambhu, one who is self-created, and in this form, he graces the devotees.")
            intent.putExtra("temple_image1", R.drawable.velliangiri1)
            intent.putExtra("temple_image2", R.drawable.velliangiri2)
            intent.putExtra("temple_image3", R.drawable.velliangiri3)
            intent.putExtra("temple_image4", R.drawable.velliangiri4)
            startActivity(intent)
        }

        // Gass Forest Museum Card Click
        val templeCard4: CardView = findViewById(R.id.GassMuseum)
        templeCard4.setOnClickListener {
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("temple_name", "Gass Forest Museum")
            intent.putExtra("temple_location", "R.S.Puram, Coimbatore")
            intent.putExtra("temple_distance", "20 Km")
            intent.putExtra("temple_description", "The Gass Forest Museum, located in Coimbatore, Tamil Nadu, is a captivating natural history museum that was established in 1902. It is named in honor of Horace Archibald Gass, a prominent figure in forestry during that era. The museum serves as a window into the rich biodiversity of the Western Ghats, showcasing an impressive and diverse collection of preserved specimens.\n\nVisitors can explore a variety of exhibits that include plants, animals, and insects native to the region. The museum also offers insightful displays on forestry practices and conservation efforts, highlighting the importance of preserving our natural heritage. For those with a passion for nature, the Gass Forest Museum provides an enriching experience that combines education with admiration for the environment.\n\nWhether you are a nature enthusiast, a student, or simply curious about the natural world, the Gass Forest Museum is a must-visit destination.")
            intent.putExtra("temple_image1", R.drawable.gassmus1)
            intent.putExtra("temple_image2", R.drawable.gassmus2)
            intent.putExtra("temple_image3", R.drawable.gassmus3)
            intent.putExtra("temple_image4", R.drawable.gassmuss4)
            startActivity(intent)
        }

        // Kaviyaruvi Waterfalls Card Click
        val templeCard5: CardView = findViewById(R.id.MoneyFalls)
        templeCard5.setOnClickListener {
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("temple_name", "Kaviyaruvi Waterfalls")
            intent.putExtra("temple_location", "Aalliyar Reserve Forest, Coimbatore")
            intent.putExtra("temple_distance", "20 Km")
            intent.putExtra("temple_description", "Monkey Falls, located near Pollachi in Tamil Nadu, is a natural waterfall nestled in the Anaimalai Hills. The falls are part of the Indira Gandhi Wildlife Sanctuary and National Park. Historically, the area around Monkey Falls was a significant center for tea and coffee plantations during the British colonial era. The falls are named due to the large presence of bonnet macaques in the vicinity. Over time, Monkey Falls has become a popular tourist destination, known for its scenic beauty and refreshing waters. The falls are also referred to as \"Kaviyaruvi,\" a name rooted in ancient Sangam literature.")
            intent.putExtra("temple_image1", R.drawable.monkeyfalls1)
            intent.putExtra("temple_image2", R.drawable.monkeyfalls2)
            intent.putExtra("temple_image3", R.drawable.monkeyfalls3)
            intent.putExtra("temple_image4", R.drawable.monkeyfalls4)
            startActivity(intent)
        }

        // Gedee Museum Card Click
        val templeCard6: CardView = findViewById(R.id.GDmuseum)
        templeCard6.setOnClickListener {
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("temple_name", "Gedee Museum")
            intent.putExtra("temple_location", "Race Course, Coimbatore")
            intent.putExtra("temple_distance", "20 Km")
            intent.putExtra("temple_description", "The Gedee Car Museum in Coimbatore, Tamil Nadu, is a unique attraction showcasing a remarkable collection of vintage and classic cars. Established by the G D Naidu Charities, the museum features rare automobiles from around the world, highlighting the evolution of automotive engineering. Visitors can marvel at the meticulously restored vehicles and learn about the history and innovation behind each model. It's a must-visit for car enthusiasts and history buffs alike.")
            intent.putExtra("temple_image1", R.drawable.gdmus1)
            intent.putExtra("temple_image2", R.drawable.gdmus2)
            intent.putExtra("temple_image3", R.drawable.gdmus3)
            intent.putExtra("temple_image4", R.drawable.gdmus4)
            startActivity(intent)
        }

        // Sathyamangalam Wildlife Sanctuary Card Click
        val templeCard7: CardView = findViewById(R.id.WildlifeSathi)
        templeCard7.setOnClickListener {
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("temple_name", "Sathyamangalam Wildlife \n" +
                    "Sanctuary")
            intent.putExtra("temple_location", "Near Coimbatore")
            intent.putExtra("temple_distance", "20 Km")
            intent.putExtra("temple_description", "Located at the confluence of the Western and Eastern Ghats in Tamil Nadu, the Sathyamangalam Wildlife Sanctuary is a sprawling wilderness known for its rich biodiversity and stunning landscapes. Spread over 1,411 square kilometers, it is a vital tiger reserve and part of the Nilgiri Biosphere Reserve. The sanctuary is home to a wide variety of flora and fauna, including tigers, elephants, leopards, gaurs, and over 200 species of birds. Its dense forests, rivers, and grasslands make it a paradise for nature enthusiasts, wildlife photographers, and eco-tourists. The sanctuary also holds historical significance, as it was once a route used by the legendary Tipu Sultan.")
            intent.putExtra("temple_image1", R.drawable.wildlifesakthi1)
            intent.putExtra("temple_image2", R.drawable.wildlifesakthi2)
            intent.putExtra("temple_image3", R.drawable.wildlifesakthi3)
            intent.putExtra("temple_image4", R.drawable.wildlifesakthi4)
            startActivity(intent)
        }

        // Government Museum of Coimbatore Card Click
        val templeCard8: CardView = findViewById(R.id.GovmentMuseum)
        templeCard8.setOnClickListener {
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("temple_name", "Government Museum of \n" +
                    "Coimbatore")
            intent.putExtra("temple_location", "Gopalapuram, Coimbatore")
            intent.putExtra("temple_distance", "20 Km")
            intent.putExtra("temple_description", "The Government Museum of Coimbatore, located in the heart of the city, is a treasure trove of art, history, and culture. Established in 1975, this museum showcases a diverse collection of artifacts, including ancient sculptures, coins, inscriptions, and traditional handicrafts. It also features exhibits on anthropology, botany, and geology, offering visitors a fascinating insight into Tamil Nadu's rich heritage and natural history. The museum's highlight is its gallery of bronze statues, which reflect the exquisite craftsmanship of the Chola period. A visit to this museum is a journey through time, perfect for history buffs, students, and curious travelers.")
            intent.putExtra("temple_image1", R.drawable.govmus1)
            intent.putExtra("temple_image2", R.drawable.govmus2)
            intent.putExtra("temple_image3", R.drawable.govmus3)
            intent.putExtra("temple_image4", R.drawable.govmus4)
            startActivity(intent)
        }

        // Valparai Hill Station Card Click
        val templeCard9: CardView = findViewById(R.id.Valparai)
        templeCard9.setOnClickListener {
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("temple_name", "Valparai Hill Station")
            intent.putExtra("temple_location", "Valparai, Coimbatore")
            intent.putExtra("temple_distance", "20 Km")
            intent.putExtra("temple_description", "Valparai is Nestled in the lush Anamalai Hills of Tamil Nadu, Valparai is a tranquil hill station known for its breathtaking landscapes, dense tea and coffee plantations, and rich biodiversity. Situated at an altitude of 3,500 feet, it offers a cool climate and stunning views of mist-covered valleys, waterfalls, and wildlife. Valparai is part of the Anamalai Tiger Reserve and is home to rare species like the Nilgiri tahr, lion-tailed macaque, and elephants. Popular attractions include the Sholayar Dam, Aliyar Dam, and the serene Nallamudi Poonjolai viewpoint. Away from the hustle and bustle of city life, Valparai is a perfect retreat for nature lovers and those seeking peace amidst pristine surroundings.")
            intent.putExtra("temple_image1", R.drawable.valparai1)
            intent.putExtra("temple_image2", R.drawable.valparai2)
            intent.putExtra("temple_image3", R.drawable.valparai3)
            intent.putExtra("temple_image4", R.drawable.valparai4)
            startActivity(intent)
        }

        // Perur Pateeswarar Temple Card Click
        val templeCard10: CardView = findViewById(R.id.PerurEeswaran)
        templeCard10.setOnClickListener {
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("temple_name", "Perur Pateeswarar Temple")
            intent.putExtra("temple_location", "Perur, Coimbatore")
            intent.putExtra("temple_distance", "20 Km")
            intent.putExtra("temple_description", "Perur Pateeswarar Temple is a Hindu temple dedicated to Lord Shiva located at Perur, in the western part of Coimbatore in the state of Tamil Nadu in India. The temple was built by Karikala Chola in the 2nd century CE. The temple is located on the bank of the Noyyal River and has been patronized by poets like Arunagirinathar and Kachiappa Munivar. Patteeswarar (Shiva) is the presiding deity of this temple together with his consort Pachainayaki (Parvati). The main deity is a Swayambu Lingam.")
            intent.putExtra("temple_image1", R.drawable.perurpateeswarartemple1)
            intent.putExtra("temple_image2", R.drawable.perurpateeswarartemple2)
            intent.putExtra("temple_image3", R.drawable.perurpateeswarartemple3)
            intent.putExtra("temple_image4", R.drawable.perurpateeswarartemple4)
            startActivity(intent)
        }

        // Bottom NavigationView Setup
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    Toast.makeText(this, "Home Clicked", Toast.LENGTH_SHORT).show()
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
        selectedButton.setTextColor(getColor(R.color.white))
        for (button in otherButtons) {
            button.setBackgroundResource(R.drawable.category_button_background)
            button.setTextColor(getColor(R.color.black))
        }
    }

    // Data class to hold place information
    data class Place(
        val name: String,
        val location: String,
        val distance: String,
        val description: String,
        val image1: Int,
        val image2: Int,
        val image3: Int,
        val image4: Int
    )
}