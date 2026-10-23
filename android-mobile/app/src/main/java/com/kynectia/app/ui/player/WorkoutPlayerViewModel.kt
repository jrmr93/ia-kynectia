package com.kynectia.app.ui.player

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kynectia.app.data.model.*
import com.kynectia.app.data.repository.TrainingRepository
import com.kynectia.app.data.tv.TvCastManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val routine: WorkoutRoutine? = null,
    val currentExerciseIndex: Int = 0,
    val currentSetNumber: Int = 1,
    val targetReps: Int = 10,
    val completedRepsInput: Int = 10,
    val weightKgInput: Double = 50.0,
    val isResting: Boolean = false,
    val restSecondsTotal: Int = 60,
    val restSecondsRemaining: Int = 60,
    val elapsedWorkoutSeconds: Int = 0,
    val isPaused: Boolean = false,
    val completedSetsByExercise: Map<Int, List<CompletedSet>> = emptyMap(),
    val isWorkoutFinished: Boolean = false,
    val summaryResult: WorkoutSummaryResult? = null
)

class WorkoutPlayerViewModel(
    private val repository: TrainingRepository,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var workoutTimerJob: Job? = null
    private var restTimerJob: Job? = null

    fun loadRoutine(routineId: Int) {
        viewModelScope.launch {
            val routine = repository.getWorkoutDetail(routineId)
            if (routine != null && routine.exercises.isNotEmpty()) {
                val firstExercise = routine.exercises.first()
                _uiState.update {
                    it.copy(
                        routine = routine,
                        currentExerciseIndex = 0,
                        currentSetNumber = 1,
                        targetReps = firstExercise.reps,
                        completedRepsInput = firstExercise.reps,
                        weightKgInput = firstExercise.recommended_weight_kg ?: 20.0,
                        restSecondsTotal = firstExercise.rest_seconds,
                        restSecondsRemaining = firstExercise.rest_seconds,
                        elapsedWorkoutSeconds = 0,
                        isPaused = false,
                        completedSetsByExercise = emptyMap(),
                        isWorkoutFinished = false,
                        summaryResult = null
                    )
                }
                syncStateToTv()
                startWorkoutTimer()
                startTvRemoteCommandsListener()
            }
        }
    }

    private var tvRemoteCommandsJob: Job? = null

    private fun startTvRemoteCommandsListener() {
        tvRemoteCommandsJob?.cancel()
        tvRemoteCommandsJob = viewModelScope.launch {
            while (true) {
                delay(700)
                if (TvCastManager.isTvConnected.value && !_uiState.value.isWorkoutFinished) {
                    try {
                        val commands = TvCastManager.consumePendingCommands(appContext)
                        for (cmd in commands) {
                            when (cmd.command) {
                                "TOGGLE_PAUSE" -> togglePause()
                                "SKIP_REST" -> {
                                    if (_uiState.value.isResting) {
                                        skipRest()
                                    } else {
                                        completeSet()
                                    }
                                }
                                "NEXT_EXERCISE" -> nextExercise()
                                "PREV_EXERCISE" -> previousExercise()
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun startWorkoutTimer() {
        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (!_uiState.value.isPaused && !_uiState.value.isWorkoutFinished) {
                    _uiState.update { it.copy(elapsedWorkoutSeconds = it.elapsedWorkoutSeconds + 1) }
                    syncStateToTv()
                }
            }
        }
    }

    fun togglePause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
        syncStateToTv()
    }


    fun updateRepsInput(delta: Int) {
        _uiState.update { it.copy(completedRepsInput = maxOf(1, it.completedRepsInput + delta)) }
    }

    fun updateWeightInput(delta: Double) {
        _uiState.update { it.copy(weightKgInput = maxOf(0.0, it.weightKgInput + delta)) }
    }

    fun completeSet() {
        val state = _uiState.value
        val routine = state.routine ?: return
        if (state.currentExerciseIndex >= routine.exercises.size) return

        val currentExercise = routine.exercises[state.currentExerciseIndex]
        val exerciseId = currentExercise.exercise_id

        val newSet = CompletedSet(
            setNumber = state.currentSetNumber,
            targetReps = state.targetReps,
            completedReps = state.completedRepsInput,
            weightKg = state.weightKgInput
        )

        val updatedMap = state.completedSetsByExercise.toMutableMap()
        val existingSets = updatedMap[exerciseId] ?: emptyList()
        updatedMap[exerciseId] = existingSets + newSet

        val hasMoreSetsInExercise = state.currentSetNumber < currentExercise.sets

        _uiState.update {
            it.copy(
                completedSetsByExercise = updatedMap,
                isResting = true,
                restSecondsTotal = currentExercise.rest_seconds,
                restSecondsRemaining = currentExercise.rest_seconds
            )
        }
        syncStateToTv()

        startRestTimer(onTimerFinished = {
            if (hasMoreSetsInExercise) {
                _uiState.update {
                    it.copy(
                        currentSetNumber = it.currentSetNumber + 1,
                        isResting = false
                    )
                }
                syncStateToTv()
            } else {
                nextExercise()
            }
        })
    }

    private fun startRestTimer(onTimerFinished: () -> Unit) {
        restTimerJob?.cancel()
        restTimerJob = viewModelScope.launch {
            while (_uiState.value.restSecondsRemaining > 0) {
                delay(1000)
                if (!_uiState.value.isPaused) {
                    _uiState.update { it.copy(restSecondsRemaining = it.restSecondsRemaining - 1) }
                    syncStateToTv()
                }
            }
            triggerVibration()
            _uiState.update { it.copy(isResting = false) }
            syncStateToTv()
            onTimerFinished()
        }
    }

    fun addRestTime(seconds: Int) {
        _uiState.update {
            val newTotal = maxOf(5, it.restSecondsTotal + seconds)
            val newRemaining = maxOf(5, it.restSecondsRemaining + seconds)
            it.copy(restSecondsTotal = newTotal, restSecondsRemaining = newRemaining)
        }
        syncStateToTv()
    }

    fun skipRest() {
        restTimerJob?.cancel()
        _uiState.update { it.copy(isResting = false, restSecondsRemaining = 0) }
        syncStateToTv()
        val state = _uiState.value
        val routine = state.routine ?: return
        val currentExercise = routine.exercises.getOrNull(state.currentExerciseIndex) ?: return

        if (state.currentSetNumber < currentExercise.sets) {
            _uiState.update { it.copy(currentSetNumber = it.currentSetNumber + 1) }
            syncStateToTv()
        } else {
            nextExercise()
        }
    }

    fun nextExercise() {
        restTimerJob?.cancel()
        val state = _uiState.value
        val routine = state.routine ?: return
        val nextIndex = state.currentExerciseIndex + 1

        if (nextIndex < routine.exercises.size) {
            val nextExercise = routine.exercises[nextIndex]
            _uiState.update {
                it.copy(
                    currentExerciseIndex = nextIndex,
                    currentSetNumber = 1,
                    targetReps = nextExercise.reps,
                    completedRepsInput = nextExercise.reps,
                    weightKgInput = nextExercise.recommended_weight_kg ?: it.weightKgInput,
                    isResting = false,
                    restSecondsTotal = nextExercise.rest_seconds,
                    restSecondsRemaining = nextExercise.rest_seconds
                )
            }
            syncStateToTv()
        } else {
            finishWorkout()
        }
    }

    fun previousExercise() {
        restTimerJob?.cancel()
        val state = _uiState.value
        val prevIndex = maxOf(0, state.currentExerciseIndex - 1)
        val routine = state.routine ?: return
        val prevExercise = routine.exercises[prevIndex]

        _uiState.update {
            it.copy(
                currentExerciseIndex = prevIndex,
                currentSetNumber = 1,
                targetReps = prevExercise.reps,
                completedRepsInput = prevExercise.reps,
                isResting = false
            )
        }
        syncStateToTv()
    }

    fun finishWorkout() {
        workoutTimerJob?.cancel()
        restTimerJob?.cancel()

        val state = _uiState.value
        val routine = state.routine ?: return

        var totalSets = 0
        var totalVolume = 0.0
        val exerciseSummaries = mutableListOf<CompletedExerciseSummary>()

        routine.exercises.forEach { ex ->
            val sets = state.completedSetsByExercise[ex.exercise_id] ?: emptyList()
            totalSets += sets.size
            sets.forEach { s ->
                totalVolume += (s.completedReps * s.weightKg)
            }
            exerciseSummaries.add(
                CompletedExerciseSummary(
                    exerciseId = ex.exercise_id,
                    exerciseName = ex.exercise_name ?: "Ejercicio #${ex.sequence_order}",
                    sets = sets
                )
            )
        }

        val summary = WorkoutSummaryResult(
            routineId = routine.id,
            routineName = routine.name,
            totalDurationSeconds = state.elapsedWorkoutSeconds,
            totalSetsCompleted = totalSets,
            totalVolumeKg = totalVolume,
            exercisesSummary = exerciseSummaries
        )

        repository.saveWorkoutResult(summary)

        _uiState.update {
            it.copy(
                isWorkoutFinished = true,
                isResting = false,
                summaryResult = summary
            )
        }

        syncStateToTv(isFinished = true, summary = summary)
    }

    fun syncStateToTv(isFinished: Boolean = false, summary: WorkoutSummaryResult? = null) {
        if (!TvCastManager.isTvConnected.value) return
        val state = _uiState.value
        val routine = state.routine ?: return
        val currentExercise = routine.exercises.getOrNull(state.currentExerciseIndex)

        val payload = TvLiveWorkoutPayload(
            sessionId = "",
            routineName = routine.name,
            exerciseName = currentExercise?.exercise_name ?: "Ejercicio",
            demoImageUrl = currentExercise?.demo_image_url,
            demoVideoUrl = currentExercise?.demo_video_url,
            currentExerciseIndex = state.currentExerciseIndex,
            totalExercises = routine.exercises.size,
            currentSetNumber = state.currentSetNumber,
            totalSets = currentExercise?.sets ?: 3,
            reps = state.completedRepsInput,
            weightKg = state.weightKgInput,
            isResting = state.isResting,
            restSecondsRemaining = state.restSecondsRemaining,
            restSecondsTotal = state.restSecondsTotal,
            elapsedWorkoutSeconds = state.elapsedWorkoutSeconds,
            isPaused = state.isPaused,
            isFinished = isFinished,
            summary = summary?.let {
                TvWorkoutSummaryPayload(
                    totalDurationSeconds = it.totalDurationSeconds,
                    totalSetsCompleted = it.totalSetsCompleted,
                    totalVolumeKg = it.totalVolumeKg
                )
            }
        )
        TvCastManager.broadcastWorkoutState(payload, appContext)
    }


    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(350)
                }
            }
        } catch (_: Exception) {
            // Ignorar si el dispositivo no soporta vibración
        }
    }

    override fun onCleared() {
        super.onCleared()
        workoutTimerJob?.cancel()
        restTimerJob?.cancel()
        tvRemoteCommandsJob?.cancel()
    }
}
