package com.example.tdexv01

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MaruthamalaiInfoActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maruthamalai_info)
        supportActionBar?.hide()

        val distance = intent.getDoubleExtra("distance", -1.0)
        val templeImage = findViewById<ImageView>(R.id.maruthamalaiImage)
        val templeDescription = findViewById<TextView>(R.id.maruthamalaiDescription)
        val distanceText = findViewById<TextView>(R.id.maruthamalaiDistance)

        // Set temple content
        templeImage.setImageResource(R.drawable.maruthamalai_1)
        templeDescription.text = getString(R.string.maruthamalai_pambatti_siddhar_temple_description)
        distanceText.text = getString(R.string.distance_from_user, String.format("%.2f", distance))
    }
}