package com.example.tdexv01

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var profileImage: ImageView
    private lateinit var profileName: TextView
    private lateinit var profileEmail: TextView
    private lateinit var btnLogout: Button
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var cardEditProfile: CardView
    private lateinit var cardSettings: CardView
    private lateinit var cardSupport: CardView
    private lateinit var cardAboutUs: CardView
    private lateinit var chatbotCard: CardView // Changed from FloatingActionButton to CardView
    private val EDIT_PROFILE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()

        // Initialize UI components
        profileImage = findViewById(R.id.profileImage) ?: throw IllegalStateException("profileImage not found in layout")
        profileName = findViewById(R.id.profileName) ?: throw IllegalStateException("profileName not found in layout")
        profileEmail = findViewById(R.id.profileEmail) ?: throw IllegalStateException("profileEmail not found in layout")
        btnLogout = findViewById(R.id.btnLogout) ?: throw IllegalStateException("btnLogout not found in layout")
        bottomNavigationView = findViewById(R.id.bottomNavigationView) ?: throw IllegalStateException("bottomNavigationView not found in layout")
        cardEditProfile = findViewById(R.id.cardEditProfile) ?: throw IllegalStateException("cardEditProfile not found in layout")
        cardSettings = findViewById(R.id.cardSettings) ?: throw IllegalStateException("cardSettings not found in layout")
        cardSupport = findViewById(R.id.cardSupport) ?: throw IllegalStateException("cardSupport not found in layout")
        cardAboutUs = findViewById(R.id.cardAboutUs) ?: throw IllegalStateException("cardAboutUs not found in layout")
        chatbotCard = findViewById(R.id.chatbotCard) ?: throw IllegalStateException("chatbotCard not found in layout") // Updated ID

        // Load current user data
        val currentUser = auth.currentUser
        if (currentUser != null) {
            loadUserProfile(currentUser.uid)
        } else {
            Toast.makeText(this, getString(R.string.user_not_signed_in), Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }

        // Edit Profile Card
        cardEditProfile.setOnClickListener {
            startActivityForResult(Intent(this, EditProfileActivity::class.java), EDIT_PROFILE_REQUEST)
        }

        // Settings Card
        cardSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Support Card
        cardSupport.setOnClickListener {
            startActivity(Intent(this, SupportActivity::class.java))
        }

        // About Us Card
        cardAboutUs.setOnClickListener {
            startActivity(Intent(this, AboutUsActivity::class.java))
        }

        // Chatbot Card Click Listener (Navigate to ChatbotActivity without a specific place)
        chatbotCard.setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java)) // No "place" extra passed
        }

        // Logout Button
        btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, getString(R.string.logged_out_successfully), Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }

        // Bottom NavigationView Setup
        bottomNavigationView.menu.findItem(R.id.profile).setChecked(true)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    Toast.makeText(this, getString(R.string.home_clicked), Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.visited -> {
                    startActivity(Intent(this, VisitedPlacesActivity::class.java))
                    Toast.makeText(this, getString(R.string.visited_clicked), Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.tovisit -> {
                    startActivity(Intent(this, AddedPlacesActivity::class.java))
                    Toast.makeText(this, getString(R.string.to_visit_clicked), Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.profile -> {
                    Toast.makeText(this, getString(R.string.profile_clicked), Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_PROFILE_REQUEST && resultCode == RESULT_OK) {
            if (data?.getBooleanExtra("PROFILE_UPDATED", false) == true) {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    loadUserProfile(currentUser.uid)
                }
            }
        }
    }

    private fun loadUserProfile(userId: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            profileName.text = currentUser.displayName ?: getString(R.string.no_name)
            profileEmail.text = currentUser.email ?: getString(R.string.no_email)

            currentUser.photoUrl?.let { photoUrl ->
                Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .circleCrop()
                    .into(profileImage)
            } ?: run {
                profileImage.setImageResource(R.drawable.default_profile)
            }
        }
    }
}