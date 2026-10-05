package com.pupil.app.core.prefs

import android.content.Context
import android.content.SharedPreferences

class OnboardingPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pupil_onboarding_prefs", Context.MODE_PRIVATE)

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_COMPLETED, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Student") ?: "Student"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "student@bujji.app") ?: "student@bujji.app"
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var userPhotoUri: String?
        get() = prefs.getString(KEY_USER_PHOTO, null)
        set(value) = prefs.edit().putString(KEY_USER_PHOTO, value).apply()

    var isAuthenticated: Boolean
        get() = prefs.getBoolean(KEY_AUTHENTICATED, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTHENTICATED, value).apply()

    companion object {
        private const val KEY_COMPLETED = "has_completed_onboarding"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHOTO = "user_photo"
        private const val KEY_AUTHENTICATED = "is_authenticated"
    }
}
