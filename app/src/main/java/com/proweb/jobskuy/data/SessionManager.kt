package com.proweb.jobskuy.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("JobsKuyPrefs", Context.MODE_PRIVATE)

    fun createLoginSession(uid: String, role: String) {
        prefs.edit().putString("KEY_UID", uid).putString("KEY_ROLE", role).apply()
    }

    fun getRole(): String? = prefs.getString("KEY_ROLE", null)
    fun getUid(): String? = prefs.getString("KEY_UID", null)

    fun logout() {
        prefs.edit().clear().apply()
    }
}