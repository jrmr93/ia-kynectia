package com.kynectia.tv.data

import android.content.Context
import android.content.SharedPreferences

data class SavedTvSession(
    val sessionId: String,
    val pinCode: String,
    val qrPayload: String,
    val isPaired: Boolean,
    val athleteName: String?
)

object TvSessionStorage {
    private const val PREFS_NAME = "kynectia_tv_session_prefs"
    private const val KEY_SESSION_ID = "saved_session_id"
    private const val KEY_PIN_CODE = "saved_pin_code"
    private const val KEY_QR_PAYLOAD = "saved_qr_payload"
    private const val KEY_IS_PAIRED = "saved_is_paired"
    private const val KEY_ATHLETE_NAME = "saved_athlete_name"

    @Volatile
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            synchronized(this) {
                if (prefs == null) {
                    prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                }
            }
        }
    }

    fun saveSession(
        sessionId: String,
        pinCode: String,
        qrPayload: String,
        isPaired: Boolean = false,
        athleteName: String? = null
    ) {
        prefs?.edit()
            ?.putString(KEY_SESSION_ID, sessionId)
            ?.putString(KEY_PIN_CODE, pinCode)
            ?.putString(KEY_QR_PAYLOAD, qrPayload)
            ?.putBoolean(KEY_IS_PAIRED, isPaired)
            ?.putString(KEY_ATHLETE_NAME, athleteName)
            ?.apply()
    }

    fun updatePairedStatus(isPaired: Boolean, athleteName: String?) {
        val editor = prefs?.edit() ?: return
        editor.putBoolean(KEY_IS_PAIRED, isPaired)
        if (athleteName != null) {
            editor.putString(KEY_ATHLETE_NAME, athleteName)
        }
        editor.apply()
    }

    fun getSession(): SavedTvSession? {
        val p = prefs ?: return null
        val sessionId = p.getString(KEY_SESSION_ID, null) ?: return null
        if (sessionId.isBlank()) return null
        val pinCode = p.getString(KEY_PIN_CODE, "") ?: ""
        val qrPayload = p.getString(KEY_QR_PAYLOAD, "") ?: ""
        val isPaired = p.getBoolean(KEY_IS_PAIRED, false)
        val athleteName = p.getString(KEY_ATHLETE_NAME, null)

        return SavedTvSession(
            sessionId = sessionId,
            pinCode = pinCode,
            qrPayload = qrPayload,
            isPaired = isPaired,
            athleteName = athleteName
        )
    }

    fun clearSession() {
        prefs?.edit()?.clear()?.apply()
    }

    fun hasSavedSession(): Boolean {
        return getSession() != null
    }
}
