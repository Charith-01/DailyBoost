package com.example.dailyboost

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

class MoodFragment : Fragment() {

    companion object {
        const val ARG_PRESELECT_EMOJI = "preselect_emoji"
    }

    private lateinit var chipGroup: ChipGroup
    private lateinit var inputNote: TextInputEditText
    private lateinit var btnSave: Button

    private val emojis = listOf(
        "🤩","😊","🙂","😐","😞","😭",
        "😤","😡","😴","😌","🥳","🤯",
        "❤️","✨","🙏","💪","🧘","📚","🎶","☕"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_mood_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Keep status bar purple with light icons
        requireActivity().window.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.primary)
        WindowCompat.getInsetsController(requireActivity().window, view)
            ?.isAppearanceLightStatusBars = false

        // ✅ Apply TOP status-bar inset to the app bar so the 110dp toolbar sits below the cutout
        val appBar = view.findViewById<View>(R.id.appBar)
        ViewCompat.setOnApplyWindowInsetsListener(appBar) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, sb.top, v.paddingRight, v.paddingBottom)
            insets
        }

        // Only handle bottom inset so controls clear the gesture/nav bar
        val content = view.findViewById<View>(R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
            val nb = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, nb.bottom)
            insets
        }

        chipGroup = view.findViewById(R.id.chipGroupEmojis)
        inputNote = view.findViewById(R.id.inputNote)
        btnSave = view.findViewById(R.id.btnSave)

        buildEmojiChips()

        arguments?.getString(ARG_PRESELECT_EMOJI)?.let { pre ->
            if (pre.isNotBlank()) selectEmoji(pre)
        }

        btnSave.isEnabled = selectedEmoji() != null
        chipGroup.setOnCheckedStateChangeListener { _, _ ->
            btnSave.isEnabled = selectedEmoji() != null
        }

        btnSave.setOnClickListener {
            val selected = selectedEmoji() ?: run {
                btnSave.isEnabled = false
                return@setOnClickListener
            }
            val note = inputNote.text?.toString()?.trim().takeUnless { it.isNullOrEmpty() }
            MoodStore.add(requireContext(), selected, note)
            // stay on Mood tab
        }
    }

    private fun buildEmojiChips() {
        chipGroup.removeAllViews()
        emojis.forEach { e ->
            val chip = layoutInflater.inflate(
                R.layout.item_mood_chip_choice,
                chipGroup,
                false
            ) as Chip
            chip.text = e
            chip.isCheckable = true
            chip.isChecked = false
            chipGroup.addView(chip)
        }
    }

    private fun selectedEmoji(): String? {
        val ids = chipGroup.checkedChipIds
        if (ids.isEmpty()) return null
        val chip = chipGroup.findViewById<Chip>(ids.first())
        return chip?.text?.toString()
    }

    private fun selectEmoji(emoji: String) {
        for (i in 0 until chipGroup.childCount) {
            val c = chipGroup.getChildAt(i) as? Chip ?: continue
            if (c.text?.toString() == emoji) {
                c.isChecked = true
                return
            }
        }
    }
}
