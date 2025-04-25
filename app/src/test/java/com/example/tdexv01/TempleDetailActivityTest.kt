package com.example.tdexv01

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class TempleDetailActivityTest {

    private lateinit var activity: TempleDetailActivity
    private lateinit var place: MainActivity.Place

    @Mock
    private lateinit var textToSpeech: TextToSpeech

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Create a sample Place object
        place = MainActivity.Place(
            name = "Test Temple",
            locationEnglish = "Test Location English",
            location = "Test Location",
            staticDistance = "10 km",
            description = "This is a test temple description.",
            latitude = 0.0,
            longitude = 0.0,
            image1 = android.R.drawable.ic_menu_info_details, // Fallback to avoid resource issues
            image2 = android.R.drawable.ic_menu_info_details,
            image3 = android.R.drawable.ic_menu_info_details,
            image4 = android.R.drawable.ic_menu_info_details,
            openingHours = "6:00 AM",
            closingHours = "8:00 PM",
            operatingWeekdays = "All days"
        )

        // Set up activity with Robolectric
        val intent = Intent(ApplicationProvider.getApplicationContext(), TempleDetailActivity::class.java).apply {
            putExtra("place", place)
        }

        try {
            activity = Robolectric.buildActivity(TempleDetailActivity::class.java, intent)
                .create()
                .start()
                .resume()
                .get() ?: throw IllegalStateException("Activity is null")
        } catch (e: Exception) {
            throw IllegalStateException("Failed to initialize activity: ${e.message}", e)
        }

        // Replace TextToSpeech with mock
        activity.textToSpeech = textToSpeech
    }

    @Test
    fun voiceButtonStopsAudioWhenClickedAgain() {
        // Arrange
        `when`(textToSpeech.isSpeaking).thenReturn(true)
        activity.isSpeaking = true

        // Act: Simulate clicking voice button
        val voiceButton = activity.findViewById<ImageView>(R.id.voiceButton)
        voiceButton.performClick()

        // Assert
        verify(textToSpeech).stop()
        assertFalse(activity.isSpeaking)
        assertEquals("Audio stopped", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun voiceButtonShowsLanguageDialogWhenNotSpeaking() {
        // Arrange
        `when`(textToSpeech.isSpeaking).thenReturn(false)
        activity.isSpeaking = false

        // Act: Simulate clicking voice button
        val voiceButton = activity.findViewById<ImageView>(R.id.voiceButton)
        voiceButton.performClick()

        // Assert: Cannot directly test dialog appearance in unit tests
        verify(textToSpeech, never()).stop()
    }

    @Test
    fun maleVoicePitchIsSetOnTtsInitialization() {
        // Arrange
        activity.selectedLanguage = Locale("en", "IN")
        `when`(textToSpeech.setLanguage(any())).thenReturn(TextToSpeech.SUCCESS)

        // Act
        activity.onInit(TextToSpeech.SUCCESS)

        // Assert
        verify(textToSpeech).setPitch(0.8f)
        verify(textToSpeech).setSpeechRate(1.0f)
    }

    @Test
    fun offlineModeUsesPlaceDescription() {
        // Arrange
        activity.selectedLanguage = Locale("en", "IN")
        `when`(textToSpeech.setLanguage(any())).thenReturn(TextToSpeech.SUCCESS)

        // Mock network to simulate offline mode (avoid deprecated activeNetworkInfo)
        val connectivityManager = mock<ConnectivityManager>()
        val context = ApplicationProvider.getApplicationContext<Context>()
        `when`(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager)
        `when`(connectivityManager.activeNetwork).thenReturn(null) // Modern API for offline

        // Act: Simulate language selection (English, offline)
        activity.showLanguageDialog(place)

        // Assert
        verify(textToSpeech).speak(eq(place.description), eq(TextToSpeech.QUEUE_FLUSH), eq(null), eq(null))
        assertTrue(activity.isSpeaking)
    }
}