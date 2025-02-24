package com.example.tdexv01

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class EditProfileActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var editName: EditText
    private lateinit var editCountry: EditText
    private lateinit var editState: EditText
    private lateinit var editDob: EditText
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        // Initialize UI components with null safety
        editName = findViewById(R.id.editProfileName) ?: run {
            throw IllegalStateException("editProfileName not found in layout")
        }
        editCountry = findViewById(R.id.editProfileCountry) ?: run {
            throw IllegalStateException("editProfileCountry not found in layout")
        }
        editState = findViewById(R.id.editProfileState) ?: run {
            throw IllegalStateException("editProfileState not found in layout")
        }
        editDob = findViewById(R.id.editDob) ?: run {
            throw IllegalStateException("editProfileDob not found in layout")
        }
        btnSave = findViewById(R.id.btnSaveProfile) ?: run {
            throw IllegalStateException("btnSaveProfile not found in layout")
        }

        // Load current user data from Firebase
        if (currentUser != null) {
            Glide.with(this)
                .load(currentUser.photoUrl)
                .placeholder(R.drawable.default_profile)
                .error(R.drawable.default_profile)
                .into(findViewById(R.id.editProfileImage)) // Display Google account photo but disable changes
            editName.setText(currentUser.displayName ?: "No Name")
            loadAdditionalProfileData(currentUser.uid)
        }

        // Set click listener for DOB to open DatePicker
        editDob.setOnClickListener {
            showDatePickerDialog()
        }

        // Save Button
        btnSave.setOnClickListener {
            saveUserProfile()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            editDob.setText(selectedDate)
        }, year, month, day).show()
    }

    private fun saveUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val name = editName.text.toString().trim()
            val country = editCountry.text.toString().trim()
            val state = editState.text.toString().trim()
            val dob = editDob.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
                return
            }

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()

            currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Save country, state, and DOB to Firebase Realtime Database
                        saveAdditionalProfileData(currentUser.uid, country, state, dob)
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        // Notify ProfileActivity of the change
                        setResult(RESULT_OK, Intent().putExtra("PROFILE_UPDATED", true))
                        finish() // Return to ProfileActivity
                    } else {
                        Toast.makeText(this, "Failed to update profile: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun saveAdditionalProfileData(userId: String, country: String, state: String, dob: String) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("users").child(userId)

        val userData = mapOf(
            "country" to country,
            "state" to state,
            "dob" to dob
        )

        userRef.updateChildren(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Additional profile data saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save additional data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadAdditionalProfileData(userId: String) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("users").child(userId)

        userRef.get().addOnSuccessListener { snapshot ->
            val country = snapshot.child("country").getValue(String::class.java) ?: ""
            val state = snapshot.child("state").getValue(String::class.java) ?: ""
            val dob = snapshot.child("dob").getValue(String::class.java) ?: ""

            editCountry.setText(country)
            editState.setText(state)
            editDob.setText(dob)
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to load additional data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}