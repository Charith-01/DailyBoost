package com.example.dailyboost

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val homeFragment: Fragment by lazy { HomeFragment() }
    private val habitsFragment: Fragment by lazy { HabitsFragment() }
    private val moodFragment: Fragment by lazy { MoodFragment() }
    private val profileFragment: Fragment by lazy { ProfileFragment() }

    private var currentTabId: Int = R.id.tab_home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // ---- Edge-to-edge: transparent status bar over content (purple header will fill behind) ----
        window.statusBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        val root = findViewById<View>(R.id.mainRoot)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        val container = findViewById<View>(R.id.nav_host_container)

        // Only handle bottom nav bar inset for the fragment container; top is handled by fragments' own layouts
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val nb = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            container.setPadding(
                container.paddingLeft,
                container.paddingTop,
                container.paddingRight,
                nb.bottom
            )
            insets
        }

        currentTabId = savedInstanceState?.getInt("selected_tab") ?: R.id.tab_home
        switchTo(currentTabId, initial = true)
        bottomNav.selectedItemId = currentTabId

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId != currentTabId) {
                currentTabId = item.itemId
                switchTo(item.itemId)
            }
            true
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (currentTabId != R.id.tab_home) {
                        switchToTab(R.id.tab_home)
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("selected_tab", currentTabId)
        super.onSaveInstanceState(outState)
    }

    /** Allow fragments to request tab switches (e.g., Home -> Mood). */
    fun switchToTab(itemId: Int) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        currentTabId = itemId
        bottomNav.selectedItemId = itemId
        switchTo(itemId)
    }

    private fun switchTo(itemId: Int, initial: Boolean = false) {
        val fragment: Fragment = when (itemId) {
            R.id.tab_habits -> habitsFragment
            R.id.tab_mood -> moodFragment
            R.id.tab_profile -> profileFragment
            else -> homeFragment
        }
        val tag = when (itemId) {
            R.id.tab_habits -> "tab_habits"
            R.id.tab_mood -> "tab_mood"
            R.id.tab_profile -> "tab_profile"
            else -> "tab_home"
        }

        val tx = supportFragmentManager.beginTransaction()
        // tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        tx.replace(R.id.nav_host_container, fragment, tag)
        supportFragmentManager.popBackStack()
        tx.commitAllowingStateLoss()
    }
}
