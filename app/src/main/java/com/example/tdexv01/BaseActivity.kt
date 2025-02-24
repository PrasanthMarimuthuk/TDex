package com.example.tdexv01

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("Settings", MODE_PRIVATE)
        val savedLanguage = prefs.getString("Language", "en")
        val locale = when (savedLanguage) {
            "ta" -> Locale("ta", "IN")
            "hi" -> Locale("hi", "IN")
            "ml" -> Locale("ml", "IN")
            else -> Locale("en", "US")
        }
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }
}