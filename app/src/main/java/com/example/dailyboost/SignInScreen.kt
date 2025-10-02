package com.example.dailyboost

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SignInScreen : AppCompatActivity() {

    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var btnSignIn: Button
    private lateinit var txtSignUp: TextView
    private lateinit var rememberMe: CheckBox
    private lateinit var forgotPassword: TextView
    private lateinit var iconEye: ImageView

    private var showPwd = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in_screen)

        // Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        bindViews()
        setupPasswordToggle()
        setupClicks()
        prefillEmailIfAvailable()
    }

    private fun bindViews() {
        inputEmail = findViewById(R.id.inputEmail)
        inputPassword = findViewById(R.id.inputPassword)
        btnSignIn = findViewById(R.id.btnSignIn)
        txtSignUp = findViewById(R.id.txtSignUp)
        rememberMe = findViewById(R.id.rememberMe)
        forgotPassword = findViewById(R.id.forgotPassword)
        iconEye = findViewById(R.id.iconEye)
    }

    private fun setupPasswordToggle() {
        iconEye.setOnClickListener {
            showPwd = !showPwd
            val sel = inputPassword.selectionEnd
            inputPassword.inputType = if (showPwd)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            inputPassword.setSelection(sel.coerceAtLeast(0))
        }
    }

    private fun setupClicks() {
        // "Sign Up" link -> NO animation (per your request)
        txtSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpScreen::class.java))
            finish()
        }

        forgotPassword.setOnClickListener {
            Toast.makeText(this, "Feature not available in exam scope.", Toast.LENGTH_SHORT).show()
        }

        btnSignIn.setOnClickListener { doLogin() }
    }

    private fun prefillEmailIfAvailable() {
        // Prefill with active user if any (so switching back is easier)
        val existing = AuthPrefs.getUser(this)
        if (existing != null) inputEmail.setText(existing.email)
    }

    private fun doLogin() {
        val email = inputEmail.text.toString().trim().lowercase()
        val pass = inputPassword.text.toString()

        if (email.isEmpty() || !AuthPrefs.isValidEmail(email)) {
            inputEmail.error = getString(R.string.error_invalid_email)
            inputEmail.requestFocus()
            return
        }
        if (pass.isEmpty()) {
            inputPassword.error = getString(R.string.error_password_short) // reuse msg
            inputPassword.requestFocus()
            return
        }

        // Multi-user: fetch the specific account by email
        val user = AuthPrefs.getUserByEmail(this, email)
        if (user == null) {
            Toast.makeText(this, "No account for this email. Please sign up.", Toast.LENGTH_SHORT).show()
            return
        }

        val ok = (AuthPrefs.sha256(pass) == user.passwordHash)
        if (!ok) {
            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
            return
        }

        // Login success → switch active user + remember flag
        AuthPrefs.switchUser(this, email)
        AuthPrefs.setLoggedIn(this, rememberMe.isChecked)

        Toast.makeText(this, "Welcome back, ${user.fullName}!", Toast.LENGTH_SHORT).show()

        // Go to MainActivity WITH fade animation (only for Sign In path)
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
