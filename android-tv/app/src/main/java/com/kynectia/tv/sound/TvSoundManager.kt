package com.kynectia.tv.sound

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

object TvSoundManager {
    private const val TAG = "TvSoundManager"
    private var toneGenerator: ToneGenerator? = null

    var isSoundEnabled: Boolean = true
    private var lastPlayedSecond: Int = -1

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo inicializar ToneGenerator: ${e.message}")
        }
    }

    /**
     * Evalúa los segundos restantes de descanso y reproduce los beeps en 3, 2, 1 y 0
     */
    fun checkRestTimerSound(remainingSeconds: Int, isResting: Boolean) {
        if (!isSoundEnabled || !isResting) {
            if (!isResting) lastPlayedSecond = -1
            return
        }

        if (remainingSeconds == lastPlayedSecond) return

        when (remainingSeconds) {
            3, 2, 1 -> {
                lastPlayedSecond = remainingSeconds
                playCountdownBeep()
            }
            0 -> {
                lastPlayedSecond = remainingSeconds
                playTimerFinished()
            }
        }
    }

    fun playCountdownBeep() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 130)
        } catch (e: Exception) {
            Log.w(TAG, "Error reproduciendo beep: ${e.message}")
        }
    }

    fun playTimerFinished() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 450)
        } catch (e: Exception) {
            Log.w(TAG, "Error reproduciendo alerta de finalización: ${e.message}")
        }
    }

    fun playWorkoutFinished() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 700)
        } catch (e: Exception) {
            Log.w(TAG, "Error reproduciendo sonido de victoria: ${e.message}")
        }
    }

    fun playConnectedSound() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
        } catch (e: Exception) {
            Log.w(TAG, "Error reproduciendo sonido de conexión: ${e.message}")
        }
    }

    fun toggleSound(): Boolean {
        isSoundEnabled = !isSoundEnabled
        if (isSoundEnabled) playCountdownBeep()
        return isSoundEnabled
    }
}
