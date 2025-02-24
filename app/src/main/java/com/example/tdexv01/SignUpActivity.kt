package com.example.tdexv01

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : BaseActivity() {

    private lateinit var edtName: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var edtConfirmPassword: EditText
    private lateinit var btnSignUp: Button
    private lateinit var tvSignIn: TextView
    private lateinit var firebaseAuth: FirebaseAuth
    private val prefs by lazy { getSharedPreferences("OnboardingPrefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        supportActionBar?.hide()

                // Initialize Firebase Auth
                firebaseAuth = FirebaseAuth.getInstance()

                // Get UI elements
                edtName = findViewById(R.id.edtName)
                edtEmail = findViewById(R.id.edtEmail)
                edtPassword = findViewById(R.id.edtPassword)
                edtConfirmPassword = findViewById(R.id.edtConfirmPassword)
                btnSignUp = findViewById(R.id.btnSignUp)
                tvSignIn = findViewById(R.id.tvSignIn)

                // Handle Signup Button Click
                btnSignUp.setOnClickListener {
            val name = edtName.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()
            val confirmPassword = edtConfirmPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Register user with Firebase
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Update user profile with name
                        val user = firebaseAuth.currentUser
                        user?.updateProfile(com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build())?.addOnCompleteListener { profileUpdateTask ->
                            if (profileUpdateTask.isSuccessful) {
                                Toast.makeText(this, "Signup Successful!", Toast.LENGTH_SHORT).show()
                                // Check onboarding status
                                val hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding", false)
                                if (hasSeenOnboarding) {
                                    startActivity(Intent(this, MainActivity::class.java))
                                } else {
                                    startActivity(Intent(this, OnboardingActivity::class.java))
                                }
                                finish()
                            } else {
                                Toast.makeText(this, "Failed to update profile: ${profileUpdateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Signup Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

                // Navigate to Sign-in Page
                tvSignIn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }
}