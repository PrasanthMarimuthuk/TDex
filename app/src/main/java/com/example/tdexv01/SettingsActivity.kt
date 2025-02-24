package com.example.tdexv01

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

        // Populate language spinner
        val languages = arrayOf("English", "Tamil", "Hindi", "Malayalam")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = adapter

        // Set default language based on current locale
        val currentLocale = Locale.getDefault().language
        when (currentLocale) {
            "ta" -> spinnerLanguage.setSelection(1) // Tamil
            "hi" -> spinnerLanguage.setSelection(2) // Hindi
            "ml" -> spinnerLanguage.setSelection(3) // Malayalam
            else -> spinnerLanguage.setSelection(0) // English
        }

        // Apply language change
        btnApplyLanguage.setOnClickListener {
            val selectedLanguage = spinnerLanguage.selectedItem.toString()
            val locale = when (selectedLanguage) {
                "Tamil" -> Locale("ta", "IN")
                "Hindi" -> Locale("hi", "IN")
                "Malayalam" -> Locale("ml", "IN")
                else -> Locale("en", "US") // English
            }

            setLocale(locale)
            recreate() // Recreate activity to apply new language
        }
    }

    private fun setLocale(locale: Locale) {
        val config = Configuration()
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        // Save the selected language preference (optional, using SharedPreferences)
        val prefs = getSharedPreferences("Settings", MODE_PRIVATE)
        prefs.edit().putString("Language", locale.language).apply()
    }

    // Load saved language preference on app start (optional)
    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("Settings", MODE_PRIVATE)
        val savedLanguage = prefs.getString("Language", "en")
        val locale = when (savedLanguage) {
            "ta" -> Locale("ta", "IN")
            "hi" -> Locale("hi", "IN")
            "ml" -> Locale("ml", "IN")
            else -> Locale("en", "US")
        }
        setLocale(locale)
    }
}