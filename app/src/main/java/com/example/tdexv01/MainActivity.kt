package com.example.tdexv01

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class MainActivity : BaseActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val LOCATION_REQUEST_CODE = 1001
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private lateinit var placesRecyclerView: RecyclerView
    private lateinit var autocompleteRecyclerView: RecyclerView
    private val addedPlaces = mutableListOf<Place>()
    private lateinit var autocompleteAdapter: AutocompleteAdapter
    private var currentCategoryFilter: ((Place) -> Boolean)? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private val prefs by lazy { getSharedPreferences("OnboardingPrefs", MODE_PRIVATE) }
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geocoder: Geocoder
    private var currentDistrict: String = "Unknown"
    private lateinit var citySpinner: Spinner
    private val templeCards = mutableMapOf<CardView, Place>()
    private var selectedCity: String = "All" // Track selected city in English for filtering
    // BLE Detection Variables
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val targetMacAddress = "FE:3D:ED:B1:A3:DE"
    private val handler = Handler(Looper.getMainLooper())
    private var isDialogShown = false
    private var isInRange = false
    private var missedScanCount = 0
    private val MAX_MISSED_SCANS = 2
    private val SCAN_INTERVAL = 10_000L
    private val SCAN_DURATION = 8_000L
    private val BLE_REQUEST_CODE = 1003
    private val TAG = "BLEDebug"
    private var wasDetectedInLastScan = false
    private val MARUTHAMALAI_REQUEST_CODE = 1004
    private var dontShowAgain = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geocoder = Geocoder(this, Locale.getDefault())

        // Initialize Spinner
        citySpinner = findViewById(R.id.citySpinner)

        // Check if user is signed in
        val currentUser = auth.currentUser
        if (currentUser == null) {
            val hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding", false)
            if (hasSeenOnboarding) {
                startActivity(Intent(this, SignInActivity::class.java))
            } else {
                startActivity(Intent(this, OnboardingActivity::class.java))
            }
            finish()
            return
        }

        val searchEditText: EditText = findViewById(R.id.searchEditText)
        val searchButton: ImageView = findViewById(R.id.searchButton)
        val voiceSearchButton: ImageView = findViewById(R.id.voiceSearchButton)
        val categoryAllButton: Button = findViewById(R.id.categoryAllButton)
        val category5kmButton: Button = findViewById(R.id.category5kmButton)
        val category10kmButton: Button = findViewById(R.id.category10kmButton)
        val category15kmButton: Button = findViewById(R.id.category15kmButton)
        val category20kmButton: Button = findViewById(R.id.category20kmButton)
        val noPlacesText: TextView = findViewById(R.id.noPlacesTextView)
        placesRecyclerView = findViewById(R.id.placesRecyclerView)
        placesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        placesRecyclerView.setHasFixedSize(true)
        autocompleteRecyclerView = findViewById(R.id.autocompleteRecyclerView)
        autocompleteRecyclerView.layoutManager = LinearLayoutManager(this)
        autocompleteRecyclerView.setHasFixedSize(true)
        autocompleteAdapter = AutocompleteAdapter(emptyList()) { selectedPlace ->
            val distance = calculateDistance(currentLatitude, currentLongitude, selectedPlace.latitude, selectedPlace.longitude)
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("place", selectedPlace)
            startActivity(intent)
            searchEditText.text.clear()
        }
        autocompleteRecyclerView.adapter = autocompleteAdapter

        // Request location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
        } else {
            getLocation()
        }

        // Restore selected city from SharedPreferences
        selectedCity = prefs.getString("selected_city", "All") ?: "All"
        updatePlacesList(Place.getAllPlaces(this))

        // Setup Spinner with multilingual support
        setupCitySpinner()

        // Initialize temple cards
        initializeTempleCards()

        // Spinner selection listener
        citySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedCity = getCityKey(position)
                prefs.edit().putString("selected_city", selectedCity).apply()
                Log.d("MainActivity", "Selected city: $selectedCity")
                filterTempleCards(selectedCity)
                updatePlacesList(Place.getAllPlaces(this@MainActivity))
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedCity = "All"
                prefs.edit().putString("selected_city", selectedCity).apply()
                Log.d("MainActivity", "No selection, defaulting to: $selectedCity")
                filterTempleCards(selectedCity)
                updatePlacesList(Place.getAllPlaces(this@MainActivity))
            }
        }

        // Initialize SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val prefs = getSharedPreferences("Settings", MODE_PRIVATE)
        val selectedLanguageCode = prefs.getString("Language", "en") ?: "en"
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, when (selectedLanguageCode) {
                "ta" -> "ta-IN" // Tamil
                "hi" -> "hi-IN" // Hindi
                "ml" -> "ml-IN" // Malayalam
                "fr" -> "fr-FR" // French
                "es" -> "es-ES" // Spanish
                "ja" -> "ja-JP" // Japanese
                "ko" -> "ko-KR" // Korean
                "ms" -> "ms-MY" // Malay
                "te" -> "te-IN" // Telugu
                "mr" -> "mr-IN" // Marathi
                "de" -> "de-DE" // German
                "kn" -> "kn-IN" // Kannada
                else -> "en-US" // English
            })
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speak_to_search))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Toast.makeText(this@MainActivity, "Speech recognition error: $error", Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { spokenText ->
                    searchEditText.setText(spokenText)
                    processVoiceSearch(spokenText)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val prefs = getSharedPreferences("Settings", MODE_PRIVATE)
                val selectedLanguageCode = prefs.getString("Language", "en") ?: "en"
                val locale = when (selectedLanguageCode) {
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
                val result = textToSpeech?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "Language $selectedLanguageCode not supported for TTS, using English", Toast.LENGTH_SHORT).show()
                    textToSpeech?.setLanguage(Locale.US)
                }
            } else {
                Toast.makeText(this, "Text-to-speech initialization failed", Toast.LENGTH_SHORT).show()
            }
        }

        // Autocomplete listener for search bar
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim()?.lowercase() ?: ""
                if (query.isNotEmpty()) {
                    val filteredPlaces = Place.getAllPlaces(this@MainActivity).filter { it.name.lowercase().contains(query) }
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
                Toast.makeText(this, getString(R.string.please_enter_a_place_name), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val matchingPlace = Place.getAllPlaces(this).find { place ->
                place.name.lowercase().contains(searchText) || place.locationEnglish.lowercase().contains(searchText)
            }
            if (matchingPlace != null) {
                val distance = calculateDistance(currentLatitude, currentLongitude, matchingPlace.latitude, matchingPlace.longitude)
                val intent = Intent(this, TempleDetailActivity::class.java)
                intent.putExtra("place", matchingPlace)
                startActivity(intent)
            } else {
                Toast.makeText(this, getString(R.string.place_not_found, searchText), Toast.LENGTH_SHORT).show()
            }
            autocompleteRecyclerView.visibility = View.GONE
        }

        // Voice Search functionality
        voiceSearchButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1003)
            } else if (speechRecognizer != null) {
                speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().language + "-" + Locale.getDefault().country)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speak_to_search))
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                })
            } else {
                Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show()
            }
        }

        categoryAllButton.setOnClickListener {
            selectCategoryButton(categoryAllButton, arrayOf(category5kmButton, category10kmButton, category15kmButton, category20kmButton))
            currentCategoryFilter = null
            updatePlacesList(Place.getAllPlaces(this))
            noPlacesText.visibility = if (Place.getAllPlaces(this).isEmpty()) View.VISIBLE else View.GONE
            placesRecyclerView.visibility = if (Place.getAllPlaces(this).isNotEmpty()) View.VISIBLE else View.GONE
            speakRecommendation(getString(R.string.showing_all_places_near_coimbatore))
        }

        category5kmButton.setOnClickListener {
            selectCategoryButton(category5kmButton, arrayOf(categoryAllButton, category10kmButton, category15kmButton, category20kmButton))
            currentCategoryFilter = { place -> calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude) <= 5 }
            updatePlacesList(Place.getAllPlaces(this))
            speakRecommendation(getString(R.string.showing_places_within_5_kilometers))
        }

        category10kmButton.setOnClickListener {
            selectCategoryButton(category10kmButton, arrayOf(categoryAllButton, category5kmButton, category15kmButton, category20kmButton))
            currentCategoryFilter = { place -> calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude) <= 10 }
            updatePlacesList(Place.getAllPlaces(this))
            speakRecommendation(getString(R.string.showing_places_within_10_kilometers))
        }

        category15kmButton.setOnClickListener {
            selectCategoryButton(category15kmButton, arrayOf(categoryAllButton, category5kmButton, category10kmButton, category20kmButton))
            currentCategoryFilter = { place -> calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude) <= 15 }
            updatePlacesList(Place.getAllPlaces(this))
            speakRecommendation(getString(R.string.showing_places_within_15_kilometers))
        }

        category20kmButton.setOnClickListener {
            selectCategoryButton(category20kmButton, arrayOf(categoryAllButton, category5kmButton, category10kmButton, category15kmButton))
            currentCategoryFilter = { place -> calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude) <= 20 }
            updatePlacesList(Place.getAllPlaces(this))
            speakRecommendation(getString(R.string.showing_places_within_20_kilometers))
        }

        // Bottom NavigationView Setup
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.menu.findItem(R.id.home).setChecked(true)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> true
                R.id.visited -> {
                    startActivity(Intent(this, VisitedPlacesActivity::class.java))
                    true
                }
                R.id.tovisit -> {
                    startActivity(Intent(this, AddedPlacesActivity::class.java))
                    true
                }
                R.id.profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Initialize Bluetooth for BLE detection
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter

        // Initialize dontShowAgain
        dontShowAgain = false

        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, getString(R.string.please_enable_bluetooth), Toast.LENGTH_LONG).show()
            return
        }

        // Request BLE and location permissions
        requestBlePermissions()
    }

    private fun initializeTempleCards() {
        val places = Place.getAllPlaces(this)
        Log.d("MainActivity", "Places size: ${places.size}")
        val cardMappings = listOf(
            R.id.MaruthamalaiTemple to 0,
            R.id.MeenakshiTemple to 1,
            R.id.Velligirihills to 2,
            R.id.GassMuseum to 3,
            R.id.MoneyFalls to 4,
            R.id.GDmuseum to 5,
            R.id.WildlifeSathi to 6,
            R.id.GovmentMuseum to 7,
            R.id.Valparai to 8,
            R.id.PerurEeswaran to 9,
            R.id.Georgefort to 10,
            R.id.Santhomchurch to 11,
            R.id.Kapaleeshwarar to 12,
            R.id.GovernmentMuseumEgmore to 13,
            R.id.Parthasarathy to 14,
            R.id.MeenakshiAmman to 15,
            R.id.ThirumalaiNayakkar to 16,
            R.id.KoodalAzhagar to 17,
            R.id.VandiyurMariamman to 18,
            R.id.GandhiMemorial to 19,
            R.id.KazimarBigMosque to 20,
            R.id.StMarysCathedral to 21,
            R.id.SamanarMalaiJainBeds to 22,
            R.id.BrihadeeswararTemple to 23,
            R.id.ShoreTemple to 24,
            R.id.KailasanatharTemple to 25,
            R.id.ThillaiNatarajaTemple to 26,
            R.id.PadmanabhapuramPalace to 27,
            R.id.AiravatesvaraTemple to 28,
            R.id.RockFortTemple  to 29,
            R.id.GangaikondaCholapuramTemple to 30,
            R.id.VivekanandaRockMemorial to 31


        )
        cardMappings.forEach { (cardId, index) ->
            if (index < places.size) {
                val card = findViewById<CardView>(cardId)
                templeCards[card] = places[index]
                card.setOnClickListener {
                    val intent = Intent(this, TempleDetailActivity::class.java)
                    intent.putExtra("place", places[index])
                    startActivity(intent)
                }
            } else {
                Log.e("MainActivity", "Index $index exceeds places size (${places.size}) for card ID $cardId")
            }
        }
        val initialPosition = arrayOf("All", "Chennai", "Coimbatore", "Madurai","Most Famous").indexOf(selectedCity).coerceAtLeast(0)
        val cityKey = getCityKey(initialPosition)
        Log.d("MainActivity", "Initial filter with city: $selectedCity, Key: $cityKey")
        filterTempleCards(cityKey)
    }

    private fun setupCitySpinner() {
        val cityOptions = arrayOf(
            getString(R.string.all),
            getString(R.string.chennai),
            getString(R.string.coimbatore),
            getString(R.string.madurai),
            getString(R.string.most_famous) // Use string resource for multilingual support
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cityOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        citySpinner.adapter = adapter

        // Restore selection using English key
        selectedCity = prefs.getString("selected_city", "All") ?: "All"
        val englishKeys = arrayOf("All", "Chennai", "Coimbatore", "Madurai", "Most Famous")
        val selectedIndex = englishKeys.indexOf(selectedCity).coerceAtLeast(0)
        citySpinner.setSelection(selectedIndex)
    }

    private fun getCityKey(position: Int): String {
        return when (position) {
            0 -> "All"
            1 -> "Chennai"
            2 -> "Coimbatore"
            3 -> "Madurai"
            4 -> "Most Famous" // Added new key
            else -> "All"
        }
    }

    private fun filterPlacesByCity(places: List<Place>): List<Place> {
        val cityKey = getCityKey(citySpinner.selectedItemPosition)
        Log.d("MainActivity", "Filtering places by city: $cityKey")
        val filtered = when (cityKey) {
            "All" -> places
            "Chennai" -> places.filter { it.locationEnglish.contains("Chennai", ignoreCase = true) }
            "Coimbatore" -> places.filter { it.locationEnglish.contains("Coimbatore", ignoreCase = true) }
            "Madurai" -> places.filter { it.locationEnglish.contains("Madurai", ignoreCase = true) }
            "Most Famous" -> {
                // Define the top 10 famous heritage sites (example list, adjust as needed)
                val famousHeritageSites = listOf(
                    "Brihadeeswarar Temple",
                    "Shore Temple",
                    "Kailasanathar Temple",
                    "Thillai Nataraja Temple",
                    "Padmanabhapuram Palace",
                    "Airavatesvara Temple",
                    "Rock Fort Temple",
                    "Gangaikonda Cholapuram Temple",
                    "Vivekananda Rock Memorial",
                    "Meenakshi Amman Temple"
                )
                // Filter places that are either in their own city or in the famous list
                places.filter { place ->
                    val isInFamousList = famousHeritageSites.contains(place.name)
                    val isInOtherCity = !place.locationEnglish.contains("Chennai", ignoreCase = true) &&
                            !place.locationEnglish.contains("Coimbatore", ignoreCase = true) &&
                            !place.locationEnglish.contains("Madurai", ignoreCase = true)
                    isInFamousList || isInOtherCity
                }.sortedBy { place -> // Sort to prioritize famous ones and limit to 10
                    if (famousHeritageSites.contains(place.name)) 0 else 1
                }.take(10) // Limit to top 10
            }
            else -> places
        }
        Log.d("MainActivity", "Filtered places count: ${filtered.size}")
        return filtered
    }

    private fun filterTempleCards(city: String) {
        Log.d("MainActivity", "Filtering temple cards for city: $city")
        templeCards.forEach { (card, place) ->
            val isVisible = when (city) {
                "All" -> true
                "Chennai" -> place.locationEnglish.contains("Chennai", ignoreCase = true)
                "Coimbatore" -> place.locationEnglish.contains("Coimbatore", ignoreCase = true)
                "Madurai" -> place.locationEnglish.contains("Madurai", ignoreCase = true)
                "Most Famous" -> {
                    val famousHeritageSites = listOf(
                        "Brihadeeswarar Temple",
                        "Shore Temple",
                        "Kailasanathar Temple",
                        "Thillai Nataraja Temple",
                        "Padmanabhapuram Palace",
                        "Airavatesvara Temple",
                        "Rock Fort Temple",
                        "Gangaikonda Cholapuram Temple",
                        "Vivekananda Rock Memorial",
                        "Meenakshi Amman Temple"
                    )
                    famousHeritageSites.contains(place.name) ||
                            (!place.locationEnglish.contains("Chennai", ignoreCase = true) &&
                                    !place.locationEnglish.contains("Coimbatore", ignoreCase = true) &&
                                    !place.locationEnglish.contains("Madurai", ignoreCase = true))
                }
                else -> true
            }
            Log.d("MainActivity", "Place: ${place.name}, Location: ${place.locationEnglish}, Visible: $isVisible")
            card.visibility = if (isVisible) View.VISIBLE else View.GONE
        }
    }

    private fun requestBlePermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permissions.all { ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            startPeriodicBleScan()
        } else {
            ActivityCompat.requestPermissions(this, permissions, BLE_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocation()
                } else {
                    Toast.makeText(this, getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show()
                    currentDistrict = "Permission denied"
                }
            }
            BLE_REQUEST_CODE -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    startPeriodicBleScan()
                } else {
                    Toast.makeText(this, getString(R.string.ble_permissions_denied), Toast.LENGTH_LONG).show()
                }
            }
            1003 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().language + "-" + Locale.getDefault().country)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speak_to_search))
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    })
                } else {
                    Toast.makeText(this, getString(R.string.microphone_permission_required), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startPeriodicBleScan() {
        val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val scanRunnable = object : Runnable {
            override fun run() {
                Log.d(TAG, "Starting scan...")
                bluetoothLeScanner.startScan(scanCallback)
                handler.postDelayed({
                    bluetoothLeScanner.stopScan(scanCallback)
                    Log.d(TAG, "Scan stopped. Missed scan count: $missedScanCount")
                    if (isInRange && !wasDetectedInLastScan) {
                        missedScanCount++
                        if (missedScanCount >= MAX_MISSED_SCANS) {
                            isInRange = false
                            missedScanCount = 0
                            Log.d(TAG, "Smartwatch out of range after $MAX_MISSED_SCANS missed scans")
                            if (!dontShowAgain) {
                                showOutOfRangeDialog()
                            }
                            Toast.makeText(this@MainActivity, getString(R.string.smartwatch_out_of_range), Toast.LENGTH_SHORT).show()
                        }
                    } else if (!isInRange && wasDetectedInLastScan) {
                        isInRange = true
                        missedScanCount = 0
                    }
                    wasDetectedInLastScan = false
                }, SCAN_DURATION)
                handler.postDelayed(this, SCAN_INTERVAL)
            }
        }
        handler.post(scanRunnable)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let { device ->
                val deviceAddress = device.address
                val rssi = result.rssi
                if (deviceAddress.equals(targetMacAddress, ignoreCase = true)) {
                    val distance = calculateDistance(rssi)
                    Log.d(TAG, "Detected $targetMacAddress, RSSI: $rssi, Distance: $distance m")
                    if (distance in 1.0..10.0) {
                        wasDetectedInLastScan = true
                        if (!isInRange && !isDialogShown && !dontShowAgain) {
                            isInRange = true
                            Log.d(TAG, "Smartwatch entered range, showing dialog")
                            showPermissionDialog(distance)
                        }
                    }
                }
            }
        }
    }

    private fun calculateDistance(rssi: Int): Double {
        val txPower = -51
        if (rssi == 0) return -1.0
        val n = 2.0
        return 10.0.pow((txPower - rssi) / (10 * n))
    }

    private fun showPermissionDialog(distance: Double) {
        isDialogShown = true
        val builder = AlertDialog.Builder(this)
            .setTitle(getString(R.string.ble_device_detected))
            .setMessage(getString(R.string.ble_want_to_know, distance))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                showMaruthamalaiInfo(distance)
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                isDialogShown = false
                Log.d(TAG, "Dialog canceled")
            }
            .setCancelable(false)
        val checkBox = CheckBox(this).apply {
            text = "Don't show again"
            setOnCheckedChangeListener { _, isChecked -> dontShowAgain = isChecked }
        }
        builder.setView(checkBox)
        builder.show()
    }

    private fun showOutOfRangeDialog() {
        isDialogShown = true
        val builder = AlertDialog.Builder(this)
            .setTitle(getString(R.string.ble_device_lost))
            .setMessage(getString(R.string.smartwatch_out_of_range))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                isDialogShown = false
            }
            .setCancelable(false)
        val checkBox = CheckBox(this).apply {
            text = "Don't show again"
            setOnCheckedChangeListener { _, isChecked -> dontShowAgain = isChecked }
        }
        builder.setView(checkBox)
        builder.show()
    }

    private fun showMaruthamalaiInfo(distance: Double) {
        isDialogShown = false
        val intent = Intent(this, MaruthamalaiInfoActivity::class.java)
        intent.putExtra("distance", distance)
        startActivityForResult(intent, MARUTHAMALAI_REQUEST_CODE)
    }

    private fun processVoiceSearch(spokenText: String) {
        val searchQuery = spokenText.trim().lowercase()
        if (searchQuery.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_voice_input_detected), Toast.LENGTH_SHORT).show()
            return
        }
        val matchingPlace = Place.getAllPlaces(this).find { place ->
            place.name.lowercase().contains(searchQuery) || place.locationEnglish.lowercase().contains(searchQuery) || place.description.lowercase().contains(searchQuery)
        }
        if (matchingPlace != null) {
            val distance = calculateDistance(currentLatitude, currentLongitude, matchingPlace.latitude, matchingPlace.longitude)
            val formattedDistance = String.format("%.1f", distance) // Format distance separately
            val message = getString(R.string.found_place, matchingPlace.name, matchingPlace.location, formattedDistance) // Pass 3 arguments
            speakRecommendation(message)
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("place", matchingPlace)
            startActivity(intent)
        } else {
            Toast.makeText(this, getString(R.string.place_not_found, searchQuery), Toast.LENGTH_SHORT).show()
            speakRecommendation(getString(R.string.couldnt_find_place, searchQuery))
        }
    }

    private fun speakRecommendation(text: String) {
        val prefs = getSharedPreferences("Settings", MODE_PRIVATE)
        val selectedLanguageCode = prefs.getString("Language", "en") ?: "en"
        val locale = when (selectedLanguageCode) {
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
        textToSpeech?.setLanguage(locale)
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
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
        val cityFilteredPlaces = filterPlacesByCity(filteredPlaces)
        val placesToDisplay = currentCategoryFilter?.let { filter -> cityFilteredPlaces.filter(filter) } ?: cityFilteredPlaces
        val adapter = PlaceAdapter(placesToDisplay, currentLatitude, currentLongitude, this)
        placesRecyclerView.adapter = adapter
        val noPlacesText: TextView = findViewById(R.id.noPlacesTextView)
        if (placesToDisplay.isEmpty()) {
            noPlacesText.visibility = View.VISIBLE
            placesRecyclerView.visibility = View.GONE
        } else {
            noPlacesText.visibility = View.GONE
            placesRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun getLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    try {
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            currentDistrict = address.subAdminArea ?: address.adminArea ?: "Unknown"
                            Log.d("MainActivity", "Current district: $currentDistrict")
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Geocoding failed: ${e.message}")
                        currentDistrict = "Location error"
                    }
                    updatePlacesList(Place.getAllPlaces(this))
                } else {
                    Log.w("MainActivity", "Location is null")
                    currentDistrict = "Location unavailable"
                }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Failed to get location: ${e.message}")
                currentDistrict = "Location failed"
                Toast.makeText(this, "Failed to get location: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val a = 6378137.0 // WGS-84 semi-major axis in meters
        val f = 1.0 / 298.257223563 // WGS-84 flattening
        val b = a * (1 - f)
        val maxIterations = 1000
        val tolerance = 1e-12 // Convergence tolerance

        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val L = Math.toRadians(lon2 - lon1)

        val U1 = atan((1 - f) * tan(phi1))
        val U2 = atan((1 - f) * tan(phi2))

        val sinU1 = sin(U1)
        val cosU1 = cos(U1)
        val sinU2 = sin(U2)
        val cosU2 = cos(U2)

        var lambda = L
        var lambdaPrev: Double
        var sinLambda: Double
        var cosLambda: Double
        var sinSigma: Double
        var cosSigma: Double
        var sigma: Double
        var cos2SigmaM: Double
        var cosSqAlpha: Double
        var iter = 0

        do {
            sinLambda = sin(lambda)
            cosLambda = cos(lambda)
            sinSigma = sqrt(
                (cosU2 * sinLambda).pow(2.0) +
                        (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda).pow(2.0)
            )
            if (sinSigma == 0.0) return 0.0 // Coincident points

            cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda
            sigma = atan2(sinSigma, cosSigma)
            val sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma
            cosSqAlpha = 1.0 - sinAlpha.pow(2.0)
            cos2SigmaM = if (cosSqAlpha != 0.0) cosSigma - 2.0 * sinU1 * sinU2 / cosSqAlpha else 0.0

            val C = f / 16.0 * cosSqAlpha * (4.0 + f * (4.0 - 3.0 * cosSqAlpha))
            lambdaPrev = lambda
            lambda = L + (1.0 - C) * f * sinAlpha * (sigma + C * sinSigma *
                    (cos2SigmaM + C * cosSigma * (-1.0 + 2.0 * cos2SigmaM.pow(2.0))))

            if (lambda > PI) lambda -= 2.0 * PI
            else if (lambda < -PI) lambda += 2.0 * PI

            iter++
        } while (abs(lambda - lambdaPrev) > tolerance && iter < maxIterations)

        if (iter == maxIterations) {
            // Fallback to Haversine formula if no convergence
            return haversineFallback(lat1, lon1, lat2, lon2)
        }

        val uSq = cosSqAlpha * (a.pow(2.0) - b.pow(2.0)) / b.pow(2.0)
        val A = 1.0 + uSq / 16384.0 * (4096.0 + uSq * (-768.0 + uSq * (320.0 - 175.0 * uSq)))
        val B = uSq / 1024.0 * (256.0 + uSq * (-128.0 + uSq * (74.0 - 47.0 * uSq)))
        val deltaSigma = B * sinSigma * (cos2SigmaM + B / 4.0 * (cosSigma * (-1.0 + 2.0 * cos2SigmaM.pow(2.0)) -
                B / 6.0 * cos2SigmaM * (-3.0 + 4.0 * sinSigma.pow(2.0)) * (-3.0 + 4.0 * cos2SigmaM.pow(2.0))))

        val s = b * A * (sigma - deltaSigma) // Distance in meters
        return s / 1000.0 // Convert to kilometers
    }

    private fun haversineFallback(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return earthRadius * c
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MARUTHAMALAI_REQUEST_CODE) {
            isDialogShown = false
            Log.d(TAG, "Returned from MaruthamalaiInfoActivity, dialog state reset")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        }
        handler.removeCallbacksAndMessages(null)
    }

    fun addPlaceToList(place: Place) {
        (application as? AddedPlacesActivity)?.addPlace(place) ?: run {
            val intent = Intent(this, AddedPlacesActivity::class.java)
            intent.putExtra("place", place)
            startActivityForResult(intent, 1002)
        }
        Toast.makeText(this, getString(R.string.place_added_to_visit_locations, place.name), Toast.LENGTH_SHORT).show()
    }

    data class Place(
        val name: String,
        val locationEnglish: String, // English location for filtering
        val location: String, // Localized location for display
        val staticDistance: String,
        val description: String,
        val latitude: Double,
        val longitude: Double,
        val image1: Int,
        val image2: Int,
        val image3: Int,
        val image4: Int,
        val openingHours: String,
        val closingHours: String,
        val operatingWeekdays: String
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readDouble(),
            parcel.readDouble(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readString() ?: ""
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(name)
            parcel.writeString(locationEnglish)
            parcel.writeString(location)
            parcel.writeString(staticDistance)
            parcel.writeString(description)
            parcel.writeDouble(latitude)
            parcel.writeDouble(longitude)
            parcel.writeInt(image1)
            parcel.writeInt(image2)
            parcel.writeInt(image3)
            parcel.writeInt(image4)
            parcel.writeString(openingHours)
            parcel.writeString(closingHours)
            parcel.writeString(operatingWeekdays)
        }

        override fun describeContents(): Int = 0

        companion object CREATOR : Parcelable.Creator<Place> {
            override fun createFromParcel(parcel: Parcel): Place = Place(parcel)
            override fun newArray(size: Int): Array<Place?> = arrayOfNulls(size)

            fun getAllPlaces(context: Context): List<Place> = listOf(
                Place(
                    context.getString(R.string.maruthamalai_temple_name),
                    "Coimbatore, Tamil Nadu",
                    context.getString(R.string.maruthamalai_temple_location),
                    context.getString(R.string.maruthamalai_temple_distance),
                    context.getString(R.string.maruthamalai_temple_description),
                    11.03823260558333, 76.86621211803832,
                    R.drawable.maruthamalai_1, R.drawable.maruthamalai_2, R.drawable.maruthamalai_3, R.drawable.marudhamalai_4,
                    context.getString(R.string.maruthamalai_temple_opening_hours),
                    context.getString(R.string.maruthamalai_temple_closing_hours),
                    context.getString(R.string.maruthamalai_temple_operating_days)
                ),
                Place(
                    context.getString(R.string.isha_yoga_centre_name),
                    "Coimbatore, Tamil Nadu",
                    context.getString(R.string.isha_yoga_centre_location),
                    context.getString(R.string.isha_yoga_centre_distance),
                    context.getString(R.string.isha_yoga_centre_description),
                    10.97637263768354, 76.73545553251903,
                    R.drawable.isha1, R.drawable.isha2, R.drawable.isha3, R.drawable.isha4,
                    context.getString(R.string.isha_yoga_centre_opening_hours),
                    context.getString(R.string.isha_yoga_centre_closing_hours),
                    context.getString(R.string.isha_yoga_centre_operating_days)
                ),
                Place(
                    context.getString(R.string.velliangiri_hills_name),
                    "Coimbatore, Tamil Nadu",
                    context.getString(R.string.velliangiri_hills_location),
                    context.getString(R.string.velliangiri_hills_distance),
                    context.getString(R.string.velliangiri_hills_description),
                    10.9839389, 76.6927034,
                    R.drawable.velliangiri1, R.drawable.velliangiri2, R.drawable.velliangiri3, R.drawable.velliangiri4,
                    context.getString(R.string.velliangiri_hills_opening_hours),
                    context.getString(R.string.velliangiri_hills_closing_hours),
                    context.getString(R.string.velliangiri_hills_operating_days)
                ),
                Place(
                    context.getString(R.string.gass_forest_museum_name),
                    "Coimbatore, Tamil Nadu",
                    context.getString(R.string.gass_forest_museum_location),
                    context.getString(R.string.gass_forest_museum_distance),
                    context.getString(R.string.gass_forest_museum_description),
                    11.0078, 76.9544,
                    R.drawable.gassmus1, R.drawable.gassmus2, R.drawable.gassmus3, R.drawable.gassmuss4,
                    context.getString(R.string.gass_forest_museum_opening_hours),
                    context.getString(R.string.gass_forest_museum_closing_hours),
                    context.getString(R.string.gass_forest_museum_operating_days)
                ),
                Place(
                    context.getString(R.string.kaviyaruvi_waterfalls_name),
                    "Coimbatore, Tamil Nadu",
                    context.getString(R.string.kaviyaruvi_waterfalls_location),
                    context.getString(R.string.kaviyaruvi_waterfalls_distance),
                    context.getString(R.string.kaviyaruvi_waterfalls_description),
                    10.5783, 76.8639,
                    R.drawable.monkeyfalls1, R.drawable.monkeyfalls2, R.drawable.monkeyfalls3, R.drawable.monkeyfalls4,
                    context.getString(R.string.kaviyaruvi_waterfalls_opening_hours),
                    context.getString(R.string.kaviyaruvi_waterfalls_closing_hours),
                    context.getString(R.string.kaviyaruvi_waterfalls_operating_days)
                ),
                Place(
                    context.getString(R.string.gedee_museum_name),
                    "Coimbatore, Tamil Nadu",
                    context.getString(R.string.gedee_museum_location),
                    context.getString(R.string.gedee_museum_distance),
                    context.getString(R.string.gedee_museum_description),
                    11.0156, 76.9583,
                    R.drawable.gdmus1, R.drawable.gdmus2, R.drawable.gdmus3, R.drawable.gdmus4,
                    context.getString(R.string.gedee_museum_opening_hours),
                    context.getString(R.string.gedee_museum_closing_hours),
                    context.getString(R.string.gedee_museum_operating_days)
                ),
                Place(
                    context.getString(R.string.sathyamangalam_wildlife_sanctuary_name),
                    "Coimbatore, Tamil Nadu",
                    context.getString(R.string.sathyamangalam_wildlife_sanctuary_location),
                    context.getString(R.string.sathyamangalam_wildlife_sanctuary_distance),
                    context.getString(R.string.sathyamangalam_wildlife_sanctuary_description),
                    11.5064, 77.2411,
                    R.drawable.wildlifesakthi1, R.drawable.wildlifesakthi2, R.drawable.wildlifesakthi3, R.drawable.wildlifesakthi4,
                    context.getString(R.string.sathyamangalam_wildlife_sanctuary_opening_hours),
                    context.getString(R.string.sathyamangalam_wildlife_sanctuary_closing_hours),
                    context.getString(R.string.sathyamangalam_wildlife_sanctuary_operating_days)
                ),
                Place(
                    context.getString(R.string.government_museum_name),
                    "Coimbatore, Tamil Nadu",
                    context.getString(R.string.government_museum_location),
                    context.getString(R.string.government_museum_distance),
                    context.getString(R.string.government_museum_description),
                    11.0139, 76.9611,
                    R.drawable.govmus1, R.drawable.govmus2, R.drawable.govmus3, R.drawable.govmus4,
                    context.getString(R.string.government_museum_opening_hours),
                    context.getString(R.string.government_museum_closing_hours),
                    context.getString(R.string.government_museum_operating_days)
                ),
                Place(
                    context.getString(R.string.valparai_hill_station_name),
                    "Coimbatore, Tamil Nadu",
                    context.getString(R.string.valparai_hill_station_location),
                    context.getString(R.string.valparai_hill_station_distance),
                    context.getString(R.string.valparai_hill_station_description),
                    10.3236, 76.9478,
                    R.drawable.valparai1, R.drawable.valparai2, R.drawable.valparai3, R.drawable.valparai4,
                    context.getString(R.string.valparai_hill_station_opening_hours),
                    context.getString(R.string.valparai_hill_station_closing_hours),
                    context.getString(R.string.valparai_hill_station_operating_days)
                ),
                Place(
                    context.getString(R.string.perur_pateeswarar_temple_name),
                    "Coimbatore, Tamil Nadu",
                    context.getString(R.string.perur_pateeswarar_temple_location),
                    context.getString(R.string.perur_pateeswarar_temple_distance),
                    context.getString(R.string.perur_pateeswarar_temple_description),
                    10.9944, 76.9289,
                    R.drawable.perurpateeswarartemple1, R.drawable.perurpateeswarartemple2, R.drawable.perurpateeswarartemple3, R.drawable.perurpateeswarartemple4,
                    context.getString(R.string.perur_pateeswarar_temple_opening_hours),
                    context.getString(R.string.perur_pateeswarar_temple_closing_hours),
                    context.getString(R.string.perur_pateeswarar_temple_operating_days)
                ),
                Place(
                    context.getString(R.string.fort_st_george_name),
                    "Chennai, Tamil Nadu",
                    context.getString(R.string.fort_st_george_location),
                    context.getString(R.string.fort_st_george_distance),
                    context.getString(R.string.fort_st_george_description),
                    13.0796, 80.2869,
                    R.drawable.georgefort1, R.drawable.georgefort2, R.drawable.georgefort3, R.drawable.georgefort4,
                    context.getString(R.string.fort_st_george_opening_hours),
                    context.getString(R.string.fort_st_george_closing_hours),
                    context.getString(R.string.fort_st_george_operating_days)
                ),
                Place(
                    context.getString(R.string.san_thome_basilica_name),
                    "Chennai, Tamil Nadu",
                    context.getString(R.string.san_thome_basilica_location),
                    context.getString(R.string.san_thome_basilica_distance),
                    context.getString(R.string.san_thome_basilica_description),
                    13.0337, 80.2778,
                    R.drawable.santhome1, R.drawable.santhome2, R.drawable.santhome3, R.drawable.santhome4,
                    context.getString(R.string.san_thome_basilica_opening_hours),
                    context.getString(R.string.san_thome_basilica_closing_hours),
                    context.getString(R.string.san_thome_basilica_operating_days)
                ),
                Place(
                    context.getString(R.string.kapaleeshwarar_temple_name),
                    "Chennai, Tamil Nadu",
                    context.getString(R.string.kapaleeshwarar_temple_location),
                    context.getString(R.string.kapaleeshwarar_temple_distance),
                    context.getString(R.string.kapaleeshwarar_temple_description),
                    13.0339, 80.2705,
                    R.drawable.kapaleeshwarar1, R.drawable.kapaleeshwarar2, R.drawable.kapaleeshwarar3, R.drawable.kapaleeshwarar4,
                    context.getString(R.string.kapaleeshwarar_temple_opening_hours),
                    context.getString(R.string.kapaleeshwarar_temple_closing_hours),
                    context.getString(R.string.kapaleeshwarar_temple_operating_days)
                ),
                Place(
                    context.getString(R.string.government_museum_egmore_name),
                    "Chennai, Tamil Nadu",
                    context.getString(R.string.government_museum_egmore_location),
                    context.getString(R.string.government_museum_egmore_distance),
                    context.getString(R.string.government_museum_egmore_description),
                    13.0699, 80.2575,
                    R.drawable.govmusegmore1, R.drawable.govmusegmore2, R.drawable.govmusegmore3, R.drawable.govmusegmore4,
                    context.getString(R.string.government_museum_egmore_opening_hours),
                    context.getString(R.string.government_museum_egmore_closing_hours),
                    context.getString(R.string.government_museum_egmore_operating_days)
                ),
                Place(
                    context.getString(R.string.parthasarathy_temple_name),
                    "Chennai, Tamil Nadu",
                    context.getString(R.string.parthasarathy_temple_location),
                    context.getString(R.string.parthasarathy_temple_distance),
                    context.getString(R.string.parthasarathy_temple_description),
                    13.0451, 80.2678,
                    R.drawable.parthasarathy1, R.drawable.parthasarathy2, R.drawable.parthasarathy3, R.drawable.parthasarathy4,
                    context.getString(R.string.parthasarathy_temple_opening_hours),
                    context.getString(R.string.parthasarathy_temple_closing_hours),
                    context.getString(R.string.parthasarathy_temple_operating_days)
                ),
                Place(
                    context.getString(R.string.meenakshi_amman_temple_name),
                    "Madurai, Tamil Nadu",
                    context.getString(R.string.meenakshi_amman_temple_location),
                    context.getString(R.string.meenakshi_amman_temple_distance),
                    context.getString(R.string.meenakshi_amman_temple_description),
                    9.9195, 78.1193,
                    R.drawable.meenakshiamman1, R.drawable.meenakshiamman2, R.drawable.meenakshiamman3, R.drawable.meenakshiamman4,
                    context.getString(R.string.meenakshi_amman_temple_opening_hours),
                    context.getString(R.string.meenakshi_amman_temple_closing_hours),
                    context.getString(R.string.meenakshi_amman_temple_operating_days)
                ),
                Place(
                    context.getString(R.string.thirumalai_nayakkar_mahal_name),
                    "Madurai, Tamil Nadu",
                    context.getString(R.string.thirumalai_nayakkar_mahal_location),
                    context.getString(R.string.thirumalai_nayakkar_mahal_distance),
                    context.getString(R.string.thirumalai_nayakkar_mahal_description),
                    9.9151, 78.1219,
                    R.drawable.thirumalaimahal1, R.drawable.thirumalaimahal2, R.drawable.thirumalaimahal3, R.drawable.thirumalaimahal4,
                    context.getString(R.string.thirumalai_nayakkar_mahal_opening_hours),
                    context.getString(R.string.thirumalai_nayakkar_mahal_closing_hours),
                    context.getString(R.string.thirumalai_nayakkar_mahal_operating_days)
                ),
                Place(
                    context.getString(R.string.koodal_azhagar_temple_name),
                    "Madurai, Tamil Nadu",
                    context.getString(R.string.koodal_azhagar_temple_location),
                    context.getString(R.string.koodal_azhagar_temple_distance),
                    context.getString(R.string.koodal_azhagar_temple_description),
                    9.914471701638622, 78.11371564844706,
                    R.drawable.koodalalagar1, R.drawable.koodalalagar2, R.drawable.koodalalagar3, R.drawable.koodalalagar4,
                    context.getString(R.string.koodal_azhagar_temple_opening_hours),
                    context.getString(R.string.koodal_azhagar_temple_closing_hours),
                    context.getString(R.string.koodal_azhagar_temple_operating_days)
                ),
                Place(
                    context.getString(R.string.vandiyur_mariamman_teppakulam_name),
                    "Madurai, Tamil Nadu",
                    context.getString(R.string.vandiyur_mariamman_teppakulam_location),
                    context.getString(R.string.vandiyur_mariamman_teppakulam_distance),
                    context.getString(R.string.vandiyur_mariamman_teppakulam_description),
                    9.9128, 78.1376,
                    R.drawable.teppakulam1, R.drawable.teppakulam2, R.drawable.teppakulam3, R.drawable.teppakulam4,
                    context.getString(R.string.vandiyur_mariamman_teppakulam_opening_hours),
                    context.getString(R.string.vandiyur_mariamman_teppakulam_closing_hours),
                    context.getString(R.string.vandiyur_mariamman_teppakulam_operating_days)
                ),
                Place(
                    context.getString(R.string.gandhi_memorial_museum_name),
                    "Madurai, Tamil Nadu",
                    context.getString(R.string.gandhi_memorial_museum_location),
                    context.getString(R.string.gandhi_memorial_museum_distance),
                    context.getString(R.string.gandhi_memorial_museum_description),
                    9.930205342300024, 78.13846841434507,
                    R.drawable.gandhimus1, R.drawable.gandhimus2, R.drawable.gandhimus3, R.drawable.gandhimus4,
                    context.getString(R.string.gandhi_memorial_museum_opening_hours),
                    context.getString(R.string.gandhi_memorial_museum_closing_hours),
                    context.getString(R.string.gandhi_memorial_museum_operating_days)
                ),
                Place(
                    context.getString(R.string.kazimar_big_mosque_name),
                    "Madurai, Tamil Nadu",
                    context.getString(R.string.kazimar_big_mosque_location),
                    context.getString(R.string.kazimar_big_mosque_distance),
                    context.getString(R.string.kazimar_big_mosque_description),
                    9.91295840527046, 78.11415351906979,
                    R.drawable.mad_kazimar1, R.drawable.mad_kazimar2, R.drawable.mad_kazimar3, R.drawable.mad_kazimar4,
                    context.getString(R.string.kazimar_big_mosque_opening_hours),
                    context.getString(R.string.kazimar_big_mosque_closing_hours),
                    context.getString(R.string.kazimar_big_mosque_operating_days)
                ),
                Place(
                    context.getString(R.string.st_marys_cathedral_name),
                    "Madurai, Tamil Nadu",
                    context.getString(R.string.st_marys_cathedral_location),
                    context.getString(R.string.st_marys_cathedral_distance),
                    context.getString(R.string.st_marys_cathedral_description),
                    9.913565442056944, 78.1255656478728,
                    R.drawable.mad_stmary1, R.drawable.mad_stmary2, R.drawable.mad_stmary3, R.drawable.mad_stmary4,
                    context.getString(R.string.st_marys_cathedral_opening_hours),
                    context.getString(R.string.st_marys_cathedral_closing_hours),
                    context.getString(R.string.st_marys_cathedral_operating_days)
                ),
                Place(
                    context.getString(R.string.samanar_malai_and_jain_beds_name),
                    "Madurai, Tamil Nadu",
                    context.getString(R.string.samanar_malai_and_jain_beds_location),
                    context.getString(R.string.samanar_malai_and_jain_beds_distance),
                    context.getString(R.string.samanar_malai_and_jain_beds_description),
                    9.9333, 78.0667,
                    R.drawable.keelakuilkudi1, R.drawable.keelakuilkudi2, R.drawable.keelakuilkudi3, R.drawable.keelakuilkudi4,
                    context.getString(R.string.samanar_malai_and_jain_beds_opening_hours),
                    context.getString(R.string.samanar_malai_and_jain_beds_closing_hours),
                    context.getString(R.string.samanar_malai_and_jain_beds_operating_days)
                ),
                Place(
                    context.getString(R.string.brihadeeswarar_temple_name),
                    "Thanjavur, Tamil Nadu",
                    context.getString(R.string.brihadeeswarar_temple_location),
                    context.getString(R.string.brihadeeswarar_temple_distance),
                    context.getString(R.string.brihadeeswarar_temple_description),
                    10.7828, 79.1318,
                    R.drawable.brihadeeshwartemple1, R.drawable.brihadeeshwartemple2, R.drawable.brihadeeshwartemple3, R.drawable.brihadeeshwartemple4,
                    context.getString(R.string.brihadeeswarar_temple_opening_hours),
                    context.getString(R.string.brihadeeswarar_temple_closing_hours),
                    context.getString(R.string.brihadeeswarar_temple_operating_days)
                ),
                Place(
                    context.getString(R.string.shore_temple_name),
                    "Chengalpattu, Tamil Nadu",
                    context.getString(R.string.shore_temple_location),
                    context.getString(R.string.shore_temple_distance),
                    context.getString(R.string.shore_temple_description),
                    12.6160, 80.1990,
                    R.drawable.shoretemple1, R.drawable.shoretemple2, R.drawable.shoretemple3, R.drawable.shoretemple4,
                    context.getString(R.string.shore_temple_opening_hours),
                    context.getString(R.string.shore_temple_closing_hours),
                    context.getString(R.string.shore_temple_operating_days)
                ),
                Place(
                    context.getString(R.string.kailasanathar_temple_name),
                    "Kanchipuram, Tamilnadu",
                    context.getString(R.string.kailasanathar_temple_location),
                    context.getString(R.string.kailasanathar_temple_distance),
                    context.getString(R.string.kailasanathar_temple_description),
                    12.8410, 79.7040,
                    R.drawable.kailasanathar1, R.drawable.kailasanathar2, R.drawable.kailasanathar3, R.drawable.kailasanathar4,
                    context.getString(R.string.kailasanathar_temple_opening_hours),
                    context.getString(R.string.kailasanathar_temple_closing_hours),
                    context.getString(R.string.kailasanathar_temple_operating_days),

                ),
                Place(
                    context.getString(R.string.thillai_nataraja_temple_name),
                    "Cuddalore, Tamil Nadu",
                    context.getString(R.string.thillai_nataraja_temple_location),
                    context.getString(R.string.thillai_nataraja_temple_distance),
                    context.getString(R.string.thillai_nataraja_temple_description),
                    11.3990, 79.6930, // Approximate coordinates in Chidambaram
                    R.drawable.chidambaram1, R.drawable.chidambaram2, R.drawable.chidambaram3, R.drawable.chidambaram4,
                    context.getString(R.string.thillai_nataraja_temple_opening_hours),
                    context.getString(R.string.thillai_nataraja_temple_closing_hours),
                    context.getString(R.string.thillai_nataraja_temple_operating_days)
                ),
                Place(
                    context.getString(R.string.padmanabhapuram_palace_name),
                    "Kanyakumari, Tamilnadu",
                    context.getString(R.string.padmanabhapuram_palace_location),
                    context.getString(R.string.padmanabhapuram_palace_distance),
                    context.getString(R.string.padmanabhapuram_palace_description),
                    8.2500, 77.3260,
                    R.drawable.padmanabhapurampalace1, R.drawable.padmanabhapurampalace2, R.drawable.padmanabhapurampalace3, R.drawable.padmanabhapurampalace4,
                    context.getString(R.string.padmanabhapuram_palace_opening_hours),
                    context.getString(R.string.padmanabhapuram_palace_closing_hours),
                    context.getString(R.string.padmanabhapuram_palace_operating_days)
                ),
                Place(
                    context.getString(R.string.airavatesvara_temple_name),
                    "Thanjavur, Tamil Nadu",
                    context.getString(R.string.airavatesvara_temple_location),
                    context.getString(R.string.airavatesvara_temple_distance),
                    context.getString(R.string.airavatesvara_temple_description),
                    10.9480, 79.3560,
                    R.drawable.airavatesvara1, R.drawable.airavatesvara2, R.drawable.airavatesvara3, R.drawable.airavatesvara4,
                    context.getString(R.string.airavatesvara_temple_opening_hours),
                    context.getString(R.string.airavatesvara_temple_closing_hours),
                    context.getString(R.string.airavatesvara_temple_operating_days)
                ),
                Place(
                    context.getString(R.string.rock_fort_temple_name),
                    "Tiruchirappalli, Tamil Nadu",
                    context.getString(R.string.rock_fort_temple_location),
                    context.getString(R.string.rock_fort_temple_distance),
                    context.getString(R.string.rock_fort_temple_description),
                    10.8270, 78.6960, // Approximate coordinates in Tiruchirappalli
                    R.drawable.rockforttemple1, R.drawable.rockforttemple2, R.drawable.rockforttemple3, R.drawable.rockforttemple4,
                    context.getString(R.string.rock_fort_temple_opening_hours),
                    context.getString(R.string.rock_fort_temple_closing_hours),
                    context.getString(R.string.rock_fort_temple_operating_days)
                ),
                Place(
                    context.getString(R.string.gangaikonda_cholapuram_temple_name),
                    "Ariyalur, Tamil Nadu",
                    context.getString(R.string.gangaikonda_cholapuram_temple_location),
                    context.getString(R.string.gangaikonda_cholapuram_temple_distance),
                    context.getString(R.string.gangaikonda_cholapuram_temple_description),
                    11.2060, 79.4470, // Approximate coordinates near Jayankondam
                    R.drawable.gangaikonda1, R.drawable.gangaikonda2, R.drawable.gangaikonda3, R.drawable.gangaikonda4,
                    context.getString(R.string.gangaikonda_cholapuram_temple_opening_hours),
                    context.getString(R.string.gangaikonda_cholapuram_temple_closing_hours),
                    context.getString(R.string.gangaikonda_cholapuram_temple_operating_days)
                ),
                Place(
                    context.getString(R.string.vivekananda_rock_memorial_name),
                    "Kanyakumari, Tamil Nadu",
                    context.getString(R.string.vivekananda_rock_memorial_location),
                    context.getString(R.string.vivekananda_rock_memorial_distance),
                    context.getString(R.string.vivekananda_rock_memorial_description),
                    8.0780, 77.5550, // Approximate coordinates near Vivekananda Rock Memorial
                    R.drawable.vivekanandarock1, R.drawable.vivekanandarock2, R.drawable.vivekanandarock3, R.drawable.vivekanandarock4,
                    context.getString(R.string.vivekananda_rock_memorial_opening_hours),
                    context.getString(R.string.vivekananda_rock_memorial_closing_hours),
                    context.getString(R.string.vivekananda_rock_memorial_operating_days)
                )

            )
        }
    }
}