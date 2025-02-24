package com.example.tdexv01

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class SignInActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001 // Google Sign-In Request Code
    private val TAG = "SignInActivity"
    private val prefs by lazy { getSharedPreferences("OnboardingPrefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()

        // Check if user is already signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "User already signed in: ${currentUser.email}")
            val hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding", false)
            if (hasSeenOnboarding) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                startActivity(Intent(this, OnboardingActivity::class.java))
            }
            finish()
            return
        }

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // UI Elements
        val emailField = findViewById<EditText>(R.id.edtEmail)
        val passwordField = findViewById<EditText>(R.id.edtPassword)
        val btnSignIn = findViewById<Button>(R.id.btnSignIn)
        val btnGoogleSignIn = findViewById<Button>(R.id.btnGoogleSignIn)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Email/Password Sign-In
        btnSignIn.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = ProgressBar.VISIBLE

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    progressBar.visibility = ProgressBar.GONE
                    if (task.isSuccessful) {
                        Log.d(TAG, "Email/Password Sign-In Successful")
                        Toast.makeText(this, "Sign-In Successful!", Toast.LENGTH_SHORT).show()
                        val hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding", false)
                        if (hasSeenOnboarding) {
                            startActivity(Intent(this, MainActivity::class.java))
                        } else {
                            startActivity(Intent(this, OnboardingActivity::class.java))
                        }
                        finish()
                    } else {
                        Log.e(TAG, "Email/Password Sign-In Failed", task.exception)
                        Toast.makeText(this, "Sign-In Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Google Sign-In with account picker
        btnGoogleSignIn.setOnClickListener {
            // Check if Google Play Services is available
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
            if (resultCode != ConnectionResult.SUCCESS) {
                Toast.makeText(this, "Google Play Services unavailable", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            progressBar.visibility = ProgressBar.VISIBLE
            // Sign out to force account selection
            googleSignInClient.signOut().addOnCompleteListener {
                Log.d(TAG, "Signed out from previous Google account")
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }

        // Navigate to Sign-Up Page
        tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
    }

    // Handle Google Sign-In Result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "Google Sign-In Successful: ${account.email}")
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {

                Log.e(TAG, "Google Sign-In Failed", e)
                Toast.makeText(this, "Google Sign-In Failed: ${e.statusCode} - ${e.message}", Toast.LENGTH_SHORT).show()
                if (e.statusCode == CommonStatusCodes.DEVELOPER_ERROR) {
                    Toast.makeText(this, "Check SHA-1 fingerprint and OAuth client ID in Firebase console", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase Authentication with Google Successful")
                    Toast.makeText(this, "Google Sign-In Successful!", Toast.LENGTH_SHORT).show()
                    val hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding", false)
                    if (hasSeenOnboarding) {
                        startActivity(Intent(this, MainActivity::class.java))
                    } else {
                        startActivity(Intent(this, OnboardingActivity::class.java))
                    }
                    finish()
                } else {
                    Log.e(TAG, "Firebase Authentication with Google Failed", task.exception)
                    Toast.makeText(this, "Google Sign-In Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}