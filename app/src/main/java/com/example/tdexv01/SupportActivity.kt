package com.example.tdexv01

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SupportActivity : BaseActivity() { // Changed BaseActivity to AppCompatActivity for clarity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_support)
        supportActionBar?.hide()

        // Set static content
        findViewById<TextView>(R.id.supportTitle).text = getString(R.string.how_can_we_help)
        findViewById<TextView>(R.id.contactUsTitle).text = getString(R.string.contact_us)
        findViewById<TextView>(R.id.contactEmail).text = getString(R.string.support_email, "prasanth.m.1107@gmail.com")
        findViewById<TextView>(R.id.contactPhone).text = getString(R.string.support_phone, "+91 9884374042")
        findViewById<TextView>(R.id.contactLocation).text = getString(R.string.support_location, "SNSCE, CBE-110")
        findViewById<TextView>(R.id.commitmentTitle).text = getString(R.string.our_commitment)
        findViewById<TextView>(R.id.commitmentText).text = getString(R.string.commitment_description)
        findViewById<TextView>(R.id.copyright).text = getString(R.string.copyright, "2025 T-dex")

        // Handle query submission
        val nameEditText = findViewById<EditText>(R.id.queryName)
        val emailEditText = findViewById<EditText>(R.id.queryEmail)
        val subjectEditText = findViewById<EditText>(R.id.querySubject)
        val messageEditText = findViewById<EditText>(R.id.queryMessage)
        val submitButton = findViewById<Button>(R.id.submitQueryButton)

        submitButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val subject = subjectEditText.text.toString().trim()
            val message = messageEditText.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || subject.isEmpty() || message.isEmpty()) {
                Toast.makeText(this, getString(R.string.all_fields_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Prepare email intent
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822" // MIME type for email
                putExtra(Intent.EXTRA_EMAIL, arrayOf("prasanth.m.1107@gmail.com")) // Recipient
                putExtra(Intent.EXTRA_SUBJECT, subject) // Subject from form
                putExtra(Intent.EXTRA_TEXT, """
                    Name: $name
                    Email: $email
                    Message: $message
                """.trimIndent()) // Body with form details
            }

            // Verify if there's an email app available and start the intent
            try {
                startActivity(Intent.createChooser(emailIntent, "Send email using..."))
                Toast.makeText(this, getString(R.string.query_submitted, name), Toast.LENGTH_SHORT).show()
                // Clear fields after launching the email intent
                nameEditText.text.clear()
                emailEditText.text.clear()
                subjectEditText.text.clear()
                messageEditText.text.clear()
            } catch (e: Exception) {
                Toast.makeText(this, "No email app found. Please install one.", Toast.LENGTH_LONG).show()
            }
        }
    }
}