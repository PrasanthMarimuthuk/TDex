package com.example.tdexv01

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.hide()

        // Initialize UI components
        val spinnerLanguage = findViewById<Spinner>(R.id.spinnerLanguage)
        val btnApplyLanguage = findViewById<Button>(R.id.btnApplyLanguage)

        // Define language options (display name and corresponding language code)
        val languages = listOf(
            "English" to "en",
            "Tamil" to "ta",
            "Hindi" to "hi",
            "Malayalam" to "ml",
            "French" to "fr",
            "Spanish" to "es",
            "Japanese" to "ja",
            "Korean" to "ko",
            "Malay" to "ms",
            "Telugu" to "te",
            "Marathi" to "mr",
            "German" to "de",
            "Bengali" to "bn",
            "Gujarati" to "gu",
            "Kannada" to "kn",
            "Chinese" to "zh"
        )

        // Populate language spinner with display names
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            languages.map { it.first }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = adapter

        // Set default language based on saved preference
        val prefs = getSharedPreferences("Settings", MODE_PRIVATE)
        val savedLanguage = prefs.getString("Language", "en") // Default to English
        val defaultPosition = languages.indexOfFirst { it.second == savedLanguage }
        spinnerLanguage.setSelection(defaultPosition.coerceAtLeast(0))

        // Apply language change
        btnApplyLanguage.setOnClickListener {
            val selectedPosition = spinnerLanguage.selectedItemPosition
            val selectedLanguageCode = languages[selectedPosition].second
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
                "bn" -> Locale("bn", "IN") // Bengali
                "gu" -> Locale("gu", "IN") // Gujarati
                "kn" -> Locale("kn", "IN") // Kannada
                "zh" -> Locale("zh", "CN") // Chinese (Simplified)
                else -> Locale("en", "US") // English
            }

            setLocale(locale)
            // Restart the app to apply the new language
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }
    }

    private fun setLocale(locale: Locale) {
        // Save the selected language preference
        val prefs = getSharedPreferences("Settings", MODE_PRIVATE)
        prefs.edit().putString("Language", locale.language).apply()

        // Note: Actual locale application is handled by BaseActivity's attachBaseContext
        // This method only updates the preference to ensure consistency
    }

    // Load saved language preference on app start
    override fun onResume() {
        super.onResume()
        // Language is already applied by BaseActivity's attachBaseContext
        // No need to reapply locale here, but we can ensure the spinner reflects the current language
        val prefs = getSharedPreferences("Settings", MODE_PRIVATE)
        val savedLanguage = prefs.getString("Language", "en")
        val spinnerLanguage = findViewById<Spinner>(R.id.spinnerLanguage)
        val languages = listOf(
            "English" to "en",
            "Tamil" to "ta",
            "Hindi" to "hi",
            "Malayalam" to "ml",
            "French" to "fr",
            "Spanish" to "es",
            "Japanese" to "ja",
            "Korean" to "ko",
            "Malay" to "ms",
            "Telugu" to "te",
            "Marathi" to "mr",
            "German" to "de",
            "Kannada" to "kn",

        )
        val currentPosition = languages.indexOfFirst { it.second == savedLanguage }
        spinnerLanguage.setSelection(currentPosition.coerceAtLeast(0))
    }
}