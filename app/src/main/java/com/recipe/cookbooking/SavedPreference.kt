package com.recipe.cookbooking

import android.content.Context
import android.content.SharedPreferences

object SavedPreference {
    private const val PREF_NAME = "user_pref"
    private const val KEY_EMAIL = "email"
    private const val KEY_USERNAME = "username"
    private const val KEY_PROFILE_PIC = "profile_pic"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setEmail(context: Context, email: String) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(KEY_EMAIL, email)
        editor.apply()
    }

    fun setUsername(context: Context, username: String) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(KEY_USERNAME, username)
        editor.apply()
    }

    fun setProfilePic(context: Context, profilePic: String) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(KEY_PROFILE_PIC, profilePic)
        editor.apply()
    }

    fun getEmail(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_EMAIL, null)
    }

    fun getUsername(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_USERNAME, null)
    }

    fun getProfilePic(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_PROFILE_PIC, null)
    }

    fun clearUserData(context: Context) {
        getSharedPreferences(context).edit().apply {
            remove(KEY_EMAIL)
            remove(KEY_USERNAME)
            remove(KEY_PROFILE_PIC)
            apply()
        }
    }
}