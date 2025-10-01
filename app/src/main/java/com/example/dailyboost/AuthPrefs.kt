package com.example.dailyboost

import android.content.Context
import android.util.Patterns
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

data class User(val fullName: String, val email: String, val passwordHash: String)

object AuthPrefs {
    private const val FILE = "auth_prefs"
    private const val KEY_USERS = "users_json"       // JSONArray of user objects
    private const val KEY_ACTIVE_EMAIL = "active_email"
    private const val KEY_LOGGED_IN = "is_logged_in"

    // ---------- Helpers ----------
    fun isValidEmail(email: String) = Patterns.EMAIL_ADDRESS.matcher(email).matches()
    fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun sp(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun loadUsers(ctx: Context): JSONArray {
        val s = sp(ctx).getString(KEY_USERS, "[]") ?: "[]"
        return try { JSONArray(s) } catch (_: Exception) { JSONArray() }
    }

    private fun saveUsers(ctx: Context, arr: JSONArray) {
        sp(ctx).edit().putString(KEY_USERS, arr.toString()).apply()
    }

    private fun userToJson(u: User) = JSONObject().apply {
        put("fullName", u.fullName)
        put("email", u.email.lowercase())
        put("passwordHash", u.passwordHash)
    }

    private fun jsonToUser(o: JSONObject) = User(
        fullName = o.optString("fullName"),
        email = o.optString("email"),
        passwordHash = o.optString("passwordHash")
    )

    private fun findIndexByEmail(arr: JSONArray, email: String): Int {
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optString("email").equals(email, ignoreCase = true)) return i
        }
        return -1
    }

    // ---------- Public API ----------
    fun register(ctx: Context, user: User) {
        val arr = loadUsers(ctx)
        val idx = findIndexByEmail(arr, user.email)
        if (idx >= 0) {
            // Update existing user (keeps other accounts)
            arr.put(idx, userToJson(user))
        } else {
            arr.put(userToJson(user))
        }
        saveUsers(ctx, arr)
        setActiveEmail(ctx, user.email)
        setLoggedIn(ctx, true)
    }

    fun isRegistered(ctx: Context): Boolean = loadUsers(ctx).length() > 0

    fun getUser(ctx: Context): User? {
        val active = getActiveEmail(ctx) ?: return null
        return getUserByEmail(ctx, active)
    }

    fun getUserByEmail(ctx: Context, email: String): User? {
        val arr = loadUsers(ctx)
        val idx = findIndexByEmail(arr, email)
        return if (idx >= 0) jsonToUser(arr.getJSONObject(idx)) else null
    }

    fun listUsers(ctx: Context): List<User> {
        val arr = loadUsers(ctx)
        return buildList {
            for (i in 0 until arr.length()) add(jsonToUser(arr.getJSONObject(i)))
        }
    }

    fun removeUser(ctx: Context, email: String) {
        val arr = loadUsers(ctx)
        val idx = findIndexByEmail(arr, email)
        if (idx >= 0) {
            // rebuild without idx (JSONArray has no remove on older APIs)
            val out = JSONArray()
            for (i in 0 until arr.length()) if (i != idx) out.put(arr.getJSONObject(i))
            saveUsers(ctx, out)
            // fix active user if needed
            if (getActiveEmail(ctx)?.equals(email, true) == true) {
                val first = if (out.length() > 0) out.getJSONObject(0).optString("email") else null
                setActiveEmail(ctx, first)
            }
        }
    }

    fun switchUser(ctx: Context, email: String): Boolean {
        val exists = getUserByEmail(ctx, email) != null
        if (exists) {
            setActiveEmail(ctx, email)
            setLoggedIn(ctx, true)
        }
        return exists
    }

    fun clearAll(ctx: Context) {
        sp(ctx).edit().clear().apply()
    }

    fun setLoggedIn(ctx: Context, value: Boolean) {
        sp(ctx).edit().putBoolean(KEY_LOGGED_IN, value).apply()
    }

    fun isLoggedIn(ctx: Context): Boolean =
        sp(ctx).getBoolean(KEY_LOGGED_IN, false)

    private fun setActiveEmail(ctx: Context, email: String?) {
        sp(ctx).edit().putString(KEY_ACTIVE_EMAIL, email).apply()
    }

    private fun getActiveEmail(ctx: Context): String? =
        sp(ctx).getString(KEY_ACTIVE_EMAIL, null)
}
