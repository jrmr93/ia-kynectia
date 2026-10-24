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
import kotlin.math.roundToInt

data class ExerciseSetState(
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val isCompleted: Boolean = false
)

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
    val summaryResult: WorkoutSummaryResult? = null,
    val exerciseSetsMap: Map<Int, List<ExerciseSetState>> = emptyMap(),
    val selectedSetIndex: Int = 0,
    val exerciseNotes: Map<Int, String> = emptyMap(),
    val availableExercisesCatalog: List<Exercise> = emptyList()
)

class WorkoutPlayerViewModel(
    private val repository: TrainingRepository,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var workoutTimerJob: Job? = null
    private var restTimerJob: Job? = null
    private var tvRemoteCommandsJob: Job? = null

    fun loadRoutine(routineId: Int) {
        viewModelScope.launch {
            val routine = repository.getWorkoutDetail(routineId)
            val catalog = repository.getExercises()
            if (routine != null && routine.exercises.isNotEmpty()) {
                val initialSetsMap = mutableMapOf<Int, List<ExerciseSetState>>()
                routine.exercises.forEach { ex ->
                    val setsCount = if (ex.sets > 0) ex.sets else 5
                    val defaultWeight = ex.recommended_weight_kg ?: 60.0
                    val defaultReps = if (ex.reps > 0) ex.reps else 8
                    val sets = (1..setsCount).map { s ->
                        ExerciseSetState(
                            setNumber = s,
                            weightKg = defaultWeight,
                            reps = defaultReps,
                            isCompleted = false
                        )
                    }
                    initialSetsMap[ex.exercise_id] = sets
                }

                val firstExercise = routine.exercises.first()
                val firstExerciseSets = initialSetsMap[firstExercise.exercise_id] ?: emptyList()
                val initialSet = firstExerciseSets.firstOrNull()

                _uiState.update {
                    it.copy(
                        routine = routine,
                        currentExerciseIndex = 0,
                        currentSetNumber = 1,
                        selectedSetIndex = 0,
                        targetReps = initialSet?.reps ?: firstExercise.reps,
                        completedRepsInput = initialSet?.reps ?: firstExercise.reps,
                        weightKgInput = initialSet?.weightKg ?: (firstExercise.recommended_weight_kg ?: 60.0),
                        restSecondsTotal = if (firstExercise.rest_seconds > 0) firstExercise.rest_seconds else 60,
                        restSecondsRemaining = if (firstExercise.rest_seconds > 0) firstExercise.rest_seconds else 60,
                        elapsedWorkoutSeconds = 0,
                        isPaused = false,
                        completedSetsByExercise = emptyMap(),
                        exerciseSetsMap = initialSetsMap,
                        availableExercisesCatalog = catalog,
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
                                        logActiveSet()
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

    /**
     * Selecciona una serie de la tabla para enfocarla o editarla
     */
    fun selectSet(setIndex: Int) {
        val state = _uiState.value
        val routine = state.routine ?: return
        val currentEx = routine.exercises.getOrNull(state.currentExerciseIndex) ?: return
        val sets = state.exerciseSetsMap[currentEx.exercise_id] ?: return
        if (setIndex in sets.indices) {
            val selected = sets[setIndex]
            _uiState.update {
                it.copy(
                    selectedSetIndex = setIndex,
                    currentSetNumber = selected.setNumber,
                    weightKgInput = selected.weightKg,
                    completedRepsInput = selected.reps,
                    targetReps = selected.reps
                )
            }
            syncStateToTv()
        }
    }

    /**
     * Actualiza el peso de una serie específica
     */
    fun updateSetWeight(setIndex: Int, newWeightKg: Double) {
        val state = _uiState.value
        val routine = state.routine ?: return
        val currentEx = routine.exercises.getOrNull(state.currentExerciseIndex) ?: return
        val sets = state.exerciseSetsMap[currentEx.exercise_id]?.toMutableList() ?: return
        if (setIndex in sets.indices) {
            val validWeight = maxOf(0.0, newWeightKg)
            sets[setIndex] = sets[setIndex].copy(weightKg = validWeight)
            val updatedMap = state.exerciseSetsMap.toMutableMap()
            updatedMap[currentEx.exercise_id] = sets

            _uiState.update {
                it.copy(
                    exerciseSetsMap = updatedMap,
                    weightKgInput = if (it.selectedSetIndex == setIndex) validWeight else it.weightKgInput
                )
            }
            syncStateToTv()
        }
    }

    /**
     * Actualiza las repeticiones de una serie específica
     */
    fun updateSetReps(setIndex: Int, newReps: Int) {
        val state = _uiState.value
        val routine = state.routine ?: return
        val currentEx = routine.exercises.getOrNull(state.currentExerciseIndex) ?: return
        val sets = state.exerciseSetsMap[currentEx.exercise_id]?.toMutableList() ?: return
        if (setIndex in sets.indices) {
            val validReps = maxOf(1, newReps)
            sets[setIndex] = sets[setIndex].copy(reps = validReps)
            val updatedMap = state.exerciseSetsMap.toMutableMap()
            updatedMap[currentEx.exercise_id] = sets

            _uiState.update {
                it.copy(
                    exerciseSetsMap = updatedMap,
                    completedRepsInput = if (it.selectedSetIndex == setIndex) validReps else it.completedRepsInput
                )
            }
            syncStateToTv()
        }
    }

    /**
     * Ajusta el peso de la serie activa (+ / -)
     */
    fun updateWeightInput(delta: Double) {
        val state = _uiState.value
        val newWeight = maxOf(0.0, state.weightKgInput + delta)
        updateSetWeight(state.selectedSetIndex, newWeight)
    }

    /**
     * Ajusta las repeticiones de la serie activa (+ / -)
     */
    fun updateRepsInput(delta: Int) {
        val state = _uiState.value
        val newReps = maxOf(1, state.completedRepsInput + delta)
        updateSetReps(state.selectedSetIndex, newReps)
    }

    /**
     * Agrega una nueva serie (+) al ejercicio actual con el mismo peso y reps de la última
     */
    fun addSet() {
        val state = _uiState.value
        val routine = state.routine ?: return
        val currentEx = routine.exercises.getOrNull(state.currentExerciseIndex) ?: return
        val existingSets = state.exerciseSetsMap[currentEx.exercise_id] ?: emptyList()

        val lastSet = existingSets.lastOrNull()
        val defaultWeight = lastSet?.weightKg ?: (currentEx.recommended_weight_kg ?: 60.0)
        val defaultReps = lastSet?.reps ?: (if (currentEx.reps > 0) currentEx.reps else 8)

        val newSet = ExerciseSetState(
            setNumber = existingSets.size + 1,
            weightKg = defaultWeight,
            reps = defaultReps,
            isCompleted = false
        )

        val updatedMap = state.exerciseSetsMap.toMutableMap()
        updatedMap[currentEx.exercise_id] = existingSets + newSet

        _uiState.update {
            it.copy(exerciseSetsMap = updatedMap)
        }
        syncStateToTv()
    }

    /**
     * Elimina la última serie del ejercicio actual (Delete)
     */
    fun deleteLastSet() {
        val state = _uiState.value
        val routine = state.routine ?: return
        val currentEx = routine.exercises.getOrNull(state.currentExerciseIndex) ?: return
        val existingSets = state.exerciseSetsMap[currentEx.exercise_id] ?: return
        if (existingSets.size <= 1) return

        val updatedSets = existingSets.dropLast(1)
        val updatedMap = state.exerciseSetsMap.toMutableMap()
        updatedMap[currentEx.exercise_id] = updatedSets

        val newSelectedIndex = minOf(state.selectedSetIndex, updatedSets.size - 1)
        val selected = updatedSets[newSelectedIndex]

        _uiState.update {
            it.copy(
                exerciseSetsMap = updatedMap,
                selectedSetIndex = newSelectedIndex,
                currentSetNumber = selected.setNumber,
                weightKgInput = selected.weightKg,
                completedRepsInput = selected.reps
            )
        }
        syncStateToTv()
    }

    /**
     * Elimina una serie específica y reenumera
     */
    fun deleteSet(setIndex: Int) {
        val state = _uiState.value
        val routine = state.routine ?: return
        val currentEx = routine.exercises.getOrNull(state.currentExerciseIndex) ?: return
        val existingSets = state.exerciseSetsMap[currentEx.exercise_id] ?: return
        if (existingSets.size <= 1 || setIndex !in existingSets.indices) return

        val mutable = existingSets.toMutableList()
        mutable.removeAt(setIndex)
        val renumbered = mutable.mapIndexed { idx, s -> s.copy(setNumber = idx + 1) }

        val updatedMap = state.exerciseSetsMap.toMutableMap()
        updatedMap[currentEx.exercise_id] = renumbered

        val newSelectedIndex = minOf(state.selectedSetIndex, renumbered.size - 1)
        val selected = renumbered[newSelectedIndex]

        _uiState.update {
            it.copy(
                exerciseSetsMap = updatedMap,
                selectedSetIndex = newSelectedIndex,
                currentSetNumber = selected.setNumber,
                weightKgInput = selected.weightKg,
                completedRepsInput = selected.reps
            )
        }
        syncStateToTv()
    }

    /**
     * "Log Set" (Registra la serie activa actualmente seleccionada)
     */
    fun logActiveSet() {
        val state = _uiState.value
        val routine = state.routine ?: return
        val currentEx = routine.exercises.getOrNull(state.currentExerciseIndex) ?: return
        val sets = state.exerciseSetsMap[currentEx.exercise_id]?.toMutableList() ?: return
        val activeIndex = state.selectedSetIndex

        if (activeIndex in sets.indices) {
            val current = sets[activeIndex]
            val completed = current.copy(
                weightKg = state.weightKgInput,
                reps = state.completedRepsInput,
                isCompleted = true
            )
            sets[activeIndex] = completed

            val updatedSetsMap = state.exerciseSetsMap.toMutableMap()
            updatedSetsMap[currentEx.exercise_id] = sets

            // Registrar en completedSetsByExercise
            val newCompletedSet = CompletedSet(
                setNumber = completed.setNumber,
                targetReps = completed.reps,
                completedReps = completed.reps,
                weightKg = completed.weightKg,
                isCompleted = true
            )
            val updatedCompletedMap = state.completedSetsByExercise.toMutableMap()
            val existingCompleted = updatedCompletedMap[currentEx.exercise_id]?.filter { it.setNumber != completed.setNumber } ?: emptyList()
            updatedCompletedMap[currentEx.exercise_id] = (existingCompleted + newCompletedSet).sortedBy { it.setNumber }

            // Buscar la siguiente serie no completada
            val nextUncompletedIndex = sets.indexOfFirst { !it.isCompleted }
            val nextIndex = if (nextUncompletedIndex != -1) nextUncompletedIndex else minOf(activeIndex + 1, sets.size - 1)
            val nextSet = sets[nextIndex]

            triggerVibration()

            _uiState.update {
                it.copy(
                    exerciseSetsMap = updatedSetsMap,
                    completedSetsByExercise = updatedCompletedMap,
                    selectedSetIndex = nextIndex,
                    currentSetNumber = nextSet.setNumber,
                    weightKgInput = nextSet.weightKg,
                    completedRepsInput = nextSet.reps,
                    isResting = true,
                    restSecondsRemaining = it.restSecondsTotal
                )
            }
            syncStateToTv()

            startRestTimer(onTimerFinished = {
                // Si todas las series de este ejercicio ya están hechas y hay más ejercicios
                val allCompleted = sets.all { it.isCompleted }
                if (allCompleted && state.currentExerciseIndex < routine.exercises.size - 1) {
                    nextExercise()
                }
            })
        }
    }

    /**
     * Alterna el estado completado de una serie al hacer clic directamente en el check
     */
    fun toggleSetCompleted(setIndex: Int) {
        val state = _uiState.value
        val routine = state.routine ?: return
        val currentEx = routine.exercises.getOrNull(state.currentExerciseIndex) ?: return
        val sets = state.exerciseSetsMap[currentEx.exercise_id]?.toMutableList() ?: return

        if (setIndex in sets.indices) {
            val item = sets[setIndex]
            val newCompleted = !item.isCompleted
            sets[setIndex] = item.copy(isCompleted = newCompleted)

            val updatedSetsMap = state.exerciseSetsMap.toMutableMap()
            updatedSetsMap[currentEx.exercise_id] = sets

            val updatedCompletedMap = state.completedSetsByExercise.toMutableMap()
            if (newCompleted) {
                val newCompletedSet = CompletedSet(
                    setNumber = item.setNumber,
                    targetReps = item.reps,
                    completedReps = item.reps,
                    weightKg = item.weightKg,
                    isCompleted = true
                )
                val existing = updatedCompletedMap[currentEx.exercise_id]?.filter { it.setNumber != item.setNumber } ?: emptyList()
                updatedCompletedMap[currentEx.exercise_id] = (existing + newCompletedSet).sortedBy { it.setNumber }
                triggerVibration()
            } else {
                val existing = updatedCompletedMap[currentEx.exercise_id]?.filter { it.setNumber != item.setNumber } ?: emptyList()
                updatedCompletedMap[currentEx.exercise_id] = existing
            }

            _uiState.update {
                it.copy(
                    exerciseSetsMap = updatedSetsMap,
                    completedSetsByExercise = updatedCompletedMap
                )
            }
            syncStateToTv()

            if (newCompleted) {
                _uiState.update { it.copy(isResting = true, restSecondsRemaining = it.restSecondsTotal) }
                startRestTimer(onTimerFinished = {})
            }
        }
    }

    // Compatibilidad para llamadas existentes
    fun completeSet() {
        logActiveSet()
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

    fun setRestSecondsTotal(seconds: Int) {
        _uiState.update {
            it.copy(
                restSecondsTotal = maxOf(5, seconds),
                restSecondsRemaining = if (it.isResting) maxOf(5, seconds) else it.restSecondsRemaining
            )
        }
        syncStateToTv()
    }

    fun skipRest() {
        restTimerJob?.cancel()
        _uiState.update { it.copy(isResting = false, restSecondsRemaining = 0) }
        syncStateToTv()
    }

    fun jumpToExercise(index: Int) {
        val state = _uiState.value
        val routine = state.routine ?: return
        if (index in routine.exercises.indices) {
            restTimerJob?.cancel()
            val nextExercise = routine.exercises[index]
            val sets = state.exerciseSetsMap[nextExercise.exercise_id] ?: emptyList()
            val firstUncompleted = sets.indexOfFirst { !it.isCompleted }.takeIf { it != -1 } ?: 0
            val activeSet = sets.getOrNull(firstUncompleted)

            _uiState.update {
                it.copy(
                    currentExerciseIndex = index,
                    selectedSetIndex = firstUncompleted,
                    currentSetNumber = activeSet?.setNumber ?: 1,
                    targetReps = activeSet?.reps ?: nextExercise.reps,
                    completedRepsInput = activeSet?.reps ?: nextExercise.reps,
                    weightKgInput = activeSet?.weightKg ?: (nextExercise.recommended_weight_kg ?: it.weightKgInput),
                    isResting = false,
                    restSecondsTotal = if (nextExercise.rest_seconds > 0) nextExercise.rest_seconds else it.restSecondsTotal,
                    restSecondsRemaining = if (nextExercise.rest_seconds > 0) nextExercise.rest_seconds else it.restSecondsTotal
                )
            }
            syncStateToTv()
        }
    }

    fun nextExercise() {
        val state = _uiState.value
        val routine = state.routine ?: return
        val nextIndex = state.currentExerciseIndex + 1

        if (nextIndex < routine.exercises.size) {
            jumpToExercise(nextIndex)
        } else {
            finishWorkout()
        }
    }

    fun previousExercise() {
        val state = _uiState.value
        val prevIndex = state.currentExerciseIndex - 1
        if (prevIndex >= 0) {
            jumpToExercise(prevIndex)
        }
    }

    fun saveExerciseNote(note: String) {
        val state = _uiState.value
        val routine = state.routine ?: return
        val currentEx = routine.exercises.getOrNull(state.currentExerciseIndex) ?: return
        val updatedNotes = state.exerciseNotes.toMutableMap()
        updatedNotes[currentEx.exercise_id] = note
        _uiState.update { it.copy(exerciseNotes = updatedNotes) }
    }

    fun swapCurrentExercise(replacementExercise: Exercise) {
        val state = _uiState.value
        val routine = state.routine ?: return
        val currentEx = routine.exercises.getOrNull(state.currentExerciseIndex) ?: return

        val replacedDetail = currentEx.copy(
            exercise_id = replacementExercise.id,
            exercise_name = replacementExercise.name,
            demo_image_url = replacementExercise.demo_image_url,
            demo_video_url = replacementExercise.demo_video_url,
            difficulty_level = replacementExercise.difficulty_level,
            exercise_type = replacementExercise.exercise_type,
            specific_instructions = replacementExercise.execution_instructions
        )

        val updatedExercises = routine.exercises.toMutableList()
        updatedExercises[state.currentExerciseIndex] = replacedDetail
        val updatedRoutine = routine.copy(exercises = updatedExercises)

        // Inicializar series para el nuevo ejercicio
        val defaultSetsCount = currentEx.sets.takeIf { it > 0 } ?: 5
        val defaultWeight = currentEx.recommended_weight_kg ?: 60.0
        val defaultReps = currentEx.reps.takeIf { it > 0 } ?: 8
        val newSets = (1..defaultSetsCount).map { s ->
            ExerciseSetState(
                setNumber = s,
                weightKg = defaultWeight,
                reps = defaultReps,
                isCompleted = false
            )
        }
        val updatedSetsMap = state.exerciseSetsMap.toMutableMap()
        updatedSetsMap[replacementExercise.id] = newSets

        _uiState.update {
            it.copy(
                routine = updatedRoutine,
                exerciseSetsMap = updatedSetsMap,
                selectedSetIndex = 0,
                currentSetNumber = 1,
                weightKgInput = defaultWeight,
                completedRepsInput = defaultReps
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
        val currentSets = state.exerciseSetsMap[currentExercise?.exercise_id] ?: emptyList()
        val totalSets = if (currentSets.isNotEmpty()) currentSets.size else (currentExercise?.sets ?: 5)

        val payload = TvLiveWorkoutPayload(
            sessionId = "",
            routineName = routine.name,
            exerciseName = currentExercise?.exercise_name ?: "Ejercicio",
            demoImageUrl = currentExercise?.demo_image_url,
            demoVideoUrl = currentExercise?.demo_video_url,
            currentExerciseIndex = state.currentExerciseIndex,
            totalExercises = routine.exercises.size,
            currentSetNumber = state.currentSetNumber,
            totalSets = totalSets,
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
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(200)
                }
            }
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        workoutTimerJob?.cancel()
        restTimerJob?.cancel()
        tvRemoteCommandsJob?.cancel()
    }
}
