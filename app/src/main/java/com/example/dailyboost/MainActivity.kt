package com.example.dailyboost

import android.graphics.Color
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val homeFragment: Fragment by lazy { HomeFragment() }
    private val habitsFragment: Fragment by lazy { HabitsFragment() }
    private val hydrationFragment: Fragment by lazy { HydrationFragment() }
    private val moodFragment: Fragment by lazy { MoodFragment() }
    private val profileFragment: Fragment by lazy { ProfileFragment() }

    private var currentTabId: Int = R.id.tab_home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        window.statusBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

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

    fun switchToTab(itemId: Int) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        currentTabId = itemId
        bottomNav.selectedItemId = itemId
        switchTo(itemId)
    }

    private fun switchTo(itemId: Int, initial: Boolean = false) {
        val fragment: Fragment = when (itemId) {
            R.id.tab_habits -> habitsFragment
            R.id.tab_hydration -> hydrationFragment
            R.id.tab_mood -> moodFragment
            R.id.tab_profile -> profileFragment
            else -> homeFragment
        }
        val tag = when (itemId) {
            R.id.tab_habits -> "tab_habits"
            R.id.tab_hydration -> "tab_hydration"
            R.id.tab_mood -> "tab_mood"
            R.id.tab_profile -> "tab_profile"
            else -> "tab_home"
        }

        val tx = supportFragmentManager.beginTransaction()
        tx.replace(R.id.nav_host_container, fragment, tag)
        supportFragmentManager.popBackStack()
        tx.commitAllowingStateLoss()
    }
}
