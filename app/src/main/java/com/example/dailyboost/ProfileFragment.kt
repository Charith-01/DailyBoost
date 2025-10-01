package com.example.dailyboost

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_profile_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Keep status bar purple with light icons
        requireActivity().window.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.primary)
        WindowCompat.getInsetsController(requireActivity().window, view)
            ?.isAppearanceLightStatusBars = false

        // Top inset for the AppBar so it sits below status bar/cutout
        val appBar = view.findViewById<View>(R.id.appBar)
        ViewCompat.setOnApplyWindowInsetsListener(appBar) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, sb.top, v.paddingRight, v.paddingBottom)
            insets
        }

        // Bottom inset for the scroll so it clears the gesture/nav bar + bottom nav
        val scroll = view.findViewById<View>(R.id.scrollProfile)
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, insets ->
            val nb = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, nb.bottom)
            insets
        }

        // --- Stats views ---
        val valueStreak = view.findViewById<TextView>(R.id.valueStreak)
        val valueToday  = view.findViewById<TextView>(R.id.valueToday)
        val valueMood   = view.findViewById<TextView>(R.id.valueMood)

        val ctx = requireContext()

        // Streak
        val streak = try { HabitStore.getStreak(ctx) } catch (_: Exception) { 0 }
        valueStreak.text = streak.toString()

        // Today counts (done/total)
        val (done, total) = try { HabitStore.todayCounts(ctx) } catch (_: Exception) { 0 to 0 }
        valueToday.text = "$done/$total"

        // Avg mood last 7 days
        val avg = try { MoodStore.averageLast7(ctx) } catch (_: Exception) { null }
        valueMood.text = if (avg == null) "–" else {
            val face = MoodStore.emojiForScore(avg)
            String.format("%s %.1f", face, avg)
        }

        // --- Actions ---
        view.findViewById<MaterialButton>(R.id.btnHydration).setOnClickListener {
            startActivity(Intent(ctx, HydrationSettingsActivity::class.java))
        }

        view.findViewById<MaterialButton>(R.id.btnShare).setOnClickListener {
            val percent = try { HabitStore.computeTodayPercent(ctx) } catch (_: Exception) { 0 }
            val moodText = if (avg == null) "No mood data yet"
            else "Avg mood: ${MoodStore.emojiForScore(avg)} (${String.format("%.1f/5", avg)})"

            val shareText = buildString {
                appendLine("🧑 Profile • DailyBoost")
                appendLine("🔥 Streak: $streak days")
                appendLine("✅ Today: $done/$total habits (${percent}%)")
                appendLine("😊 $moodText")
                append("Sent via DailyBoost")
            }

            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(send, "Share weekly summary"))
        }

        // --- Logout → navigate to SignInScreen and clear back stack ---
        view.findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            // If you maintain auth/session, clear it here (SharedPreferences, tokens, etc.)
            // Example:
            // getSharedPreferences("auth", Context.MODE_PRIVATE).edit().clear().apply()

            val intent = Intent(requireContext(), SignInScreen::class.java).apply {
                // Clear the whole task so user can't return via back button
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
    }
}
