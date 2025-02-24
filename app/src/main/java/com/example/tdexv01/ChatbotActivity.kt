package com.example.tdexv01

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

class ChatbotActivity : BaseActivity(), TextToSpeech.OnInitListener, AdapterView.OnItemSelectedListener {

    private lateinit var chatInput: EditText
    private lateinit var sendButton: ImageView
    private lateinit var voiceChatButton: ImageView
    private lateinit var chatHistory: LinearLayout
    private lateinit var languageSpinner: Spinner
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private var isListening = false
    private var selectedLanguage: Locale = Locale.ENGLISH // Default to English
    private val GEMINI_API_KEY = "AIzaSyDIz1wCg-671yxKnye5P9cGB7Qk_tRPYTg" // Replace with your free Gemini API key

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)
        supportActionBar?.hide()

        chatInput = findViewById(R.id.chatInput)
        sendButton = findViewById(R.id.sendButton)
        voiceChatButton = findViewById(R.id.voiceChatButton)
        chatHistory = findViewById(R.id.chatHistory)
        languageSpinner = findViewById(R.id.languageSpinner)

        // Get the current Place from the intent (optional)
        val currentPlace = intent.getParcelableExtra<Place>("place")

        // Set up language spinner
        val languages = arrayOf("English", "Tamil", "Hindi", "Malayalam")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter
        languageSpinner.onItemSelectedListener = this

        // Initialize SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        updateRecognizerIntent()

        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(this, this)

        // Send Button Click Listener
        sendButton.setOnClickListener {
            val message = chatInput.text.toString().trim()
            if (message.isNotEmpty()) {
                addMessageToHistory("You: $message", true)
                sendMessage(message, currentPlace)
                chatInput.text.clear()
            }
        }

        // Voice Chat Button Click Listener (Toggle Listen/Stop)
        voiceChatButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1007)
            } else {
                if (isListening) {
                    speechRecognizer.stopListening()
                    Toast.makeText(this, "Stopping listening...", Toast.LENGTH_SHORT).show()
                } else {
                    updateRecognizerIntent()
                    speechRecognizer.startListening(getRecognizerIntent())
                    isListening = true
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(selectedLanguage)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language not supported, using English", Toast.LENGTH_SHORT).show()
                textToSpeech.setLanguage(Locale.US)
                selectedLanguage = Locale.US
            }
        } else {
            Toast.makeText(this, "Text-to-speech initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        selectedLanguage = when (position) {
            0 -> Locale.ENGLISH
            1 -> Locale("ta") // Tamil
            2 -> Locale("hi") // Hindi
            3 -> Locale("ml") // Malayalam
            else -> Locale.ENGLISH
        }
        textToSpeech.setLanguage(selectedLanguage)
        updateRecognizerIntent()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        selectedLanguage = Locale.ENGLISH
        textToSpeech.setLanguage(selectedLanguage)
        updateRecognizerIntent()
    }

    private fun sendMessage(message: String, place: Place? = null) {
        runBlocking {
            val aiResponse = if (place != null) {
                getGeminiResponseWithContext(message, place)
            } else {
                getGeminiResponseWithAllPlaces(message)
            }
            val targetLangCode = when (selectedLanguage.language) {
                "en" -> "en"
                "ta" -> "ta"
                "hi" -> "hi"
                "ml" -> "ml"
                else -> "en"
            }
            val translatedResponse = if (targetLangCode == "en") aiResponse else translateText(aiResponse, targetLangCode)
            addMessageToHistory("Bot: $translatedResponse", false)
            speakResponse(translatedResponse)
        }
    }

    private suspend fun getGeminiResponseWithContext(query: String, place: Place): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$GEMINI_API_KEY")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val context = "You are a chatbot for a temple tourism app. Respond in English with concise answers using the following information: Place Name: ${place.name}, Location: ${place.location}, Opening Hours: ${place.openingHours}, Closing Hours: ${place.closingHours}, Operating Weekdays: ${place.operatingWeekdays}, Description: ${place.description}, User Query: $query"

            val requestBody = """
                {
                    "contents": [{
                        "parts": [{
                            "text": "$context"
                        }]
                    }],
                    "generationConfig": {
                        "maxOutputTokens": 50,
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
                return@withContext "Sorry, I couldn’t process your request."
            } else {
                val error = connection.errorStream.bufferedReader().use { it.readText() }
                Log.e("GeminiAPI", "Error: $responseCode - $error")
                return@withContext "Error processing your request. Please try again."
            }
        } catch (e: IOException) {
            Log.e("GeminiAPI", "Network error: ${e.message}")
            return@withContext "Network error. Please check your internet connection."
        }
    }

    private suspend fun getGeminiResponseWithAllPlaces(query: String): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$GEMINI_API_KEY")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Build context with all places
            val allPlaces = MainActivity.Place.getAllPlaces(this@ChatbotActivity)
            val placesContext = allPlaces.joinToString(separator = "; ") { place ->
                "Place Name: ${place.name}, Location: ${place.location}, Description: ${place.description}, Opening Hours: ${place.openingHours}, Closing Hours: ${place.closingHours}, Operating Weekdays: ${place.operatingWeekdays}"
            }
            val context = "You are a chatbot for a temple tourism app. Answer any question in English based on the following information about all places: $placesContext. User Query: $query"

            val requestBody = """
                {
                    "contents": [{
                        "parts": [{
                            "text": "$context"
                        }]
                    }],
                    "generationConfig": {
                        "maxOutputTokens": 50,
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
                return@withContext "Sorry, I couldn’t process your request."
            } else {
                val error = connection.errorStream.bufferedReader().use { it.readText() }
                Log.e("GeminiAPI", "Error: $responseCode - $error")
                return@withContext "Error processing your request. Please try again."
            }
        } catch (e: IOException) {
            Log.e("GeminiAPI", "Network error: ${e.message}")
            return@withContext "Network error. Please check your internet connection."
        }
    }

    private suspend fun getGeminiResponse(query: String): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$GEMINI_API_KEY")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val requestBody = """
                {
                    "contents": [{
                        "parts": [{
                            "text": "$query"
                        }]
                    }],
                    "generationConfig": {
                        "maxOutputTokens": 50,
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
                return@withContext "Sorry, I couldn’t process your request."
            } else {
                val error = connection.errorStream.bufferedReader().use { it.readText() }
                Log.e("GeminiAPI", "Error: $responseCode - $error")
                return@withContext "Error processing your request. Please try again."
            }
        } catch (e: IOException) {
            Log.e("GeminiAPI", "Network error: ${e.message}")
            return@withContext "Network error. Please check your internet connection."
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

    private fun addMessageToHistory(message: String, isUser: Boolean) {
        val textView = TextView(this).apply {
            text = message
            textSize = 16f
            setPadding(16, 12, 16, 12)
            setBackgroundResource(if (isUser) R.drawable.user_message_bg else R.drawable.bot_message_bg)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = if (isUser) android.view.Gravity.END else android.view.Gravity.START
                marginStart = if (isUser) 16 else 8
                marginEnd = if (isUser) 8 else 16
                topMargin = 8
                bottomMargin = 8
            }
        }
        chatHistory.addView(textView)
    }

    private fun speakResponse(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun updateRecognizerIntent() {
        speechRecognizer.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Toast.makeText(this@ChatbotActivity, "Listening...", Toast.LENGTH_SHORT).show()
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Toast.makeText(this@ChatbotActivity, "Speech recognition error: $error", Toast.LENGTH_SHORT).show()
                isListening = false
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { spokenText ->
                    chatInput.setText(spokenText)
                    sendMessage(spokenText, intent.getParcelableExtra<Place>("place"))
                }
                isListening = false
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun getRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, when (selectedLanguage.language) {
                "en" -> "en-US"
                "ta" -> "ta-IN"
                "hi" -> "hi-IN"
                "ml" -> "ml-IN"
                else -> "en-US"
            })
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1007) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isListening) {
                    speechRecognizer.stopListening()
                    Toast.makeText(this, "Stopping listening...", Toast.LENGTH_SHORT).show()
                } else {
                    updateRecognizerIntent()
                    speechRecognizer.startListening(getRecognizerIntent())
                    isListening = true
                }
            } else {
                Toast.makeText(this, "Microphone permission required for voice input", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        textToSpeech.shutdown()
    }
}