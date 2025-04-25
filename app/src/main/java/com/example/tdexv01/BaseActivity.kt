package com.example.tdexv01

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("Settings", MODE_PRIVATE)
        val savedLanguage = prefs.getString("Language", "en") // Default to English
        val locale = when (savedLanguage) {
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
            else -> Locale("en", "US") // Default to English
        }
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }
}