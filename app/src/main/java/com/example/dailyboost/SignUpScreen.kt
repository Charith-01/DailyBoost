package com.example.dailyboost

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SignUpScreen : AppCompatActivity() {

    private lateinit var inputName: EditText
    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var inputConfirmPassword: EditText
    private lateinit var acceptTerms: CheckBox
    private lateinit var btnSignUp: Button
    private lateinit var iconEye1: ImageView
    private lateinit var iconEye2: ImageView

    private var showPwd1 = false
    private var showPwd2 = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up_screen)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        bindViews()
        setupPasswordToggles()

        // "Sign In" link -> no animation (per your request)
        findViewById<TextView>(R.id.txtSignIn).setOnClickListener {
            startActivity(Intent(this, SignInScreen::class.java))
            finish()
        }

        // Sign Up button -> will use fade animation after successful registration
        btnSignUp.setOnClickListener { doRegister() }
    }

    private fun bindViews() {
        inputName = findViewById(R.id.inputName)
        inputEmail = findViewById(R.id.inputEmail)
        inputPassword = findViewById(R.id.inputPassword)
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword)
        acceptTerms = findViewById(R.id.acceptTerms)
        btnSignUp = findViewById(R.id.btnSignUp)
        iconEye1 = findViewById(R.id.iconEye1)
        iconEye2 = findViewById(R.id.iconEye2)
    }

    private fun setupPasswordToggles() {
        iconEye1.setOnClickListener {
            showPwd1 = !showPwd1
            togglePasswordVisibility(inputPassword, showPwd1)
        }
        iconEye2.setOnClickListener {
            showPwd2 = !showPwd2
            togglePasswordVisibility(inputConfirmPassword, showPwd2)
        }
    }

    private fun togglePasswordVisibility(field: EditText, show: Boolean) {
        val sel = field.selectionEnd
        field.inputType = if (show)
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        else
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        field.setSelection(sel.coerceAtLeast(0))
    }

    private fun doRegister() {
        val name = inputName.text.toString().trim()
        val email = inputEmail.text.toString().trim().lowercase()
        val pass = inputPassword.text.toString()
        val confirm = inputConfirmPassword.text.toString()

        // Validate
        if (name.isEmpty()) {
            inputName.error = getString(R.string.error_full_name_required)
            inputName.requestFocus(); return
        }
        if (email.isEmpty() || !AuthPrefs.isValidEmail(email)) {
            inputEmail.error = getString(R.string.error_invalid_email)
            inputEmail.requestFocus(); return
        }
        if (pass.length < 6) {
            inputPassword.error = getString(R.string.error_password_short)
            inputPassword.requestFocus(); return
        }
        if (pass != confirm) {
            inputConfirmPassword.error = getString(R.string.error_password_mismatch)
            inputConfirmPassword.requestFocus(); return
        }
        if (!acceptTerms.isChecked) {
            Toast.makeText(this, getString(R.string.error_accept_terms), Toast.LENGTH_SHORT).show()
            return
        }

        // Multi-user register/update (keeps previous users)
        val existing = AuthPrefs.getUserByEmail(this, email)
        val hash = AuthPrefs.sha256(pass)
        val user = User(fullName = name, email = email, passwordHash = hash)

        AuthPrefs.register(this, user) // adds/updates, sets active + logged-in

        // UX feedback
        if (existing == null) {
            Toast.makeText(this, getString(R.string.signup_success), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Account updated", Toast.LENGTH_SHORT).show()
        }

        // Continue to Sign In WITH fade animation (only for Sign Up path)
        startActivity(Intent(this, SignInScreen::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
