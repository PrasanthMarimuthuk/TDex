package com.example.tdexv01

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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
    lateinit var textToSpeech: TextToSpeech
    private val GEMINI_API_KEY = "AIzaSyDIz1wCg-671yxKnye5P9cGB7Qk_tRPYTg" // Replace with your free Gemini API key
    private val LOCATION_REQUEST_CODE = 1002
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var selectedLanguage: Locale = Locale("en", "IN") // Default to Indian English
    var isSpeaking = false // Track if TTS is currently speaking

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
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

        // Get Data from Intent
        val place: MainActivity.Place? = intent.extras?.getParcelable("place", MainActivity.Place::class.java)
            ?: MainActivity.Place.getAllPlaces(this).firstOrNull()
        if (place != null) {
            templeName.text = place.name
            templeLocation.text = place.location // Use localized location for display
            updateDynamicDistance(place)
            templeDescription.text = place.description
            openingHoursText.text = "${getString(R.string.opening_hours)} ${place.openingHours}"
            closingHoursText.text = "${getString(R.string.closing_hours)} ${place.closingHours}"
            operatingWeekdaysText.text = "${getString(R.string.operating_days)} ${place.operatingWeekdays}"

            templeImages[0].setImageResource(place.image1)
            templeImages[1].setImageResource(place.image2)
            templeImages[2].setImageResource(place.image3)
            templeImages[3].setImageResource(place.image4)

            templeImages.forEachIndexed { index, imageView ->
                imageView.setOnClickListener {
                    showFullScreenImage(index, place.image1, place.image2, place.image3, place.image4)
                }
            }
        } else {
            templeName.text = getString(R.string.unknown_temple)
            templeLocation.text = getString(R.string.unknown_location)
            templeDistance.text = getString(R.string.distance_unavailable)
            templeDescription.text = getString(R.string.no_description)
            openingHoursText.text = "${getString(R.string.opening_hours)} ${getString(R.string.unknown)}"
            closingHoursText.text = "${getString(R.string.closing_hours)} ${getString(R.string.unknown)}"
            operatingWeekdaysText.text = "${getString(R.string.operating_days)} ${getString(R.string.unknown)}"
            templeImages.forEach { it.setImageResource(android.R.drawable.ic_menu_info_details) } // Fallback
        }

        // Direction Button
        btnDirection.setOnClickListener {
            val location = "${place?.name}, ${place?.locationEnglish}" // Use locationEnglish for maps query
            if (location.isNotEmpty()) {
                val gmmIntentUri = Uri.parse("geo:$currentLatitude,$currentLongitude?q=$location")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                if (mapIntent.resolveActivity(packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$location"))
                    startActivity(browserIntent)
                }
            }
        }

        // Add Button
        btnAdd.setOnClickListener {
            val placeToAdd = place?.let {
                MainActivity.Place(
                    name = it.name,
                    locationEnglish = it.locationEnglish,
                    location = it.location,
                    staticDistance = it.staticDistance,
                    description = it.description,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    image1 = it.image1,
                    image2 = it.image2,
                    image3 = it.image3,
                    image4 = it.image4,
                    openingHours = it.openingHours,
                    closingHours = it.closingHours,
                    operatingWeekdays = it.operatingWeekdays
                )
            } ?: MainActivity.Place(
                name = templeName.text.toString(),
                locationEnglish = templeLocation.text.toString(),
                location = templeLocation.text.toString(),
                staticDistance = templeDistance.text.toString(),
                description = templeDescription.text.toString(),
                latitude = 0.0,
                longitude = 0.0,
                image1 = android.R.drawable.ic_menu_info_details,
                image2 = android.R.drawable.ic_menu_info_details,
                image3 = android.R.drawable.ic_menu_info_details,
                image4 = android.R.drawable.ic_menu_info_details,
                openingHours = "6:00 AM",
                closingHours = "8:00 PM",
                operatingWeekdays = "All days"
            )
            val intent = Intent(this, AddedPlacesActivity::class.java)
            intent.putExtra("place", placeToAdd)
            startActivity(intent)
            Toast.makeText(this, getString(R.string.place_added_to_visit_locations, placeToAdd.name), Toast.LENGTH_SHORT).show()
        }

        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(this, this)

        // Voice Button - Toggle play/stop
        voiceButton.setOnClickListener {
            if (isSpeaking) {
                textToSpeech.stop()
                isSpeaking = false
                Toast.makeText(this, getString(R.string.audio_stopped), Toast.LENGTH_SHORT).show()
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1006)
            } else {
                showLanguageDialog(place)
            }
        }

        // Chatbot FAB
        chatbotFab.setOnClickListener {
            val intent = Intent(this, ChatbotActivity::class.java)
            intent.putExtra("place", place)
            startActivity(intent)
        }

        // Bottom Navigation
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    startActivity(Intent(this, MainActivity::class.java))
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
                    startActivity(Intent(this, ProfileActivity::class.java))
                    Toast.makeText(this, getString(R.string.profile_clicked), Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        // Request location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
        } else {
            getLocation()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val prefs = getSharedPreferences("Settings", MODE_PRIVATE)
            val selectedLanguageCode = prefs.getString("Language", "en") ?: "en"
            selectedLanguage = when (selectedLanguageCode) {
                "ta" -> Locale("ta", "IN") // Tamil
                "hi" -> Locale("hi", "IN") // Hindi
                "ml" -> Locale("ml", "IN") // Malayalam
                "fr" -> Locale("fr", "FR") // French
                "es" -> Locale("es", "ES") // Spanish
                "ja" -> Locale("ja", "JP") // Japanese
                "ko" -> Locale("ko", "KR") // Korean
                "ms" -> Locale("ms", "MY") // Malay
                "te" -> Locale("te", "IN") // Telugu
                "mr" -> Locale("mr", "IN") // Marathi
                "de" -> Locale("de", "DE") // German
                "kn" -> Locale("kn", "IN") // Kannada
                else -> Locale("en", "US") // English
            }
            val result = textToSpeech.setLanguage(selectedLanguage)
            textToSpeech.setPitch(0.8f) // Male-like voice
            textToSpeech.setSpeechRate(1.0f)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language $selectedLanguageCode not supported for TTS, falling back to English", Toast.LENGTH_SHORT).show()
                textToSpeech.setLanguage(Locale("en", "US"))
                selectedLanguage = Locale("en", "US")
            }
        } else {
            Toast.makeText(this, getString(R.string.tts_initialization_failed), Toast.LENGTH_SHORT).show()
        }
    }

    fun playAudioDescription(text: String) {
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        isSpeaking = true
        textToSpeech.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                isSpeaking = false
            }
            override fun onError(utteranceId: String?) {
                isSpeaking = false
            }
        })
    }

    private suspend fun getGeminiResponseWithContext(query: String, place: MainActivity.Place): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$GEMINI_API_KEY")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val context = "You are a chatbot for a temple tourism app. Respond with only the name and history or description of the place in English, without any introductory phrases. Use the following information: " +
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
                return@withContext translatedText ?: text
            } else {
                Log.e("TranslateAPI", "Error: $responseCode")
                return@withContext text
            }
        } catch (e: IOException) {
            Log.e("TranslateAPI", "Network error: ${e.message}")
            return@withContext text
        }
    }

    private fun isOnline(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun showLanguageDialog(place: MainActivity.Place?) {
        val languages = arrayOf(
            "English", "Tamil", "Hindi", "Malayalam", "French", "Spanish", "Japanese", "Korean",
            "Malay", "Telugu", "Marathi", "Kannada"
        )
        val languageCodes = arrayOf(
            "en", "ta", "hi", "ml", "fr", "es", "ja", "ko", "ms", "te", "mr", "de", "kn"
        )
        val prefs = getSharedPreferences("Settings", MODE_PRIVATE)
        val currentLanguageCode = prefs.getString("Language", "en") ?: "en"
        var selectedIndex = languageCodes.indexOf(currentLanguageCode).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_language))
            .setSingleChoiceItems(languages, selectedIndex) { _, which ->
                selectedIndex = which
                selectedLanguage = when (languageCodes[which]) {
                    "ta" -> Locale("ta", "IN")
                    "hi" -> Locale("hi", "IN")
                    "ml" -> Locale("ml", "IN")
                    "fr" -> Locale("fr", "FR")
                    "es" -> Locale("es", "ES")
                    "ja" -> Locale("ja", "JP")
                    "ko" -> Locale("ko", "KR")
                    "ms" -> Locale("ms", "MY")
                    "te" -> Locale("te", "IN")
                    "mr" -> Locale("mr", "IN")
                    "de" -> Locale("de", "DE")
                    "kn" -> Locale("kn", "IN")
                    else -> Locale("en", "US")
                }
            }
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                textToSpeech.setLanguage(selectedLanguage)
                if (place != null) {
                    runBlocking {
                        val descriptionText: String = if (isOnline()) {
                            val geminiQuery = "Provide the name and a concise history or description of ${place.name} located at ${place.location}."
                            getGeminiResponseWithContext(geminiQuery, place)
                        } else {
                            place.description
                        }

                        val targetLangCode = when (selectedLanguage.language) {
                            "ta" -> "ta"
                            "hi" -> "hi"
                            "ml" -> "ml"
                            "fr" -> "fr"
                            "es" -> "es"
                            "ja" -> "ja"
                            "ko" -> "ko"
                            "ms" -> "ms"
                            "te" -> "te"
                            "mr" -> "mr"
                            "de" -> "de"
                            "kn" -> "kn"
                            else -> "en"
                        }
                        val translatedText = if (targetLangCode == "en" || !isOnline()) {
                            descriptionText
                        } else {
                            translateText(descriptionText, targetLangCode)
                        }

                        playAudioDescription(translatedText)
                    }
                } else {
                    Toast.makeText(this, getString(R.string.no_place_data_available), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showFullScreenImage(index: Int, res1: Int, res2: Int, res3: Int, res4: Int) {
        val imageRes = when (index) {
            0 -> res1
            1 -> res2
            2 -> res3
            3 -> res4
            else -> android.R.drawable.ic_menu_info_details
        }

        val dialog = FullScreenImageDialogFragment.newInstance(imageRes)
        dialog.show(supportFragmentManager, "FullScreenImageDialog")
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1006) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val place = intent.extras?.getParcelable("place", MainActivity.Place::class.java)
                    ?: MainActivity.Place.getAllPlaces(this).firstOrNull()
                showLanguageDialog(place)
            } else {
                Toast.makeText(this, getString(R.string.microphone_permission_required), Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation()
            } else {
                Toast.makeText(this, getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show()
                val place = intent.extras?.getParcelable("place", MainActivity.Place::class.java)
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
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(2000L)
            .build()

        val locationCallback = object : LocationCallback() {
            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    Log.d("TempleDetailActivity", "Location updated: Lat=$currentLatitude, Lon=$currentLongitude")
                    val place = intent.extras?.getParcelable("place", MainActivity.Place::class.java)
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

    private fun updateDynamicDistance(place: MainActivity.Place) {
        val distance = calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude).roundToInt()
        templeDistance.text = "$distance ${getString(R.string.km)}"
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}

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
        val imageRes = arguments?.getInt(ARG_IMAGE_RES) ?: android.R.drawable.ic_menu_info_details
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_full_screen_image)

        val imageView = dialog.findViewById<ImageView>(R.id.fullScreenImage)
        val closeButton = dialog.findViewById<Button>(R.id.btnClose)

        imageView.setImageResource(imageRes)

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        return dialog
    }
}