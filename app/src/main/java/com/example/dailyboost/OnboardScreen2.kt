package com.example.dailyboost

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class OnboardScreen2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_onboard_screen2)

        // Root insets (avoid purple strip at bottom)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, 0)
            insets
        }

        // Bottom sheet insets (make behind gesture bar white)
        val bottomSheet = findViewById<android.view.View>(R.id.bottomSheet)
        ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, sb.bottom)
            insets
        }

        // --- Navigation ---
        val btnPrev = findViewById<ImageView>(R.id.btnSkip)   // your "previous" image (prebtn)
        val btnNext = findViewById<ImageView>(R.id.btnNext)   // your "next" image (nextbtn)

        btnPrev.setOnClickListener {
            startActivity(Intent(this, OnboardScreen1::class.java))
            finish() // optional: remove screen2 from back stack
        }

        btnNext.setOnClickListener {
            startActivity(Intent(this, OnboardScreen3::class.java))
            // don't finish() if you want back press to return to screen2
        }
    }
}
