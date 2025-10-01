package com.example.dailyboost

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
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
        // status bar style to match app bar
        requireActivity().window.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.primary)
        WindowCompat.getInsetsController(requireActivity().window, view)
            ?.isAppearanceLightStatusBars = false

        // apply top inset to app bar
        view.findViewById<View>(R.id.appBar)?.let { appBar ->
            ViewCompat.setOnApplyWindowInsetsListener(appBar) { v, insets ->
                val sb = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                v.setPadding(v.paddingLeft, sb.top, v.paddingRight, v.paddingBottom)
                insets
            }
        }
        // bottom inset for content area (if present in layout)
        view.findViewById<View>(R.id.content)?.let { content ->
            ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
                val nb = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, nb.bottom)
                insets
            }
        }

        chipGroup = view.findViewById(R.id.chipGroupEmojis)
        inputNote = view.findViewById(R.id.inputNote)
        btnSave = view.findViewById(R.id.btnSave)

        buildEmojiChips()

        // Preselect if provided
        arguments?.getString(ARG_PRESELECT_EMOJI)?.let { pre ->
            if (!pre.isNullOrBlank()) selectEmoji(pre)
        }

        // initial state for the button
        btnSave.isEnabled = selectedEmoji() != null

        chipGroup.setOnCheckedStateChangeListener { _, _ ->
            btnSave.isEnabled = selectedEmoji() != null
        }

        btnSave.setOnClickListener {
            val emoji = selectedEmoji()
            if (emoji == null) {
                btnSave.isEnabled = false
                return@setOnClickListener
            }
            val note = inputNote.text?.toString()?.trim().takeUnless { it.isNullOrEmpty() }

            // If your MoodStore.add doesn't accept note, use the 2-arg version:
            // MoodStore.add(requireContext(), emoji)
            MoodStore.add(requireContext(), emoji, note)

            Toast.makeText(requireContext(), "Saved $emoji", Toast.LENGTH_SHORT).show()

            // reset UI (stay on Mood tab)
            chipGroup.clearCheck()
            inputNote.setText("")
            btnSave.isEnabled = false
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
            chip.id = View.generateViewId()        // 🔑 unique ID so ChipGroup can track selection
            chip.text = e
            chip.isCheckable = true
            chip.isChecked = false
            chipGroup.addView(chip)
        }
    }

    private fun selectedEmoji(): String? {
        val id = chipGroup.checkedChipId
        if (id == View.NO_ID) return null
        return chipGroup.findViewById<Chip>(id)?.text?.toString()
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
