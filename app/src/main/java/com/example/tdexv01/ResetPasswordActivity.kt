package com.example.tdexv01

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ResetPasswordActivity : BaseActivity() {

    private lateinit var edtEmail: EditText
    private lateinit var btnSendOtp: Button
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_reset_password)
        supportActionBar?.hide()

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Get UI elements
        edtEmail = findViewById(R.id.edtEmail)
        btnSendOtp = findViewById(R.id.btnSendOtp)

        // Handle Send OTP Button Click
        btnSendOtp.setOnClickListener {
            val email = edtEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Email is required!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if the user exists
            firebaseAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val signInMethods = task.result?.signInMethods ?: emptyList<String>()
                        if (signInMethods.isNotEmpty()) {
                            // User exists, send OTP
                            firebaseAuth.sendPasswordResetEmail(email)
                                .addOnCompleteListener { sendEmailTask ->
                                    if (sendEmailTask.isSuccessful) {
                                        Toast.makeText(this, "OTP sent to your email!", Toast.LENGTH_SHORT).show()
                                        // Redirect to SignInActivity
                                        val intent = Intent(this, SignInActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        Toast.makeText(this, "Failed to send OTP: ${sendEmailTask.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                        } else {
                            Toast.makeText(this, "No user found with this email!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Failed to check user: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}