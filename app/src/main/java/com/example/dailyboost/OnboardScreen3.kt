package com.example.dailyboost

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class OnboardScreen3 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_onboard_screen3)

        // 1) Root: top + sides only (NO bottom padding → prevents purple strip)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, 0)
            insets
        }

        // 2) Bottom sheet: take the bottom inset so the area behind the gesture bar is WHITE
        val bottomSheet = findViewById<android.view.View>(R.id.bottomSheet)
        ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, sb.bottom)
            insets
        }

        // --- Navigation: Previous button goes back to Screen 2 ---
        val btnPrev = findViewById<ImageView>(R.id.btnSkip)
        btnPrev.setOnClickListener {
            startActivity(Intent(this, OnboardScreen2::class.java))
            finish()
        }

        // --- Navigation: Get Started button goes to SignInScreen ---
        val btnNext = findViewById<ImageView>(R.id.btnNext)
        btnNext.setOnClickListener {
            startActivity(Intent(this, SignInScreen::class.java))
            finish()
        }
    }
}
