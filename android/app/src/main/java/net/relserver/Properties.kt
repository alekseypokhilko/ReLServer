package net.relserver

import android.content.Context
import android.content.SharedPreferences

class Properties(private var applicationContext: Context) {
    private val PREFS = "PREFS"
    private val DEFAULT_VALUE = ""

    fun initProperties() {
        val settings = getPreferences()
    }

    fun getProperty(key: String): String? {
        val settings = getPreferences()
        return settings.getString(key, DEFAULT_VALUE)
    }

    fun saveProperty(key: String, value: String) {
        val settings = getPreferences()
        val editor = settings.edit()
        editor.putString(key, value)
        editor.apply()
    }

    private fun getPreferences(): SharedPreferences =
        applicationContext.getSharedPreferences(PREFS, 0)
}