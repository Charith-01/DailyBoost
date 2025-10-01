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

    private lateinit var txtName: TextView
    private lateinit var txtEmail: TextView
    private lateinit var valueStreak: TextView
    private lateinit var valueToday: TextView
    private lateinit var valueMood: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_profile_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Status bar: primary bg + light icons
        requireActivity().window.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.primary)
        WindowCompat.getInsetsController(requireActivity().window, view)
            ?.isAppearanceLightStatusBars = false

        // Insets: AppBar top padding
        val appBar = view.findViewById<View>(R.id.appBar)
        ViewCompat.setOnApplyWindowInsetsListener(appBar) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, sb.top, v.paddingRight, v.paddingBottom)
            insets
        }

        // Insets: bottom nav / gesture bar padding
        val scroll = view.findViewById<View>(R.id.scrollProfile)
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, insets ->
            val nb = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, nb.bottom)
            insets
        }

        // Header fields
        txtName  = view.findViewById(R.id.txtName)
        txtEmail = view.findViewById(R.id.txtEmail)

        // Stats views
        valueStreak = view.findViewById(R.id.valueStreak)
        valueToday  = view.findViewById(R.id.valueToday)
        valueMood   = view.findViewById(R.id.valueMood)

        // Populate header with real user data
        bindUserHeader()

        // Populate stats
        bindStats()

        // Actions
        view.findViewById<MaterialButton>(R.id.btnHydration).setOnClickListener {
            startActivity(Intent(requireContext(), HydrationSettingsActivity::class.java))
        }

        view.findViewById<MaterialButton>(R.id.btnShare).setOnClickListener {
            shareWeeklySummary()
        }

        // Logout: clear login state and route to SignIn
        view.findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            // Clear only the "logged in" flag to respect exam scope.
            // If you want a full sign-out (remove stored user), call AuthPrefs.clear(requireContext()).
            AuthPrefs.setLoggedIn(requireContext(), false)

            // Navigate to SignIn and clear back stack
            val intent = Intent(requireContext(), SignInScreen::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }

        // (Optional) Edit Profile — if you later add a profile edit screen
        view.findViewById<MaterialButton>(R.id.btnEditProfile).setOnClickListener {
            // startActivity(Intent(requireContext(), EditProfileActivity::class.java))
            // For now, no-op.
        }
    }

    override fun onResume() {
        super.onResume()
        // If user changed in another screen, refresh name/email & stats
        bindUserHeader()
        bindStats()
    }

    private fun bindUserHeader() {
        val user = AuthPrefs.getUser(requireContext())
        val displayName = user?.fullName?.takeIf { it.isNotBlank() } ?: "User"
        val displayEmail = user?.email?.takeIf { it.isNotBlank() } ?: "Not signed in"

        txtName.text = displayName
        txtEmail.text = displayEmail
    }

    private fun bindStats() {
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
    }

    private fun shareWeeklySummary() {
        val ctx = requireContext()

        val streak = try { HabitStore.getStreak(ctx) } catch (_: Exception) { 0 }
        val (done, total) = try { HabitStore.todayCounts(ctx) } catch (_: Exception) { 0 to 0 }
        val percent = try { HabitStore.computeTodayPercent(ctx) } catch (_: Exception) { 0 }
        val avg = try { MoodStore.averageLast7(ctx) } catch (_: Exception) { null }

        val moodText = if (avg == null) "No mood data yet"
        else "Avg mood: ${MoodStore.emojiForScore(avg)} (${String.format("%.1f/5", avg)})"

        val user = AuthPrefs.getUser(ctx)
        val name = user?.fullName?.takeIf { it.isNotBlank() } ?: "User"

        val shareText = buildString {
            appendLine("🧑 Profile • DailyBoost")
            appendLine("👤 Name: $name")
            user?.email?.let { appendLine("✉️ Email: $it") }
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
}
