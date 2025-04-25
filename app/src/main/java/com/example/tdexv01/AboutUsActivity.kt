package com.example.tdexv01

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView


class AboutUsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)
        supportActionBar?.hide()

        // Set static content
        findViewById<TextView>(R.id.aboutUsTitle).text = getString(R.string.our_team)
        findViewById<TextView>(R.id.teamSubtitle).text = getString(R.string.meet_the_faces_behind_tdexv01)
        findViewById<TextView>(R.id.missionTitle).text = getString(R.string.our_mission)
        findViewById<TextView>(R.id.missionText).text = getString(R.string.mission_description)
        findViewById<TextView>(R.id.commitmentTitle).text = getString(R.string.our_commitment)
        findViewById<TextView>(R.id.commitmentText).text = getString(R.string.commitment_description)
        findViewById<TextView>(R.id.copyright).text = getString(R.string.copyright, "2025 T-Dex")

        // Set team member images and names (using drawable resources for simplicity)
        findViewById<ImageView>(R.id.prasanthImage).setImageResource(R.drawable.prasanth) // Add prasanth_m.png to drawable
        findViewById<TextView>(R.id.prasanthName).text = getString(R.string.prasanth_m)
        findViewById<TextView>(R.id.prasanthRole).text = getString(R.string.lead_developer)

        findViewById<ImageView>(R.id.ragulImage).setImageResource(R.drawable.ragul) // Add deepika_s.png to drawable
        findViewById<TextView>(R.id.ragulName).text = getString(R.string.ragul_r)
        findViewById<TextView>(R.id.ragulRole).text = getString(R.string.developer)

        findViewById<ImageView>(R.id.sivaImage).setImageResource(R.drawable.siva) // Add lugeeban_s.png to drawable
        findViewById<TextView>(R.id.sivaName).text = getString(R.string.sivapprakash_c)
        findViewById<TextView>(R.id.sivaRole).text = getString(R.string.developer)

        findViewById<ImageView>(R.id.navaImage).setImageResource(R.drawable.nava) // Add hariknath_b.png to drawable
        findViewById<TextView>(R.id.navaName).text = getString(R.string.navaneeth_t)
        findViewById<TextView>(R.id.navaRole).text = getString(R.string.co_lead_and_content_creator)
    }
}