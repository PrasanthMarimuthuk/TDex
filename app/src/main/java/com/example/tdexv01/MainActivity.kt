package com.example.tdexv01

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import android.os.Parcel
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import java.util.Locale

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

    // BLE Detection Variables
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val targetMacAddress = "FE:3D:ED:B1:A3:DE" // Your smartwatch MAC address (BLE tag)
    private val handler = Handler(Looper.getMainLooper())
    private var isDialogShown = false
    private var isInRange = false // Tracks if BLE tag is currently in range
    private var missedScanCount = 0 // Counts consecutive scans without detection
    private val MAX_MISSED_SCANS = 2 // Require 2 missed scans to confirm out-of-range
    private val SCAN_INTERVAL = 10_000L // 10 seconds between scan starts
    private val SCAN_DURATION = 8_000L // Scan for 8 seconds each time
    private val BLE_REQUEST_CODE = 1003
    private val TAG = "BLEDebug"
    private var wasDetectedInLastScan = false // Tracks detection in current scan cycle
    private val MARUTHAMALAI_REQUEST_CODE = 1004 // Unique request code for MaruthamalaiInfoActivity
    private var dontShowAgain = false // In-memory variable, resets on app restart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        // Check if user is signed in; if not, redirect to SignInActivity
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

        val popularPlacesCarousel: LinearLayout = findViewById(R.id.popularPlacesCarousel)

        // Request location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
        } else {
            getLocation()
        }

        updatePlacesList(Place.getAllPlaces(this))

        // Initialize SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().language + "-" + Locale.getDefault().country)
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
                val result = textToSpeech?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "Language not supported, using English", Toast.LENGTH_SHORT).show()
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
                place.name.lowercase().contains(searchText) ||
                        place.location.lowercase().contains(searchText)
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
           // Toast.makeText(this, getString(R.string.category_all), Toast.LENGTH_SHORT).show()
            speakRecommendation(getString(R.string.showing_all_places_near_coimbatore))
        }

        category5kmButton.setOnClickListener {
            selectCategoryButton(category5kmButton, arrayOf(categoryAllButton, category10kmButton, category15kmButton, category20kmButton))
            val filteredPlaces = Place.getAllPlaces(this).filter { place ->
                val distance = calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude)
                distance <= 5
            }
            currentCategoryFilter = { place -> calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude) <= 5 }
            updatePlacesList(filteredPlaces)
            noPlacesText.visibility = if (filteredPlaces.isEmpty()) View.VISIBLE else View.GONE
            placesRecyclerView.visibility = if (filteredPlaces.isNotEmpty()) View.VISIBLE else View.GONE
           // Toast.makeText(this, getString(R.string.category_5_km), Toast.LENGTH_SHORT).show()
            speakRecommendation(getString(R.string.showing_places_within_5_kilometers))
            //filteredPlaces.forEach { place -> addPlaceToList(place) }
        }

        category10kmButton.setOnClickListener {
            selectCategoryButton(category10kmButton, arrayOf(categoryAllButton, category5kmButton, category15kmButton, category20kmButton))
            val filteredPlaces = Place.getAllPlaces(this).filter { place ->
                val distance = calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude)
                distance <= 10
            }
            currentCategoryFilter = { place -> calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude) <= 10 }
            updatePlacesList(filteredPlaces)
            noPlacesText.visibility = if (filteredPlaces.isEmpty()) View.VISIBLE else View.GONE
            placesRecyclerView.visibility = if (filteredPlaces.isNotEmpty()) View.VISIBLE else View.GONE
            //Toast.makeText(this, getString(R.string.category_10_km), Toast.LENGTH_SHORT).show()
            speakRecommendation(getString(R.string.showing_places_within_10_kilometers))
            //filteredPlaces.forEach { place -> addPlaceToList(place) }
        }

        category15kmButton.setOnClickListener {
            selectCategoryButton(category15kmButton, arrayOf(categoryAllButton, category5kmButton, category10kmButton, category20kmButton))
            val filteredPlaces = Place.getAllPlaces(this).filter { place ->
                val distance = calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude)
                distance <= 15
            }
            currentCategoryFilter = { place -> calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude) <= 15 }
            updatePlacesList(filteredPlaces)
            noPlacesText.visibility = if (filteredPlaces.isEmpty()) View.VISIBLE else View.GONE
            placesRecyclerView.visibility = if (filteredPlaces.isNotEmpty()) View.VISIBLE else View.GONE
            //Toast.makeText(this, getString(R.string.category_15_km), Toast.LENGTH_SHORT).show()
            speakRecommendation(getString(R.string.showing_places_within_15_kilometers))
            //filteredPlaces.forEach { place -> addPlaceToList(place) }
        }

        category20kmButton.setOnClickListener {
            selectCategoryButton(category20kmButton, arrayOf(categoryAllButton, category5kmButton, category10kmButton, category15kmButton))
            val filteredPlaces = Place.getAllPlaces(this).filter { place ->
                val distance = calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude)
                distance <= 20
            }
            currentCategoryFilter = { place -> calculateDistance(currentLatitude, currentLongitude, place.latitude, place.longitude) <= 20 }
            updatePlacesList(filteredPlaces)
            noPlacesText.visibility = if (filteredPlaces.isEmpty()) View.VISIBLE else View.GONE
            placesRecyclerView.visibility = if (filteredPlaces.isNotEmpty()) View.VISIBLE else View.GONE
            //Toast.makeText(this, getString(R.string.category_20_km), Toast.LENGTH_SHORT).show()
            speakRecommendation(getString(R.string.showing_places_within_20_kilometers))
            //filteredPlaces.forEach { place -> addPlaceToList(place) }
        }

        // Card Click Listeners
        val templeCard1: CardView = findViewById(R.id.BrahidheerwararTemple)
        templeCard1.setOnClickListener {
            val place = Place.getAllPlaces(this)[0]
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("place", place)
            startActivity(intent)
        }

        val templeCard2: CardView = findViewById(R.id.MeenakshiTemple)
        templeCard2.setOnClickListener {
            val place = Place.getAllPlaces(this)[1]
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("place", place)
            startActivity(intent)
        }

        val templeCard3: CardView = findViewById(R.id.Velligirihills)
        templeCard3.setOnClickListener {
            val place = Place.getAllPlaces(this)[2]
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("place", place)
            startActivity(intent)
        }

        val templeCard4: CardView = findViewById(R.id.GassMuseum)
        templeCard4.setOnClickListener {
            val place = Place.getAllPlaces(this)[3]
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("place", place)
            startActivity(intent)
        }

        val templeCard5: CardView = findViewById(R.id.MoneyFalls)
        templeCard5.setOnClickListener {
            val place = Place.getAllPlaces(this)[4]
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("place", place)
            startActivity(intent)
        }

        val templeCard6: CardView = findViewById(R.id.GDmuseum)
        templeCard6.setOnClickListener {
            val place = Place.getAllPlaces(this)[5]
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("place", place)
            startActivity(intent)
        }

        val templeCard7: CardView = findViewById(R.id.WildlifeSathi)
        templeCard7.setOnClickListener {
            val place = Place.getAllPlaces(this)[6]
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("place", place)
            startActivity(intent)
        }

        val templeCard8: CardView = findViewById(R.id.GovmentMuseum)
        templeCard8.setOnClickListener {
            val place = Place.getAllPlaces(this)[7]
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("place", place)
            startActivity(intent)
        }

        val templeCard9: CardView = findViewById(R.id.Valparai)
        templeCard9.setOnClickListener {
            val place = Place.getAllPlaces(this)[8]
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("place", place)
            startActivity(intent)
        }

        val templeCard10: CardView = findViewById(R.id.PerurEeswaran)
        templeCard10.setOnClickListener {
            val place = Place.getAllPlaces(this)[9]
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("place", place)
            startActivity(intent)
        }

        // Bottom NavigationView Setup
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.menu.findItem(R.id.home).setChecked(true)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    true
                }
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

        // Initialize dontShowAgain as false (resets on app start)
        dontShowAgain = false

        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, getString(R.string.please_enable_bluetooth), Toast.LENGTH_LONG).show()
            return
        }

        // Request BLE and location permissions
        requestBlePermissions()
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
        if (requestCode == BLE_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startPeriodicBleScan()
        } else if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation()
            } else {
                Toast.makeText(this, getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == 1003) {
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
        } else {
            Toast.makeText(this, getString(R.string.ble_permissions_denied), Toast.LENGTH_LONG).show()
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
        val txPower = -51 // Calibrated for -65 dBm at 2 meters
        if (rssi == 0) return -1.0
        val n = 2.0 // Path loss exponent
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
            setOnCheckedChangeListener { _, isChecked ->
                dontShowAgain = isChecked // Only updates in-memory, resets on app close
            }
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
            setOnCheckedChangeListener { _, isChecked ->
                dontShowAgain = isChecked // Only updates in-memory, resets on app close
            }
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
            place.name.lowercase().contains(searchQuery) ||
                    place.location.lowercase().contains(searchQuery) ||
                    place.description.lowercase().contains(searchQuery)
        }

        if (matchingPlace != null) {
            val distance = calculateDistance(currentLatitude, currentLongitude, matchingPlace.latitude, matchingPlace.longitude)
            val intent = Intent(this, TempleDetailActivity::class.java)
            intent.putExtra("place", matchingPlace)
            startActivity(intent)
            speakRecommendation(getString(R.string.found_place, matchingPlace.name, matchingPlace.location, String.format("%.1f", distance)))
        } else {
            Toast.makeText(this, getString(R.string.place_not_found, searchQuery), Toast.LENGTH_SHORT).show()
            speakRecommendation(getString(R.string.couldnt_find_place, searchQuery))
        }
    }

    private fun speakRecommendation(text: String) {
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
        val placesToDisplay = currentCategoryFilter?.let { filter -> filteredPlaces.filter(filter) } ?: filteredPlaces
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
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Check if location permissions are granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
            return
        }

        // Create location request
        val locationRequest = LocationRequest.create().apply {
            interval = 5000 // Update interval in milliseconds
            fastestInterval = 2000 // Fastest update interval
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // Define location callback
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    Log.d("MainActivity", "Location updated: Lat=$currentLatitude, Lon=$currentLongitude")
                    // Update UI with new location
                    val filteredPlaces = currentCategoryFilter?.let { filter ->
                        Place.getAllPlaces(this@MainActivity).filter(filter)
                    } ?: Place.getAllPlaces(this@MainActivity)
                    updatePlacesList(filteredPlaces)
                    // Optionally stop updates if you only need one update
                    fusedLocationClient.removeLocationUpdates(this)
                } ?: run {
                    Log.e("MainActivity", "Location result is null")
                }
            }
        }

        // Request location updates
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Failed to request location updates: ${e.message}")
                    Toast.makeText(this, "Failed to get location: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: SecurityException) {
            Log.e("MainActivity", "Security exception: ${e.message}")
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }

        // Fallback: Try to get last known location if real-time updates fail
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    Log.d("MainActivity", "Last known location: Lat=$currentLatitude, Lon=$currentLongitude")
                    val filteredPlaces = currentCategoryFilter?.let { filter ->
                        Place.getAllPlaces(this@MainActivity).filter(filter)
                    } ?: Place.getAllPlaces(this@MainActivity)
                    updatePlacesList(filteredPlaces)
                } else {
                    Log.w("MainActivity", "Last known location is null")
                    Toast.makeText(this, "Unable to get last known location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Failed to get last location: ${e.message}")
            }
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
        val location: String,
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
                    context.getString(R.string.maruthamalai_temple_location),
                    context.getString(R.string.maruthamalai_temple_distance),
                    context.getString(R.string.maruthamalai_temple_description),
                    11.0139, 76.8028,
                    R.drawable.maruthamalai_1, R.drawable.maruthamalai_2, R.drawable.maruthamalai_3, R.drawable.marudhamalai_4,
                    context.getString(R.string.maruthamalai_temple_opening_hours),
                    context.getString(R.string.maruthamalai_temple_closing_hours),
                    context.getString(R.string.maruthamalai_temple_operating_days)
                ),
                Place(
                    context.getString(R.string.isha_yoga_centre_name),
                    context.getString(R.string.isha_yoga_centre_location),
                    context.getString(R.string.isha_yoga_centre_distance),
                    context.getString(R.string.isha_yoga_centre_description),
                    10.9711, 76.8259,
                    R.drawable.isha1, R.drawable.isha2, R.drawable.isha3, R.drawable.isha4,
                    context.getString(R.string.isha_yoga_centre_opening_hours),
                    context.getString(R.string.isha_yoga_centre_closing_hours),
                    context.getString(R.string.isha_yoga_centre_operating_days)
                ),
                Place(
                    context.getString(R.string.velliangiri_hills_name),
                    context.getString(R.string.velliangiri_hills_location),
                    context.getString(R.string.velliangiri_hills_distance),
                    context.getString(R.string.velliangiri_hills_description),
                    10.9722, 76.8317,
                    R.drawable.velliangiri1, R.drawable.velliangiri2, R.drawable.velliangiri3, R.drawable.velliangiri4,
                    context.getString(R.string.velliangiri_hills_opening_hours),
                    context.getString(R.string.velliangiri_hills_closing_hours),
                    context.getString(R.string.velliangiri_hills_operating_days)
                ),
                Place(
                    context.getString(R.string.gass_forest_museum_name),
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
                    context.getString(R.string.perur_pateeswarar_temple_location),
                    context.getString(R.string.perur_pateeswarar_temple_distance),
                    context.getString(R.string.perur_pateeswarar_temple_description),
                    10.9944, 76.9289,
                    R.drawable.perurpateeswarartemple1, R.drawable.perurpateeswarartemple2, R.drawable.perurpateeswarartemple3, R.drawable.perurpateeswarartemple4,
                    context.getString(R.string.perur_pateeswarar_temple_opening_hours),
                    context.getString(R.string.perur_pateeswarar_temple_closing_hours),
                    context.getString(R.string.perur_pateeswarar_temple_operating_days)
                )
            )
        }
    }
}