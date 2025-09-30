package com.example.dailyboost

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

class AddMoodActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PRESELECT_EMOJI = "preselect_emoji"
    }

    private lateinit var chipGroup: ChipGroup
    private lateinit var inputNote: TextInputEditText
    private lateinit var btnSave: Button

    // Keep the set compact; you can expand later
    private val emojis = listOf(
        "🤩","😊","🙂","😐","😞","😭",
        "😤","😡","😴","😌","🥳","🤯",
        "❤️","✨","🙏","💪","🧘","📚","🎶","☕"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_mood)

        // Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Add mood"

        // Insets
        val appBar = findViewById<View>(R.id.appBar)
        ViewCompat.setOnApplyWindowInsetsListener(appBar) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, sb.top, v.paddingRight, v.paddingBottom)
            insets
        }
        val root = findViewById<View>(R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val nb = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, nb.bottom)
            insets
        }

        chipGroup = findViewById(R.id.chipGroupEmojis)
        inputNote = findViewById(R.id.inputNote)
        btnSave = findViewById(R.id.btnSave)

        // Build emoji chips
        buildEmojiChips()

        // Preselect if provided
        intent?.getStringExtra(EXTRA_PRESELECT_EMOJI)?.let { pre ->
            if (pre.isNotBlank()) selectEmoji(pre)
        }

        // Save
        btnSave.setOnClickListener {
            val selected = selectedEmoji() ?: run {
                btnSave.isEnabled = false
                return@setOnClickListener
            }
            val note = inputNote.text?.toString()?.trim().takeUnless { it.isNullOrEmpty() }
            MoodStore.add(this, selected, note)
            setResult(RESULT_OK)
            finish()
        }
        btnSave.isEnabled = selectedEmoji() != null

        chipGroup.setOnCheckedStateChangeListener { _, _ ->
            btnSave.isEnabled = selectedEmoji() != null
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun buildEmojiChips() {
        chipGroup.removeAllViews()
        emojis.forEach { e ->
            val chip = layoutInflater.inflate(
                R.layout.item_mood_chip_choice, chipGroup, false
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
