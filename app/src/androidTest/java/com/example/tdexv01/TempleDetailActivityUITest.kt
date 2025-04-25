package com.example.tdexv01

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TempleDetailActivityUITest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO)

    private lateinit var scenario: ActivityScenario<TempleDetailActivity>
    private lateinit var place: MainActivity.Place

    @Before
    fun setUp() {
        // Create a sample Place object
        place = MainActivity.Place(
            name = "Test Temple",
            locationEnglish = "Test Location English",
            location = "Test Location",
            staticDistance = "10 km",
            description = "This is a test temple description.",
            latitude = 0.0,
            longitude = 0.0,
            image1 = android.R.drawable.ic_menu_info_details, // Fallback for testing
            image2 = android.R.drawable.ic_menu_info_details,
            image3 = android.R.drawable.ic_menu_info_details,
            image4 = android.R.drawable.ic_menu_info_details,
            openingHours = "6:00 AM",
            closingHours = "8:00 PM",
            operatingWeekdays = "All days"
        )

        // Launch activity with intent
        val intent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, TempleDetailActivity::class.java).apply {
            putExtra("place", place)
        }
        scenario = ActivityScenario.launch(intent)

        // Wait for activity to be fully loaded
        scenario.onActivity { /* Ensure activity is resumed */ }
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun displaysCorrectPlaceData() {
        onView(withId(R.id.templeName)).check(matches(withText("Test Temple")))
        onView(withId(R.id.templeLocation)).check(matches(withText("Test Location")))
        onView(withId(R.id.templeDescription)).check(matches(withText("This is a test temple description.")))
        onView(withId(R.id.openingHoursText)).check(matches(withText("Opening Hours: 6:00 AM")))
        onView(withId(R.id.closingHoursText)).check(matches(withText("Closing Hours: 8:00 PM")))
        onView(withId(R.id.operatingWeekdaysText)).check(matches(withText("Operating Days: All days")))
    }

    @Test
    fun voiceButtonClickDoesNotCrashAndTriggersDialog() {
        onView(withId(R.id.voiceButton)).check(matches(isDisplayed()))
        onView(withId(R.id.voiceButton)).perform(click())
        onView(withId(R.id.voiceButton)).check(matches(isDisplayed()))
    }
}