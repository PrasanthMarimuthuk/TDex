package com.example.tdexv01

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import com.example.tdexv01.MainActivity.Place
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class TempleDetailActivity : BaseActivity(), TextToSpeech.OnInitListener {

    private lateinit var templeImages: Array<ImageView>
    private lateinit var templeName: TextView
    private lateinit var templeLocation: TextView
    private lateinit var templeDistance: TextView
    private lateinit var templeDescription: TextView
    private lateinit var openingHoursText: TextView
    private lateinit var closingHoursText: TextView
    private lateinit var operatingWeekdaysText: TextView
    private lateinit var btnAdd: Button
    private lateinit var btnDirection: Button
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var voiceButton: ImageView
    private lateinit var chatbotFab: FloatingActionButton
    private lateinit var textToSpeech: TextToSpeech
    private val GEMINI_API_KEY = "AIzaSyDIz1wCg-671yxKnye5P9cGB7Qk_tRPYTg" // Replace with your free Gemini API key
    private val LOCATION_REQUEST_CODE = 1002
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var selectedLanguage: Locale = Locale.ENGLISH // Default to English

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temple_detail)
        supportActionBar?.hide()

        // Initialize UI components
        templeImages = arrayOf(
            findViewById(R.id.templeImage1),
            findViewById(R.id.templeImage2),
            findViewById(R.id.templeImage3),
            findViewById(R.id.templeImage4)
        )
        templeName = findViewById(R.id.templeName)
        templeLocation = findViewById(R.id.templeLocation)
        templeDistance = findViewById(R.id.templeDistance)
        templeDescription = findViewById(R.id.templeDescription)
        openingHoursText = findViewById(R.id.openingHoursText)
        closingHoursText = findViewById(R.id.closingHoursText)
        operatingWeekdaysText = findViewById(R.id.operatingWeekdaysText)
        btnAdd = findViewById(R.id.btnAdd)
        btnDirection = findViewById(R.id.btnDirection)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        voiceButton = findViewById(R.id.voiceButton)
        chatbotFab = findViewById(R.id.chatbotFab)

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Get Data from Intent (using the full Place object)
        val place = intent.getParcelableExtra<Place>("place") ?: MainActivity.Place.getAllPlaces(this).firstOrNull()
        if (place != null) {
            templeName.text = place.name
            templeLocation.text = place.location
            updateDynamicDistance(place) // Update distance dynamically
            templeDescription.text = place.description
            openingHoursText.text = "${getString(R.string.opening_hours)} ${place.openingHours}"
            closingHoursText.text = "${getString(R.string.closing_hours)} ${place.closingHours}"
            operatingWeekdaysText.text = "${getString(R.string.operating_days)} ${place.operatingWeekdays}"

            // Set images
            templeImages[0].setImageResource(place.image1)
            templeImages[1].setImageResource(place.image2)
            templeImages[2].setImageResource(place.image3)
            templeImages[3].setImageResource(place.image4)

            // Set click listeners for each image to expand
            templeImages.forEachIndexed { index, imageView ->
                imageView.setOnClickListener {
                    showFullScreenImage(index, place.image1, place.image2, place.image3, place.image4)
                }
            }
        } else {
            // Fallback if no place is provided
            templeName.text = getString(R.string.unknown_temple)
            templeLocation.text = getString(R.string.unknown_location)
            templeDistance.text = getString(R.string.distance_unavailable)
            templeDescription.text = getString(R.string.no_description)
            openingHoursText.text = "${getString(R.string.opening_hours)} ${getString(R.string.unknown)}"
            closingHoursText.text = "${getString(R.string.closing_hours)} ${getString(R.string.unknown)}"
            operatingWeekdaysText.text = "${getString(R.string.operating_days)} ${getString(R.string.unknown)}"
            templeImages.forEach { it.setImageResource(R.drawable.maruthamalai_1) }
        }

        // Handle Direction Button Click (use phone's default maps app with dynamic location)
        btnDirection.setOnClickListener {
            val location = "${place?.name}, ${place?.location}"
            if (location.isNotEmpty()) {
                val gmmIntentUri = Uri.parse("geo:$currentLatitude,$currentLongitude?q=$location")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps") // Use Google Maps if installed
                if (mapIntent.resolveActivity(packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    // Fallback to default browser-based maps if Google Maps isn't installed
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$location"))
                    startActivity(browserIntent)
                }
            }
        }

        // Handle Add Button Click (navigate to AddedPlacesActivity and add place)
        btnAdd.setOnClickListener {
            val placeToAdd = place ?: MainActivity.Place(
                templeName.text.toString(),
                templeLocation.text.toString(),
                templeDistance.text.toString(),
                templeDescription.text.toString(),
                0.0, 0.0, // Placeholder coordinates
                R.drawable.maruthamalai_1, R.drawable.maruthamalai_2, R.drawable.maruthamalai_3, R.drawable.marudhamalai_4,
                "6:00 AM", // Default opening hours (adjust as needed)
                "8:00 PM", // Default closing hours (adjust as needed)
                "All days" // Default operating weekdays (adjust as needed)
            )
            // Add place to AddedPlacesActivity via MainActivity
            (application as? MainActivity)?.addPlaceToList(placeToAdd) ?: run {
                // Fallback: Start AddedPlacesActivity directly if MainActivity isn’t available
                val intent = Intent(this, AddedPlacesActivity::class.java)
                intent.putExtra("place", placeToAdd) // Pass place as Parcelable
                startActivity(intent)
            }
            Toast.makeText(this, getString(R.string.place_added_to_visit_locations, placeToAdd.name), Toast.LENGTH_SHORT).show()
        }

        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(this, this)

        // Voice Button (Speaker Icon) Click Listener - Show Language Dialog
        voiceButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1006)
            } else {
                showLanguageDialog(place)
            }
        }

        // Chatbot FAB Click Listener (Pass the current Place)
        chatbotFab.setOnClickListener {
            val intent = Intent(this, ChatbotActivity::class.java)
            intent.putExtra("place", place) // Pass the current Place object to ChatbotActivity
            startActivity(intent)
        }

        // Bottom NavigationView Setup (Fixed at Bottom)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.visited -> {
                    startActivity(Intent(this, VisitedPlacesActivity::class.java))
                    Toast.makeText(this, getString(R.string.visited_clicked), Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.tovisit -> {
                    startActivity(Intent(this, AddedPlacesActivity::class.java))
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

        // Request location permission and get current location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
        } else {
            getLocation()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(selectedLanguage)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, getString(R.string.language_not_supported_using_english), Toast.LENGTH_SHORT).show()
                textToSpeech.setLanguage(Locale.US) // Fallback to English
                selectedLanguage = Locale.US
            }
        } else {
            Toast.makeText(this, getString(R.string.tts_initialization_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun playAudioDescription(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private suspend fun getGeminiResponseWithContext(query: String, place: Place): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$GEMINI_API_KEY")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val context = "You are a chatbot for a temple tourism app. Respond with only the name and history or description of the place in English, without any introductory phrases like \\\"Here is the detailed history of...\\\". Use the following information: " +
                    "Place Name: ${place.name}, " +
                    "Location: ${place.location}, " +
                    "Opening Hours: ${place.openingHours}, " +
                    "Closing Hours: ${place.closingHours}, " +
                    "Operating Weekdays: ${place.operatingWeekdays}, " +
                    "Description: ${place.description}, " +
                    "User Query: $query"

            val requestBody = """
                {
                    "contents": [{
                        "parts": [{
                            "text": "$context"
                        }]
                    }],
                    "generationConfig": {
                        "maxOutputTokens": 100,
                        "temperature": 0.7
                    }
                }
            """.trimIndent()

            connection.outputStream.write(requestBody.toByteArray())
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JsonParser.parseString(response).asJsonObject
                val candidates = jsonObject.getAsJsonArray("candidates")
                if (!candidates.isEmpty) {
                    val firstCandidate = candidates[0].asJsonObject
                    val content = firstCandidate.getAsJsonObject("content")
                    val parts = content.getAsJsonArray("parts")
                    if (!parts.isEmpty) {
                        val part = parts[0].asJsonObject
                        return@withContext part.get("text").asString.trim()
                    }
                }
                return@withContext getString(R.string.gemini_could_not_process)
            } else {
                val error = connection.errorStream.bufferedReader().use { it.readText() }
                Log.e("GeminiAPI", "Error: $responseCode - $error")
                return@withContext getString(R.string.gemini_error_processing)
            }
        } catch (e: IOException) {
            Log.e("GeminiAPI", "Network error: ${e.message}")
            return@withContext getString(R.string.network_error)
        }
    }

    // New function to translate text using MyMemory API
    private suspend fun translateText(text: String, targetLang: String): String = withContext(Dispatchers.IO) {
        try {
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val url = URL("https://api.mymemory.translated.net/get?q=$encodedText&langpair=en|$targetLang")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JsonParser.parseString(response).asJsonObject
                val translatedText = jsonObject.getAsJsonObject("responseData")
                    ?.get("translatedText")?.asString
                return@withContext translatedText ?: text // Fallback to original text if translation fails
            } else {
                Log.e("TranslateAPI", "Error: $responseCode")
                return@withContext text // Fallback to original text
            }
        } catch (e: IOException) {
            Log.e("TranslateAPI", "Network error: ${e.message}")
            return@withContext text // Fallback to original text
        }
    }

    private fun showLanguageDialog(place: Place?) {
        val languages = arrayOf("English", "Tamil", "Hindi", "Malayalam")
        AlertDialog.Builder(this)
            .setTitle("Select Language")
            .setSingleChoiceItems(languages, 0) { _, which ->
                selectedLanguage = when (which) {
                    0 -> Locale.ENGLISH
                    1 -> Locale("ta") // Tamil
                    2 -> Locale("hi") // Hindi
                    3 -> Locale("ml") // Malayalam
                    else -> Locale.ENGLISH
                }
            }
            .setPositiveButton("OK") { _, _ ->
                textToSpeech.setLanguage(selectedLanguage)
                if (place != null) {
                    runBlocking {
                        // Get English text from Gemini
                        val geminiQuery = "Provide the name and a concise history or description of ${place.name} located at ${place.location}."
                        val englishResponse = getGeminiResponseWithContext(geminiQuery, place)

                        // Translate to the selected language
                        val targetLangCode = when (selectedLanguage.language) {
                            "en" -> "en" // No translation needed for English
                            "ta" -> "ta"
                            "hi" -> "hi"
                            "ml" -> "ml"
                            else -> "en"
                        }
                        val translatedText = if (targetLangCode == "en") {
                            englishResponse
                        } else {
                            translateText(englishResponse, targetLangCode)
                        }

                        playAudioDescription(translatedText)
                    }
                } else {
                    Toast.makeText(this, getString(R.string.no_place_data_available), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFullScreenImage(index: Int, res1: Int, res2: Int, res3: Int, res4: Int) {
        val imageRes = when (index) {
            0 -> res1
            1 -> res2
            2 -> res3
            3 -> res4
            else -> R.drawable.maruthamalai_1 // Default
        }

        // Create and show a DialogFragment for full-screen image
        val dialog = FullScreenImageDialogFragment.newInstance(imageRes)
        dialog.show(supportFragmentManager, "FullScreenImageDialog")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1006) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val place = intent.getParcelableExtra<Place>("place") ?: MainActivity.Place.getAllPlaces(this).firstOrNull()
                showLanguageDialog(place)
            } else {
                Toast.makeText(this, getString(R.string.microphone_permission_required), Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation()
            } else {
                Toast.makeText(this, getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show()
                // Fallback to static distance if permission is denied
                val place = intent.getParcelableExtra<Place>("place")
                if (place != null) {
                    templeDistance.text = place.staticDistance
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.shutdown()
    }

    private fun getLocation() {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000 // 5 seconds
            fastestInterval = 2000 // 2 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    Log.d("TempleDetailActivity", "Location updated: Lat=${currentLatitude}, Lon=${currentLongitude}")
                    val place = intent.getParcelableExtra<Place>("place")
                    if (place != null) {
                        updateDynamicDistance(place)
                    }
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun updateDynamicDistance(place: Place) {
        val distance = calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude).roundToInt()
        templeDistance.text = "${distance} ${getString(R.string.km)}" // Use localized "km"
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
}

// FullScreenImageDialogFragment remains unchanged
class FullScreenImageDialogFragment : DialogFragment() {
    companion object {
        private const val ARG_IMAGE_RES = "image_res"

        fun newInstance(imageRes: Int): FullScreenImageDialogFragment {
            val fragment = FullScreenImageDialogFragment()
            val args = Bundle()
            args.putInt(ARG_IMAGE_RES, imageRes)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val imageRes = arguments?.getInt(ARG_IMAGE_RES) ?: R.drawable.maruthamalai_1
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_full_screen_image)

        val imageView = dialog.findViewById<ImageView>(R.id.fullScreenImage)
        val closeButton = dialog.findViewById<Button>(R.id.btnClose)

        imageView.setImageResource(imageRes)

        // Handle close button click
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        // Optional: Dismiss on tap anywhere on the screen
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        return dialog
    }
}