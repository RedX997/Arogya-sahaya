package com.example.arogyasahaya.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val authPrefs: SharedPreferences = context.getSharedPreferences("arogyasahaya_prefs", Context.MODE_PRIVATE)
    private val settingsPrefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_AGE = "user_age"
        private const val KEY_USER_GENDER = "user_gender"
        private const val KEY_USER_PHOTO = "user_photo_uri"
        private const val KEY_IS_GUEST = "is_guest"
        
        // Settings keys
        private const val KEY_CHRONIC = "chronic_conditions"
        private const val KEY_BLOOD_GROUP = "blood_group"
        private const val KEY_EMERGENCY_NAME = "emergency_name"
        private const val KEY_EMERGENCY_PHONE = "emergency_phone"
    }

    fun saveAuthData(token: String, userId: String, name: String, email: String, isGuest: Boolean = false) {
        authPrefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            putBoolean(KEY_IS_GUEST, isGuest)
            apply()
        }
    }

    fun saveProfileData(name: String, age: String, gender: String, photoUri: String, chronic: String? = null, blood: String? = null, emName: String? = null, emPhone: String? = null) {
        authPrefs.edit().apply {
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_AGE, age)
            putString(KEY_USER_GENDER, gender)
            putString(KEY_USER_PHOTO, photoUri)
            apply()
        }
        settingsPrefs.edit().apply {
            putString("profile_name", name)
            putString("profile_age", age)
            putString("profile_gender", gender)
            putString("profile_photo_uri", photoUri)
            if (chronic != null) putString(KEY_CHRONIC, chronic)
            if (blood != null) putString(KEY_BLOOD_GROUP, blood)
            if (emName != null) putString(KEY_EMERGENCY_NAME, emName)
            if (emPhone != null) putString(KEY_EMERGENCY_PHONE, emPhone)
            apply()
        }
    }

    fun getToken(): String? = authPrefs.getString(KEY_TOKEN, null)
    fun getUserId(): String? = authPrefs.getString(KEY_USER_ID, null)
    fun getUserName(): String = authPrefs.getString(KEY_USER_NAME, "New User") ?: "New User"
    fun getUserEmail(): String = authPrefs.getString(KEY_USER_EMAIL, "Not Specified") ?: "Not Specified"
    fun getUserAge(): String = authPrefs.getString(KEY_USER_AGE, "25") ?: "25"
    fun getUserGender(): String = authPrefs.getString(KEY_USER_GENDER, "Not Specified") ?: "Not Specified"
    fun getUserPhoto(): String = authPrefs.getString(KEY_USER_PHOTO, "") ?: ""
    fun isGuest(): Boolean = authPrefs.getBoolean(KEY_IS_GUEST, false)
    
    fun getChronicConditions(): String = settingsPrefs.getString(KEY_CHRONIC, "None") ?: "None"
    fun getBloodGroup(): String = settingsPrefs.getString(KEY_BLOOD_GROUP, "—") ?: "—"
    fun getEmergencyName(): String = settingsPrefs.getString(KEY_EMERGENCY_NAME, "Not Set") ?: "Not Set"
    fun getEmergencyPhone(): String = settingsPrefs.getString(KEY_EMERGENCY_PHONE, "Not Set") ?: "Not Set"

    fun isLoggedIn(): Boolean = getToken() != null

    fun clear() {
        authPrefs.edit().clear().apply()
        settingsPrefs.edit().clear().apply()
    }
}
