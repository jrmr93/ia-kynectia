package com.kynectia.tv.ui

import android.app.Activity
import android.app.Application
import android.content.pm.ActivityInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kynectia.tv.data.*
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

    data class RoutinesCatalog(
        val routines: List<WorkoutRoutine>,
        val selectedCategory: String? = null,
        val selectedDifficulty: String? = null,
        val isLoading: Boolean = false,
        val isOffline: Boolean = false,
        val selectedRoutine: WorkoutRoutine? = null
    ) : TvUiState

    data class Pairing(
        val sessionId: String,
        val pinCode: String,
        val qrPayload: String,
        val errorMessage: String? = null
    ) : TvUiState

    data class Connected(
        val sessionId: String,
        val pinCode: String,
        val athleteName: String
    ) : TvUiState

    data class LiveWorkout(
        val sessionId: String,
        val athleteName: String,
        val state: TvLiveWorkoutState,
        val isStandalone: Boolean = false
    ) : TvUiState

    data class Summary(
        val sessionId: String,
        val athleteName: String,
        val routineName: String,
        val summary: TvWorkoutSummary,
        val isStandalone: Boolean = false
    ) : TvUiState

    data class Error(val message: String) : TvUiState
}

class TvViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TvTrainingRepository(application.applicationContext)

    private val _uiState = MutableStateFlow<TvUiState>(TvUiState.Loading)
    val uiState: StateFlow<TvUiState> = _uiState.asStateFlow()

    private val _orientationMode = MutableStateFlow(TvDisplayOrientation.HORIZONTAL)
    val orientationMode: StateFlow<TvDisplayOrientation> = _orientationMode.asStateFlow()

    private val _isSoundEnabled = MutableStateFlow(true)
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()

    val isOfflineMode: StateFlow<Boolean> = TvTrainingRepository.isOffline

    private var currentSessionId: String? = null
    private var pollingJob: Job? = null
    private var localTickerJob: Job? = null
    private var lastFinishedCelebrated: Boolean = false
    private var lastAppliedRotateCount: Int = -1
    private var hasCelebratedConnection: Boolean = false

    // Estado del motor de entrenamiento autónomo en TV
    private var isStandaloneWorkout: Boolean = false
    private var activeRoutine: WorkoutRoutine? = null
    private var currentExerciseIndex: Int = 0
    private var currentSetNumber: Int = 1
    private var currentWeightKg: Double = 50.0
    private var currentReps: Int = 10
    private var isResting: Boolean = false
    private var restSecondsRemaining: Int = 60
    private var restSecondsTotal: Int = 60
    private var elapsedWorkoutSeconds: Int = 0
    private var isPaused: Boolean = false
    private val completedSetsHistory = mutableListOf<CompletedSet>()
    private var standaloneTickerJob: Job? = null
    private var standaloneRestJob: Job? = null

    init {
        TvSessionStorage.init(application.applicationContext)
        restoreOrInitSession()
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

    /**
     * Abre el catálogo de rutinas para entrenamiento directo y autónomo en la TV.
     */
    fun openRoutinesCatalog(category: String? = null, difficulty: String? = null) {
        pollingJob?.cancel()
        localTickerJob?.cancel()
        viewModelScope.launch {
            val routines = repository.getWorkouts(
                difficultyLevel = difficulty,
                category = category
            )
            _uiState.value = TvUiState.RoutinesCatalog(
                routines = routines,
                selectedCategory = category,
                selectedDifficulty = difficulty,
                isLoading = false,
                isOffline = TvTrainingRepository.isOffline.value,
                selectedRoutine = routines.firstOrNull()
            )
        }
    }

    fun selectRoutineInCatalog(routine: WorkoutRoutine) {
        val current = _uiState.value
        if (current is TvUiState.RoutinesCatalog) {
            _uiState.value = current.copy(selectedRoutine = routine)
        }
    }

    /**
     * Inicia una rutina de forma autónoma en la TV (sin requerir móvil conectado).
     */
    fun startStandaloneWorkout(routine: WorkoutRoutine) {
        pollingJob?.cancel()
        localTickerJob?.cancel()
        standaloneTickerJob?.cancel()
        standaloneRestJob?.cancel()

        isStandaloneWorkout = true
        activeRoutine = routine
        currentExerciseIndex = 0
        currentSetNumber = 1
        val firstEx = routine.exercises.firstOrNull()
        currentWeightKg = firstEx?.recommended_weight_kg ?: 50.0
        currentReps = if ((firstEx?.reps ?: 0) > 0) firstEx!!.reps else 10
        restSecondsTotal = if ((firstEx?.rest_seconds ?: 0) > 0) firstEx!!.rest_seconds else 60
        restSecondsRemaining = restSecondsTotal
        isResting = false
        isPaused = false
        elapsedWorkoutSeconds = 0
        completedSetsHistory.clear()

        TvSoundManager.playConnectedSound()
        publishStandaloneLiveState()
        startStandaloneTimer()
    }

    private fun buildStandaloneState(): TvLiveWorkoutState {
        val routine = activeRoutine
        val exercises = routine?.exercises ?: emptyList()
        val currentEx = exercises.getOrNull(currentExerciseIndex)

        return TvLiveWorkoutState(
            routineName = routine?.name ?: "RUTINA EN TV",
            exerciseName = currentEx?.exercise_name ?: "Ejercicio",
            demoImageUrl = currentEx?.demo_image_url,
            demoVideoUrl = currentEx?.demo_video_url,
            currentExerciseIndex = currentExerciseIndex,
            totalExercises = exercises.size.coerceAtLeast(1),
            currentSetNumber = currentSetNumber,
            totalSets = currentEx?.sets ?: 3,
            reps = currentReps,
            weightKg = currentWeightKg,
            isResting = isResting,
            restSecondsRemaining = restSecondsRemaining,
            restSecondsTotal = restSecondsTotal,
            elapsedWorkoutSeconds = elapsedWorkoutSeconds,
            isPaused = isPaused,
            isFinished = false,
            lastUpdated = System.currentTimeMillis()
        )
    }

    private fun publishStandaloneLiveState() {
        if (!isStandaloneWorkout) return
        _uiState.value = TvUiState.LiveWorkout(
            sessionId = "standalone",
            athleteName = "Atleta TV",
            state = buildStandaloneState(),
            isStandalone = true
        )
    }

    private fun startStandaloneTimer() {
        standaloneTickerJob?.cancel()
        standaloneTickerJob = viewModelScope.launch {
            while (isStandaloneWorkout) {
                delay(1000)
                if (!isPaused) {
                    elapsedWorkoutSeconds++
                    publishStandaloneLiveState()
                }
            }
        }
    }

    /**
     * Registra la serie actual en modo autónomo y activa la cuenta regresiva de descanso.
     */
    fun logStandaloneSet() {
        if (!isStandaloneWorkout || isResting) return

        completedSetsHistory.add(
            CompletedSet(
                setNumber = currentSetNumber,
                targetReps = currentReps,
                completedReps = currentReps,
                weightKg = currentWeightKg
            )
        )

        TvSoundManager.playCountdownBeep()

        val routine = activeRoutine
        val currentEx = routine?.exercises?.getOrNull(currentExerciseIndex)
        val restDuration = if ((currentEx?.rest_seconds ?: 0) > 0) currentEx!!.rest_seconds else 60

        isResting = true
        restSecondsTotal = restDuration
        restSecondsRemaining = restDuration
        publishStandaloneLiveState()

        standaloneRestJob?.cancel()
        standaloneRestJob = viewModelScope.launch {
            while (isResting && restSecondsRemaining > 0) {
                delay(1000)
                if (!isPaused) {
                    restSecondsRemaining--
                    TvSoundManager.checkRestTimerSound(restSecondsRemaining, true)
                    publishStandaloneLiveState()
                }
            }
            if (isResting && restSecondsRemaining <= 0) {
                advanceToNextSetOrExercise()
            }
        }
    }

    /**
     * Salta el tiempo de descanso restante y pasa a la siguiente serie o ejercicio.
     */
    fun skipStandaloneRest() {
        if (!isStandaloneWorkout || !isResting) return
        standaloneRestJob?.cancel()
        TvSoundManager.playCountdownBeep()
        advanceToNextSetOrExercise()
    }

    private fun advanceToNextSetOrExercise() {
        isResting = false
        val routine = activeRoutine ?: return
        val currentEx = routine.exercises.getOrNull(currentExerciseIndex) ?: return
        val maxSets = if (currentEx.sets > 0) currentEx.sets else 3

        if (currentSetNumber < maxSets) {
            currentSetNumber++
        } else {
            // Siguiente ejercicio
            if (currentExerciseIndex < routine.exercises.size - 1) {
                currentExerciseIndex++
                currentSetNumber = 1
                val nextEx = routine.exercises[currentExerciseIndex]
                currentWeightKg = nextEx.recommended_weight_kg ?: currentWeightKg
                currentReps = if (nextEx.reps > 0) nextEx.reps else 10
                restSecondsTotal = if (nextEx.rest_seconds > 0) nextEx.rest_seconds else 60
            } else {
                // Rutina completada con éxito
                finishStandaloneWorkout()
                return
            }
        }
        publishStandaloneLiveState()
    }

    fun adjustWeight(deltaKg: Double) {
        if (!isStandaloneWorkout || isResting) return
        currentWeightKg = (currentWeightKg + deltaKg).coerceAtLeast(0.0)
        publishStandaloneLiveState()
    }

    fun adjustReps(deltaReps: Int) {
        if (!isStandaloneWorkout || isResting) return
        currentReps = (currentReps + deltaReps).coerceAtLeast(1)
        publishStandaloneLiveState()
    }

    fun toggleWorkoutPause() {
        if (isStandaloneWorkout) {
            isPaused = !isPaused
            publishStandaloneLiveState()
        } else {
            sendRemoteCommand("TOGGLE_PAUSE")
        }
    }

    fun nextExercise() {
        if (isStandaloneWorkout) {
            val routine = activeRoutine ?: return
            if (currentExerciseIndex < routine.exercises.size - 1) {
                standaloneRestJob?.cancel()
                isResting = false
                currentExerciseIndex++
                currentSetNumber = 1
                val nextEx = routine.exercises[currentExerciseIndex]
                currentWeightKg = nextEx.recommended_weight_kg ?: currentWeightKg
                currentReps = if (nextEx.reps > 0) nextEx.reps else 10
                restSecondsTotal = if (nextEx.rest_seconds > 0) nextEx.rest_seconds else 60
                publishStandaloneLiveState()
            }
        } else {
            sendRemoteCommand("NEXT_EXERCISE")
        }
    }

    fun previousExercise() {
        if (isStandaloneWorkout) {
            val routine = activeRoutine ?: return
            if (currentExerciseIndex > 0) {
                standaloneRestJob?.cancel()
                isResting = false
                currentExerciseIndex--
                currentSetNumber = 1
                val prevEx = routine.exercises[currentExerciseIndex]
                currentWeightKg = prevEx.recommended_weight_kg ?: currentWeightKg
                currentReps = if (prevEx.reps > 0) prevEx.reps else 10
                restSecondsTotal = if (prevEx.rest_seconds > 0) prevEx.rest_seconds else 60
                publishStandaloneLiveState()
            }
        } else {
            sendRemoteCommand("PREV_EXERCISE")
        }
    }

    fun finishStandaloneWorkout() {
        standaloneTickerJob?.cancel()
        standaloneRestJob?.cancel()
        isStandaloneWorkout = false

        val routine = activeRoutine
        val totalVolume = completedSetsHistory.sumOf { it.completedReps * it.weightKg }
        val summary = TvWorkoutSummary(
            totalDurationSeconds = elapsedWorkoutSeconds,
            totalSetsCompleted = completedSetsHistory.size,
            totalVolumeKg = totalVolume
        )

        TvSoundManager.playWorkoutFinished()

        _uiState.value = TvUiState.Summary(
            sessionId = "standalone",
            athleteName = "Atleta TV",
            routineName = routine?.name ?: "Rutina TV",
            summary = summary,
            isStandalone = true
        )
    }

    fun exitWorkoutToCatalog() {
        standaloneTickerJob?.cancel()
        standaloneRestJob?.cancel()
        isStandaloneWorkout = false
        openRoutinesCatalog()
    }

    /**
     * Restaura la última sesión enlazada de Android TV si existe y no ha sido cerrada,
     * o genera una nueva sesión de vinculación. Si no hay conexión a internet,
     * abre directamente el catálogo de rutinas en modo sin conexión.
     */
    fun restoreOrInitSession() {
        pollingJob?.cancel()
        localTickerJob?.cancel()
        standaloneTickerJob?.cancel()
        standaloneRestJob?.cancel()
        isStandaloneWorkout = false
        lastFinishedCelebrated = false
        lastAppliedRotateCount = -1
        hasCelebratedConnection = false
        _uiState.value = TvUiState.Loading

        val saved = TvSessionStorage.getSession()
        if (saved == null || saved.sessionId.isBlank()) {
            createTvSession()
            return
        }

        viewModelScope.launch {
            try {
                val service = TvApiClient.getService()
                val response = service.getSessionStatus(saved.sessionId)
                if (response.success && response.data != null) {
                    val status = response.data
                    currentSessionId = status.sessionId
                    val athlete = status.pairedAthleteName ?: saved.athleteName ?: "Atleta"

                    TvSessionStorage.saveSession(
                        sessionId = status.sessionId,
                        pinCode = status.pinCode,
                        qrPayload = saved.qrPayload.ifBlank {
                            "kynectia://tv-pair?sessionId=${status.sessionId}&pin=${status.pinCode}"
                        },
                        isPaired = status.isPaired,
                        athleteName = athlete
                    )

                    if (status.isPaired) {
                        hasCelebratedConnection = true
                        if (status.state != null) {
                            val liveState = status.state
                            if (liveState.isFinished && liveState.summary != null) {
                                _uiState.value = TvUiState.Summary(
                                    sessionId = status.sessionId,
                                    athleteName = athlete,
                                    routineName = liveState.routineName,
                                    summary = liveState.summary
                                )
                            } else {
                                _uiState.value = TvUiState.LiveWorkout(
                                    sessionId = status.sessionId,
                                    athleteName = athlete,
                                    state = liveState
                                )
                            }
                        } else {
                            _uiState.value = TvUiState.Connected(
                                sessionId = status.sessionId,
                                pinCode = status.pinCode,
                                athleteName = athlete
                            )
                        }
                    } else {
                        _uiState.value = TvUiState.Pairing(
                            sessionId = status.sessionId,
                            pinCode = status.pinCode,
                            qrPayload = saved.qrPayload.ifBlank {
                                "kynectia://tv-pair?sessionId=${status.sessionId}&pin=${status.pinCode}"
                            }
                        )
                    }

                    startPolling(status.sessionId)
                    startLocalTicker()
                } else {
                    TvSessionStorage.clearSession()
                    createTvSession()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Si no hay red, abrir catálogo en modo offline en lugar de pantalla de error bloqueante
                openRoutinesCatalog()
            }
        }
    }

    fun forceNewTvSession() {
        TvSessionStorage.clearSession()
        createTvSession()
    }

    fun createTvSession() {
        pollingJob?.cancel()
        localTickerJob?.cancel()
        lastFinishedCelebrated = false
        lastAppliedRotateCount = -1
        hasCelebratedConnection = false
        _uiState.value = TvUiState.Loading

        viewModelScope.launch {
            try {
                val service = TvApiClient.getService()
                val response = service.createSession()
                if (response.success && response.data != null) {
                    val session = response.data
                    currentSessionId = session.sessionId
                    TvSessionStorage.saveSession(
                        sessionId = session.sessionId,
                        pinCode = session.pinCode,
                        qrPayload = session.qrPayload,
                        isPaired = false
                    )
                    _uiState.value = TvUiState.Pairing(
                        sessionId = session.sessionId,
                        pinCode = session.pinCode,
                        qrPayload = session.qrPayload
                    )
                    startPolling(session.sessionId)
                    startLocalTicker()
                } else {
                    // Fallback transparente al catálogo si el backend responde con error
                    openRoutinesCatalog()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Servidor no disponible o modo sin conexión -> abrir catálogo offline automáticamente
                openRoutinesCatalog()
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

                        if (lastAppliedRotateCount == -1) {
                            lastAppliedRotateCount = status.rotateCount
                        } else if (status.rotateCount > lastAppliedRotateCount) {
                            lastAppliedRotateCount = status.rotateCount
                            if (status.orientation != null) {
                                val target = TvDisplayOrientation.entries.find { it.name == status.orientation }
                                if (target != null) _orientationMode.value = target
                            } else {
                                cycleOrientation()
                            }
                        }

                        if (status.isPaired && status.state != null) {
                            TvSessionStorage.updatePairedStatus(isPaired = true, athleteName = athlete)
                            hasCelebratedConnection = true
                            val liveState = status.state

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
                        } else if (status.isPaired && status.state == null) {
                            TvSessionStorage.updatePairedStatus(isPaired = true, athleteName = athlete)
                            if (!hasCelebratedConnection) {
                                hasCelebratedConnection = true
                                TvSoundManager.playConnectedSound()
                            }
                            _uiState.value = TvUiState.Connected(
                                sessionId = sessionId,
                                pinCode = status.pinCode,
                                athleteName = athlete
                            )
                        } else if (!status.isPaired) {
                            hasCelebratedConnection = false
                            val current = _uiState.value
                            if (current !is TvUiState.Pairing && current !is TvUiState.RoutinesCatalog && current !is TvUiState.LiveWorkout) {
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
                    if (e is retrofit2.HttpException && e.code() == 404) {
                        TvSessionStorage.clearSession()
                        createTvSession()
                        return@launch
                    }
                }
                delay(800)
            }
        }
    }

    private fun startLocalTicker() {
        localTickerJob?.cancel()
        localTickerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _uiState.value
                if (current is TvUiState.LiveWorkout && !current.isStandalone) {
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

    fun sendRemoteCommand(command: String) {
        val sessionId = currentSessionId ?: return
        viewModelScope.launch {
            try {
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
            if (current.isStandalone) {
                if (current.state.isResting) {
                    skipStandaloneRest()
                } else {
                    logStandaloneSet()
                }
            } else {
                if (current.state.isResting) {
                    sendRemoteCommand("SKIP_REST")
                } else {
                    sendRemoteCommand("TOGGLE_PAUSE")
                }
            }
        }
    }

    fun handleDpadRight() {
        val current = _uiState.value
        if (current is TvUiState.LiveWorkout) {
            if (current.isStandalone) {
                if (current.state.isResting) {
                    skipStandaloneRest()
                } else {
                    nextExercise()
                }
            } else {
                if (current.state.isResting) {
                    sendRemoteCommand("SKIP_REST")
                } else {
                    sendRemoteCommand("NEXT_EXERCISE")
                }
            }
        }
    }

    fun handleDpadLeft() {
        val current = _uiState.value
        if (current is TvUiState.LiveWorkout) {
            if (current.isStandalone) {
                previousExercise()
            } else {
                sendRemoteCommand("PREV_EXERCISE")
            }
        }
    }

    fun handleDpadUp(): Boolean {
        val current = _uiState.value
        if (current is TvUiState.LiveWorkout && current.isStandalone && !current.state.isResting) {
            adjustWeight(2.5)
            return true
        }
        return false
    }

    fun handleDpadDown(): Boolean {
        val current = _uiState.value
        if (current is TvUiState.LiveWorkout && current.isStandalone && !current.state.isResting) {
            adjustWeight(-2.5)
            return true
        }
        return false
    }

    fun disconnect() {
        val sessionId = currentSessionId
        TvSessionStorage.clearSession()
        pollingJob?.cancel()
        localTickerJob?.cancel()
        standaloneTickerJob?.cancel()
        standaloneRestJob?.cancel()
        isStandaloneWorkout = false
        _uiState.value = TvUiState.Loading
        viewModelScope.launch {
            if (sessionId != null) {
                try {
                    TvApiClient.getService().disconnect(sessionId)
                } catch (_: Exception) {}
            }
            createTvSession()
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        localTickerJob?.cancel()
        standaloneTickerJob?.cancel()
        standaloneRestJob?.cancel()
    }
}
