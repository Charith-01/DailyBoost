package com.example.dailyboost

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

class HydrationSettingsActivity : AppCompatActivity() {

    // --- Views ---
    private lateinit var inputLayoutMinutes: TextInputLayout
    private lateinit var inputMinutes: TextInputEditText
    private lateinit var sliderMinutes: Slider
    private lateinit var pillEvery: TextView
    private lateinit var summaryNextAt: TextView
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton

    private lateinit var inputGoal: EditText
    private lateinit var inputDrinkAmount: EditText
    private lateinit var txtSummaryGoal: TextView
    private lateinit var txtSummaryDrink: TextView

    // --- Variables ---
    private var minutes: Int = 120
    private var goal: Int = 2500
    private var drinkAmount: Int = 300
    private var suppressTextWatcher = false

    private lateinit var data: HydrationData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hydration_settings)

        data = HydrationData(this)
        data.ensureNewDayReset()

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)

        // Handle status bar padding
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, sb.top, v.paddingRight, v.paddingBottom)
            insets
        }

        // --- Bind Views ---
        inputLayoutMinutes = findViewById(R.id.inputLayoutMinutes)
        inputMinutes = findViewById(R.id.inputMinutes)
        sliderMinutes = findViewById(R.id.sliderMinutes)
        pillEvery = findViewById(R.id.pillEvery)
        summaryNextAt = findViewById(R.id.summaryNextAt)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        inputGoal = findViewById(R.id.inputGoal)
        inputDrinkAmount = findViewById(R.id.inputDrinkAmount)
        txtSummaryGoal = findViewById(R.id.txtSummaryGoal)
        txtSummaryDrink = findViewById(R.id.txtSummaryDrink)

        // ⏱ Test reminder button (clock on reminder card header)
        findViewById<ImageView>(R.id.imgClockDecor)?.setOnClickListener {
            // Schedule a quick test notification (30 seconds from now)
            HydrationTest.scheduleTestReminder(this, 1)
        }

        // --- Load saved data ---
        minutes = min(max(1, data.reminderInterval), 24 * 60)
        goal = data.goal
        drinkAmount = data.oneDrinkAmount

        // --- Initialize UI ---
        sliderMinutes.value = minutes.coerceIn(15, 240).toFloat()
        setInputMinutesSafe(minutes)
        inputGoal.setText(goal.toString())
        inputDrinkAmount.setText(drinkAmount.toString())
        validateAndRender()

        // --- Slider → Input sync ---
        sliderMinutes.addOnChangeListener { _, value, _ ->
            val v = value.toInt()
            if (minutes != v) {
                minutes = v
                setInputMinutesSafe(v)
                validateAndRender()
            }
        }

        // --- Input → Slider sync ---
        inputMinutes.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (suppressTextWatcher) return
                val t = s?.toString()?.trim()
                if (t.isNullOrEmpty()) {
                    setInvalid("Enter minutes (1–1440)")
                    return
                }
                val num = t.toIntOrNull()
                if (num == null) {
                    setInvalid("Numbers only (1–1440)")
                    return
                }
                minutes = num.coerceIn(1, 24 * 60)
                val sliderTarget = minutes.coerceIn(15, 240).toFloat()
                if (sliderMinutes.value != sliderTarget) sliderMinutes.value = sliderTarget
                validateAndRender()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Buttons
        btnCancel.setOnClickListener { finish() }
        btnSave.setOnClickListener {
            if (isValid(minutes)) {
                val g = inputGoal.text.toString().toIntOrNull()?.coerceIn(500, 6000) ?: 2500
                val d = inputDrinkAmount.text.toString().toIntOrNull()?.coerceIn(50, 1000) ?: 300

                data.goal = g
                data.oneDrinkAmount = d
                data.reminderInterval = minutes

                if (HydrationPrefs.isEnabled(this)) {
                    HydrationScheduler.updateInterval(this, minutes)
                }
                finish()
            } else {
                setInvalid("Minutes must be 1–1440")
            }
        }

        // Update summaries as inputs change
        addSimpleWatcher(inputGoal) { validateAndRender() }
        addSimpleWatcher(inputDrinkAmount) { validateAndRender() }
    }

    private fun addSimpleWatcher(edit: EditText, after: () -> Unit) {
        edit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = after()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setInputMinutesSafe(v: Int) {
        suppressTextWatcher = true
        inputMinutes.setText(v.toString())
        inputMinutes.setSelection(inputMinutes.text?.length ?: 0)
        suppressTextWatcher = false
    }

    private fun validateAndRender() {
        if (isValid(minutes)) {
            inputLayoutMinutes.error = null
            btnSave.isEnabled = true
            renderSummary()
        } else {
            setInvalid("Minutes must be 1–1440")
        }
    }

    private fun isValid(v: Int): Boolean = v in 1..1440

    private fun setInvalid(msg: String) {
        inputLayoutMinutes.error = msg
        btnSave.isEnabled = false
        renderSummary()
    }

    private fun renderSummary() {
        // Interval summary
        val txt = if (minutes % 60 == 0) {
            val h = minutes / 60
            "Every $h hour" + if (h != 1) "s" else ""
        } else {
            "Every ${minutes} min"
        }
        pillEvery.text = txt

        // Next time summary
        val cal = Calendar.getInstance().apply { add(Calendar.MINUTE, minutes) }
        val timeText = DateFormat.getTimeFormat(this).format(cal.time)
        summaryNextAt.text = "Next at $timeText"

        // Goal & drink summaries
        val g = inputGoal.text.toString().toIntOrNull() ?: goal
        val d = inputDrinkAmount.text.toString().toIntOrNull() ?: drinkAmount
        txtSummaryGoal.text = "Goal: $g mL"
        txtSummaryDrink.text = "Each Drink: $d mL"
    }
}
