package com.example.dailyboost

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class AddEditHabitActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_HABIT_ID = "extra_habit_id"
    }

    private var editingHabit: Habit? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_habit)

        val titleBar = findViewById<TextView>(R.id.titleBar)
        val inputTitle = findViewById<EditText>(R.id.inputTitle)
        val groupType = findViewById<RadioGroup>(R.id.groupType)
        val rbCount = findViewById<RadioButton>(R.id.rbCount)
        val rbYesNo = findViewById<RadioButton>(R.id.rbYesNo)
        val goalContainer = findViewById<LinearLayout>(R.id.goalContainer)
        val inputGoal = findViewById<EditText>(R.id.inputGoal)
        val switchActive = findViewById<SwitchMaterial>(R.id.switchActive)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnDelete = findViewById<TextView>(R.id.btnDelete)

        // Show/Hide goal field based on type
        groupType.setOnCheckedChangeListener { _, checkedId ->
            goalContainer.visibility = if (checkedId == R.id.rbCount) View.VISIBLE else View.GONE
        }

        // Edit mode?
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID)
        if (!habitId.isNullOrEmpty()) {
            titleBar.text = getString(R.string.edit_habit)
            editingHabit = HabitStore.loadHabits(this).find { it.id == habitId }
            editingHabit?.let { h ->
                inputTitle.setText(h.title)
                if (h.type == HabitType.COUNT) {
                    rbCount.isChecked = true
                    goalContainer.visibility = View.VISIBLE
                    inputGoal.setText(h.goalPerDay.toString())
                } else {
                    rbYesNo.isChecked = true
                    goalContainer.visibility = View.GONE
                }
                switchActive.isChecked = h.isActive
                btnDelete.visibility = View.VISIBLE
            }
        }

        btnSave.setOnClickListener {
            val title = inputTitle.text.toString().trim()
            if (title.isEmpty()) {
                inputTitle.error = getString(R.string.title_required)
                return@setOnClickListener
            }

            val type = if (rbCount.isChecked) HabitType.COUNT else HabitType.YES_NO
            val goal = if (type == HabitType.COUNT)
                inputGoal.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1
            else 1
            val active = switchActive.isChecked

            val existing = editingHabit
            if (existing == null) {
                HabitStore.addHabit(this, Habit(title = title, type = type, goalPerDay = goal, isActive = active))
            } else {
                existing.title = title
                existing.type = type
                existing.goalPerDay = goal
                existing.isActive = active
                // keep today's progress unless goal shrinks below it
                if (existing.progressToday > goal) existing.progressToday = goal
                HabitStore.updateHabit(this, existing)
            }
            finish()
        }

        btnDelete.setOnClickListener {
            editingHabit?.let { HabitStore.deleteHabit(this, it.id) }
            finish()
        }
    }
}
