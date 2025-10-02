package com.example.dailyboost

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class OnboardScreen1 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_onboard_screen1)

        // Fix system bars insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Apply bottom inset only to the white bottom sheet
        val bottomSheet = findViewById<android.view.View>(R.id.bottomSheet)
        ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom)
            insets
        }

        // Next → OnboardScreen2 (with fade transition)
        val nextBtn = findViewById<ImageView>(R.id.btnNext)
        nextBtn.setOnClickListener {
            val intent = Intent(this, OnboardScreen2::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish() // optional: prevent back to this screen
        }

        // Skip → SignInScreen (with fade transition)
        val skipBtn = findViewById<ImageView>(R.id.btnSkip)
        skipBtn.setOnClickListener {
            val intent = Intent(this, SignInScreen::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish() // close onboarding step 1
        }
    }
}
