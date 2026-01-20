package org.example.app

import android.content.Context

/**
 * Simple persistence for onboarding completion state.
 */
internal object OnboardingPrefs {
    private const val PREFS_NAME = "onboarding_prefs"
    private const val KEY_COMPLETED = "completed"

    fun hasCompleted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_COMPLETED, false)
    }

    fun setCompleted(context: Context, completed: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_COMPLETED, completed).apply()
    }
}
