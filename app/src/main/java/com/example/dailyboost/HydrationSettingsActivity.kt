package com.example.dailyboost

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.inputmethod.EditorInfo
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

    private lateinit var inputLayoutMinutes: TextInputLayout
    private lateinit var inputMinutes: TextInputEditText
    private lateinit var sliderMinutes: Slider
    private lateinit var pillEvery: TextView
    private lateinit var summaryNextAt: TextView
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton

    private var minutes: Int = 120
    private var suppressTextWatcher = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hydration_settings)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)

        // Push toolbar content below status bar / cutouts (no back button)
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, sb.top, v.paddingRight, v.paddingBottom)
            insets
        }

        // Views
        inputLayoutMinutes = findViewById(R.id.inputLayoutMinutes)
        inputMinutes = findViewById(R.id.inputMinutes)
        sliderMinutes = findViewById(R.id.sliderMinutes)
        pillEvery = findViewById(R.id.pillEvery)
        summaryNextAt = findViewById(R.id.summaryNextAt)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        // Load saved value (1..1440)
        minutes = max(1, HydrationPrefs.intervalMinutes(this))
        minutes = min(minutes, 24 * 60)

        // Init UI
        sliderMinutes.value = minutes.coerceIn(15, 240).toFloat()
        setInputMinutesSafe(minutes)
        validateAndRender()

        // Slider → input
        sliderMinutes.addOnChangeListener { _, value, _ ->
            val v = value.toInt()
            if (minutes != v) {
                minutes = v
                setInputMinutesSafe(v)
                validateAndRender()
            }
        }

        // Input → slider
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
        inputMinutes.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) inputMinutes.clearFocus()
            false
        }

        // Buttons
        btnCancel.setOnClickListener { finish() }
        btnSave.setOnClickListener {
            if (isValid(minutes)) {
                HydrationPrefs.setIntervalMinutes(this, minutes)
                if (HydrationPrefs.isEnabled(this)) {
                    HydrationScheduler.updateInterval(this, minutes)
                }
                finish()
            } else {
                setInvalid("Minutes must be 1–1440")
            }
        }
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
        val txt = if (minutes % 60 == 0) {
            val h = minutes / 60
            "Every $h hour" + if (h != 1) "s" else ""
        } else {
            "Every ${minutes} min"
        }
        pillEvery.text = txt

        val cal = Calendar.getInstance().apply { add(Calendar.MINUTE, minutes) }
        val timeText = DateFormat.getTimeFormat(this).format(cal.time)
        summaryNextAt.text = "Next at $timeText"
    }
}
