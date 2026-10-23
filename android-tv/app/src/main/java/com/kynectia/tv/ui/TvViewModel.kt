package com.kynectia.tv.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kynectia.tv.data.TvApiClient
import com.kynectia.tv.data.TvCommandRequest
import com.kynectia.tv.data.TvLiveWorkoutState
import com.kynectia.tv.data.TvWorkoutSummary
import com.kynectia.tv.sound.TvSoundManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TvDisplayOrientation(val label: String, val rotationDegrees: Float, val isVerticalLayout: Boolean) {
    HORIZONTAL("Horizontal (16:9)", 0f, false),
    VERTICAL_FULL("Vertical Completa", 0f, true),
    VERTICAL_90("Vertical 90°", 90f, true),
    VERTICAL_270("Vertical 270°", 270f, true)
}

sealed interface TvUiState {
    object Loading : TvUiState
    data class Pairing(
        val sessionId: String,
        val pinCode: String,
        val qrPayload: String,
        val errorMessage: String? = null
    ) : TvUiState

    data class LiveWorkout(
        val sessionId: String,
        val athleteName: String,
        val state: TvLiveWorkoutState
    ) : TvUiState

    data class Summary(
        val sessionId: String,
        val athleteName: String,
        val routineName: String,
        val summary: TvWorkoutSummary
    ) : TvUiState

    data class Error(val message: String) : TvUiState
}

class TvViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<TvUiState>(TvUiState.Loading)
    val uiState: StateFlow<TvUiState> = _uiState.asStateFlow()

    private val _orientationMode = MutableStateFlow(TvDisplayOrientation.HORIZONTAL)
    val orientationMode: StateFlow<TvDisplayOrientation> = _orientationMode.asStateFlow()

    private val _isSoundEnabled = MutableStateFlow(true)
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()

    private var currentSessionId: String? = null
    private var pollingJob: Job? = null
    private var localTickerJob: Job? = null
    private var lastFinishedCelebrated: Boolean = false

    init {
        createTvSession()
    }

    fun cycleOrientation(activity: Activity? = null) {
        val nextMode = when (_orientationMode.value) {
            TvDisplayOrientation.HORIZONTAL -> TvDisplayOrientation.VERTICAL_FULL
            TvDisplayOrientation.VERTICAL_FULL -> TvDisplayOrientation.VERTICAL_90
            TvDisplayOrientation.VERTICAL_90 -> TvDisplayOrientation.VERTICAL_270
            TvDisplayOrientation.VERTICAL_270 -> TvDisplayOrientation.HORIZONTAL
        }
        setOrientation(nextMode, activity)
    }

    fun setOrientation(mode: TvDisplayOrientation, activity: Activity? = null) {
        _orientationMode.value = mode
        try {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } catch (_: Exception) {}
    }

    fun toggleSound(): Boolean {
        val newState = TvSoundManager.toggleSound()
        _isSoundEnabled.value = newState
        return newState
    }

    fun createTvSession() {
        pollingJob?.cancel()
        localTickerJob?.cancel()
        lastFinishedCelebrated = false
        _uiState.value = TvUiState.Loading

        viewModelScope.launch {
            try {
                val service = TvApiClient.getService()
                val response = service.createSession()
                if (response.success && response.data != null) {
                    val session = response.data
                    currentSessionId = session.sessionId
                    _uiState.value = TvUiState.Pairing(
                        sessionId = session.sessionId,
                        pinCode = session.pinCode,
                        qrPayload = session.qrPayload
                    )
                    startPolling(session.sessionId)
                    startLocalTicker()
                } else {
                    _uiState.value = TvUiState.Error(response.message ?: "No se pudo crear la sesión de TV")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = TvUiState.Error("Error de conexión con el servidor: ${e.localizedMessage}")
            }
        }
    }

    private fun startPolling(sessionId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            val service = TvApiClient.getService()
            while (true) {
                try {
                    val res = service.getSessionStatus(sessionId)
                    if (res.success && res.data != null) {
                        val status = res.data
                        val athlete = status.pairedAthleteName ?: "Atleta"

                        if (status.isPaired && status.state != null) {
                            val liveState = status.state

                            // Reproducir efectos de sonido correspondientes
                            if (liveState.isFinished) {
                                if (!lastFinishedCelebrated) {
                                    lastFinishedCelebrated = true
                                    TvSoundManager.playWorkoutFinished()
                                }
                            } else {
                                TvSoundManager.checkRestTimerSound(
                                    liveState.restSecondsRemaining,
                                    liveState.isResting
                                )
                            }

                            if (liveState.isFinished && liveState.summary != null) {
                                _uiState.value = TvUiState.Summary(
                                    sessionId = sessionId,
                                    athleteName = athlete,
                                    routineName = liveState.routineName,
                                    summary = liveState.summary
                                )
                            } else {
                                _uiState.value = TvUiState.LiveWorkout(
                                    sessionId = sessionId,
                                    athleteName = athlete,
                                    state = liveState
                                )
                            }
                        } else if (!status.isPaired) {
                            val current = _uiState.value
                            if (current !is TvUiState.Pairing) {
                                _uiState.value = TvUiState.Pairing(
                                    sessionId = status.sessionId,
                                    pinCode = status.pinCode,
                                    qrPayload = "kynectia://tv-pair?sessionId=${status.sessionId}&pin=${status.pinCode}"
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }
                delay(800)
            }
        }
    }

    /**
     * Ticker local en segundo plano para asegurar que los pitidos del temporizador (3, 2, 1, 0)
     * se ejecuten puntualmente cada segundo sin depender de la latencia de red.
     */
    private fun startLocalTicker() {
        localTickerJob?.cancel()
        localTickerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _uiState.value
                if (current is TvUiState.LiveWorkout) {
                    val state = current.state
                    if (state.isResting && !state.isPaused && state.restSecondsRemaining > 0) {
                        val newRemaining = state.restSecondsRemaining - 1
                        TvSoundManager.checkRestTimerSound(newRemaining, true)
                        _uiState.value = current.copy(
                            state = state.copy(restSecondsRemaining = newRemaining)
                        )
                    }
                }
            }
        }
    }

    /**
     * Envía comandos del control remoto hacia el backend
     */
    fun sendRemoteCommand(command: String) {
        val sessionId = currentSessionId ?: return
        viewModelScope.launch {
            try {
                // Actualización optimista local
                val current = _uiState.value
                if (current is TvUiState.LiveWorkout) {
                    when (command) {
                        "TOGGLE_PAUSE" -> {
                            _uiState.value = current.copy(
                                state = current.state.copy(isPaused = !current.state.isPaused)
                            )
                        }
                        "SKIP_REST" -> {
                            _uiState.value = current.copy(
                                state = current.state.copy(isResting = false, restSecondsRemaining = 0)
                            )
                        }
                    }
                }

                TvApiClient.getService().sendRemoteCommand(
                    TvCommandRequest(sessionId = sessionId, command = command)
                )
            } catch (_: Exception) {}
        }
    }

    fun handleDpadCenter() {
        val current = _uiState.value
        if (current is TvUiState.LiveWorkout) {
            if (current.state.isResting) {
                sendRemoteCommand("SKIP_REST")
            } else {
                sendRemoteCommand("TOGGLE_PAUSE")
            }
        }
    }

    fun handleDpadRight() {
        val current = _uiState.value
        if (current is TvUiState.LiveWorkout) {
            if (current.state.isResting) {
                sendRemoteCommand("SKIP_REST")
            } else {
                sendRemoteCommand("NEXT_EXERCISE")
            }
        }
    }

    fun handleDpadLeft() {
        sendRemoteCommand("PREV_EXERCISE")
    }

    fun disconnect() {
        val sessionId = currentSessionId ?: return
        viewModelScope.launch {
            try {
                TvApiClient.getService().disconnect(sessionId)
            } catch (_: Exception) {}
            createTvSession()
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        localTickerJob?.cancel()
    }
}
