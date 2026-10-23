package com.kynectia.app.data.api

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.kynectia.app.BuildConfig
import com.kynectia.app.data.model.UserProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("kynectia_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_PROFILE = "user_profile"

        const val HOST_LAN_URL = "http://192.168.0.24:4000/api/v1/"
        const val ADB_REVERSE_URL = "http://localhost:4000/api/v1/"
        const val EMULATOR_URL = "http://10.0.2.2:4000/api/v1/"

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val isEmulator: Boolean
        get() = Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")

    var baseUrl: String
        get() {
            val saved = prefs.getString(KEY_BASE_URL, null)
            return if (!saved.isNullOrBlank()) {
                // Si está en un dispositivo físico y la URL guardada era 10.0.2.2, auto-migrar a la IP LAN
                if (!isEmulator && saved.contains("10.0.2.2")) {
                    HOST_LAN_URL
                } else {
                    saved
                }
            } else {
                if (isEmulator) EMULATOR_URL else HOST_LAN_URL
            }
        }
        set(value) {
            val normalized = if (value.endsWith("/")) value else "$value/"
            prefs.edit().putString(KEY_BASE_URL, normalized).apply()
            ApiClient.reset()
        }

    var authToken: String?
        get() = prefs.getString(KEY_AUTH_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()
        }

    val isLoggedIn: Boolean
        get() = !authToken.isNullOrBlank()

    fun saveUserProfile(profile: UserProfile) {
        val serialized = json.encodeToString(profile)
        prefs.edit().putString(KEY_USER_PROFILE, serialized).apply()
    }

    fun getUserProfile(): UserProfile {
        val serialized = prefs.getString(KEY_USER_PROFILE, null)
        return if (serialized != null) {
            try {
                json.decodeFromString<UserProfile>(serialized)
            } catch (e: Exception) {
                UserProfile()
            }
        } else {
            UserProfile()
        }
    }

    fun clearSession() {
        prefs.edit().remove(KEY_AUTH_TOKEN).remove(KEY_USER_PROFILE).apply()
    }
}
