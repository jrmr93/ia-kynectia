package com.kynectia.app.data.tv

import android.content.Context
import android.util.Log
import com.kynectia.app.data.api.ApiClient
import com.kynectia.app.data.model.TvLiveWorkoutPayload
import com.kynectia.app.data.model.TvPairRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

object TvCastManager {
    private const val TAG = "TvCastManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var appContext: Context? = null

    private val _isTvConnected = MutableStateFlow(false)
    val isTvConnected: StateFlow<Boolean> = _isTvConnected.asStateFlow()

    private val _pairedSessionId = MutableStateFlow<String?>(null)
    val pairedSessionId: StateFlow<String?> = _pairedSessionId.asStateFlow()

    private val _pairedPin = MutableStateFlow<String?>(null)
    val pairedPin: StateFlow<String?> = _pairedPin.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun initialize(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    suspend fun pairWithPin(context: Context, pin: String, athleteName: String): Boolean {
        initialize(context)
        _isConnecting.value = true
        _errorMessage.value = null
        try {
            val api = ApiClient.getService(context.applicationContext)
            val response = api.pairWithTv(
                TvPairRequest(
                    pinCode = pin.trim(),
                    athleteName = athleteName
                )
            )

            if (response.success && response.data != null) {
                val data = response.data
                _pairedSessionId.value = data.sessionId
                _pairedPin.value = data.pinCode
                _isTvConnected.value = true
                _isConnecting.value = false
                return true
            } else {
                _errorMessage.value = response.message ?: "Código PIN no válido o pantalla no encontrada"
                _isConnecting.value = false
                return false
            }
        } catch (e: Exception) {
            _errorMessage.value = "Error al conectar: ${e.localizedMessage}"
            _isConnecting.value = false
            return false
        }
    }

    suspend fun pairWithQrPayload(context: Context, payload: String, athleteName: String): Boolean {
        initialize(context)
        _isConnecting.value = true
        _errorMessage.value = null
        try {
            var extractedPin: String? = null
            var extractedSessionId: String? = null

            // Si es JSON
            if (payload.trim().startsWith("{")) {
                val json = JSONObject(payload)
                if (json.has("pinCode")) extractedPin = json.getString("pinCode")
                if (json.has("sessionId")) extractedSessionId = json.getString("sessionId")
            } else if (payload.contains("pin=")) {

                // Si es URL o querystring kynectia://tv-pair?sessionId=...&pin=...
                val parts = payload.substringAfter("?").split("&")
                for (p in parts) {
                    val kv = p.split("=")
                    if (kv.size == 2) {
                        if (kv[0] == "pin") extractedPin = kv[1]
                        if (kv[0] == "sessionId") extractedSessionId = kv[1]
                    }
                }
            } else if (payload.trim().length == 6 && payload.trim().all { it.isDigit() }) {
                extractedPin = payload.trim()
            }

            val api = ApiClient.getService(context.applicationContext)
            val response = api.pairWithTv(
                TvPairRequest(
                    pinCode = extractedPin,
                    sessionId = extractedSessionId,
                    athleteName = athleteName
                )
            )

            if (response.success && response.data != null) {
                val data = response.data
                _pairedSessionId.value = data.sessionId
                _pairedPin.value = data.pinCode
                _isTvConnected.value = true
                _isConnecting.value = false
                return true
            } else {
                _errorMessage.value = response.message ?: "Código QR inválido o expirado"
                _isConnecting.value = false
                return false
            }
        } catch (e: Exception) {
            _errorMessage.value = "Error al procesar QR: ${e.localizedMessage}"
            _isConnecting.value = false
            return false
        }
    }

    fun broadcastWorkoutState(payload: TvLiveWorkoutPayload, context: Context? = null) {
        val targetContext = context?.applicationContext ?: appContext ?: return
        val sessionId = _pairedSessionId.value ?: return
        if (!_isTvConnected.value) return

        scope.launch {
            try {
                ApiClient.getService(targetContext).updateTvState(payload.copy(sessionId = sessionId))
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo actualizar estado en TV: ${e.message}")
            }
        }
    }

    fun disconnect(context: Context? = null) {
        val targetContext = context?.applicationContext ?: appContext
        val sessionId = _pairedSessionId.value
        if (sessionId != null && targetContext != null) {
            scope.launch {
                try {
                    ApiClient.getService(targetContext).disconnectTv(sessionId)
                } catch (_: Exception) {}
            }
        }
        _isTvConnected.value = false
        _pairedSessionId.value = null
        _pairedPin.value = null
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    suspend fun consumePendingCommands(context: Context? = null): List<com.kynectia.app.data.model.TvRemoteCommandItem> {
        val targetContext = context?.applicationContext ?: appContext ?: return emptyList()
        val sessionId = _pairedSessionId.value ?: return emptyList()
        if (!_isTvConnected.value) return emptyList()

        return try {
            val response = ApiClient.getService(targetContext).consumeTvCommands(sessionId)
            if (response.success && response.data != null) {
                response.data.commands
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
