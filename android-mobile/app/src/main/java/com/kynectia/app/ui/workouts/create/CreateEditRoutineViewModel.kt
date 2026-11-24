package com.kynectia.app.ui.workouts.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kynectia.app.data.model.*
import com.kynectia.app.data.repository.TrainingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

data class CreateEditRoutineUiState(
    val initialRoutineId: Int? = null,
    val name: String = "",
    val description: String = "",
    val goal: String = "Hipertrofia y Resistencia",
    val difficultyLevel: String = "intermediate",
    val category: String = "Fuerza",
    val coverImageUrl: String? = "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=800",
    val exercises: List<RoutineExerciseDetail> = emptyList(),
    val catalogExercises: List<Exercise> = emptyList(),
    val catalogMuscles: List<MuscleGroup> = emptyList(),
    val isLoading: Boolean = false,
    val isPickerOpen: Boolean = false,
    val pickerSearchQuery: String = "",
    val pickerMuscleFilter: String? = null,
    val userModifiedDurationMinutes: Int? = null,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
) {
    val totalSets: Int
        get() = exercises.sumOf { it.sets }

    val calculatedDurationMinutes: Int
        get() {
            if (userModifiedDurationMinutes != null && userModifiedDurationMinutes > 0) {
                return userModifiedDurationMinutes
            }
            if (exercises.isEmpty()) return 30
            var totalSeconds = 0
            for (ex in exercises) {
                val repTime = if ((ex.duration_seconds ?: 0) > 0) ex.duration_seconds!! else 45
                totalSeconds += (ex.sets * repTime) + (ex.sets * ex.rest_seconds)
            }
            val minutes = (totalSeconds + 59) / 60
            return max(5, ((minutes + 4) / 5) * 5) // Redondear a múltiplos de 5 min
        }

    val isValid: Boolean
        get() = name.isNotBlank() && exercises.isNotEmpty()
}

class CreateEditRoutineViewModel(
    private val repository: TrainingRepository,
    private val routineId: Int? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEditRoutineUiState(initialRoutineId = routineId))
    val uiState: StateFlow<CreateEditRoutineUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val catalog = repository.getExercises()
                val muscles = repository.getMuscles()

                var loadedName = ""
                var loadedDesc = ""
                var loadedGoal = "Hipertrofia y Resistencia"
                var loadedDifficulty = "intermediate"
                var loadedCategory = "Fuerza"
                var loadedCover = "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=800"
                var loadedExercises = emptyList<RoutineExerciseDetail>()
                var loadedDuration: Int? = null

                if (routineId != null && routineId != 0) {
                    val existing = repository.getWorkoutDetail(routineId)
                    if (existing != null) {
                        loadedName = existing.name
                        loadedDesc = existing.description ?: ""
                        loadedGoal = existing.goal
                        loadedDifficulty = existing.difficulty_level
                        loadedCategory = existing.category
                        loadedCover = existing.cover_image_url ?: loadedCover
                        loadedExercises = existing.exercises
                        loadedDuration = existing.estimated_duration_minutes
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        catalogExercises = catalog,
                        catalogMuscles = muscles,
                        name = loadedName,
                        description = loadedDesc,
                        goal = loadedGoal,
                        difficultyLevel = loadedDifficulty,
                        category = loadedCategory,
                        coverImageUrl = loadedCover,
                        exercises = loadedExercises,
                        userModifiedDurationMinutes = loadedDuration
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun setName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun setDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun setGoal(goal: String) {
        _uiState.update { it.copy(goal = goal) }
    }

    fun setDifficulty(level: String) {
        _uiState.update { it.copy(difficultyLevel = level) }
    }

    fun setCategory(category: String) {
        _uiState.update { it.copy(category = category) }
    }

    fun setCoverImage(url: String) {
        _uiState.update { it.copy(coverImageUrl = url) }
    }

    fun setEstimatedDuration(minutes: Int) {
        _uiState.update { it.copy(userModifiedDurationMinutes = minutes.coerceAtLeast(5)) }
    }

    fun openExercisePicker() {
        _uiState.update { it.copy(isPickerOpen = true, pickerSearchQuery = "", pickerMuscleFilter = null) }
    }

    fun closeExercisePicker() {
        _uiState.update { it.copy(isPickerOpen = false) }
    }

    fun setPickerSearchQuery(query: String) {
        _uiState.update { it.copy(pickerSearchQuery = query) }
    }

    fun setPickerMuscleFilter(muscle: String?) {
        _uiState.update { it.copy(pickerMuscleFilter = muscle) }
    }

    fun addExercise(exercise: Exercise) {
        val currentList = _uiState.value.exercises.toMutableList()
        val defaultWeight = when (exercise.primary_muscle_name?.lowercase()) {
            "piernas", "cuádriceps" -> 50.0
            "pectorales" -> 40.0
            "dorsales", "espalda" -> 35.0
            "deltoides", "hombros" -> 16.0
            "bíceps" -> 12.0
            "tríceps" -> 15.0
            "core / abdominales" -> 0.0
            else -> 20.0
        }

        val newExercise = RoutineExerciseDetail(
            id = -(System.currentTimeMillis() % 100000L).toInt(),
            sequence_order = currentList.size + 1,
            exercise_id = exercise.id,
            exercise_name = exercise.name,
            difficulty_level = exercise.difficulty_level,
            exercise_type = exercise.exercise_type,
            demo_image_url = exercise.demo_image_url,
            demo_video_url = exercise.demo_video_url,
            sets = 3,
            reps = 10,
            rest_seconds = 60,
            recommended_weight_kg = defaultWeight,
            duration_seconds = 0,
            specific_instructions = ""
        )

        currentList.add(newExercise)
        _uiState.update { it.copy(exercises = currentList) }
    }

    fun removeExercise(index: Int) {
        val current = _uiState.value.exercises.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            // Re-secuenciar
            val reindexed = current.mapIndexed { idx, item -> item.copy(sequence_order = idx + 1) }
            _uiState.update { it.copy(exercises = reindexed) }
        }
    }

    fun moveExerciseUp(index: Int) {
        if (index <= 0) return
        val current = _uiState.value.exercises.toMutableList()
        val item = current.removeAt(index)
        current.add(index - 1, item)
        val reindexed = current.mapIndexed { idx, ex -> ex.copy(sequence_order = idx + 1) }
        _uiState.update { it.copy(exercises = reindexed) }
    }

    fun moveExerciseDown(index: Int) {
        val current = _uiState.value.exercises.toMutableList()
        if (index !in 0 until current.lastIndex) return
        val item = current.removeAt(index)
        current.add(index + 1, item)
        val reindexed = current.mapIndexed { idx, ex -> ex.copy(sequence_order = idx + 1) }
        _uiState.update { it.copy(exercises = reindexed) }
    }

    fun updateSets(index: Int, sets: Int) {
        val current = _uiState.value.exercises.toMutableList()
        if (index in current.indices) {
            val clamped = sets.coerceIn(1, 20)
            current[index] = current[index].copy(sets = clamped)
            _uiState.update { it.copy(exercises = current) }
        }
    }

    fun updateReps(index: Int, reps: Int) {
        val current = _uiState.value.exercises.toMutableList()
        if (index in current.indices) {
            val clamped = reps.coerceIn(1, 100)
            current[index] = current[index].copy(reps = clamped)
            _uiState.update { it.copy(exercises = current) }
        }
    }

    fun updateWeight(index: Int, weightKg: Double) {
        val current = _uiState.value.exercises.toMutableList()
        if (index in current.indices) {
            val clamped = weightKg.coerceAtLeast(0.0)
            current[index] = current[index].copy(recommended_weight_kg = clamped)
            _uiState.update { it.copy(exercises = current) }
        }
    }

    fun updateRestSeconds(index: Int, seconds: Int) {
        val current = _uiState.value.exercises.toMutableList()
        if (index in current.indices) {
            val clamped = seconds.coerceIn(0, 600)
            current[index] = current[index].copy(rest_seconds = clamped)
            _uiState.update { it.copy(exercises = current) }
        }
    }

    fun updateDurationSeconds(index: Int, seconds: Int) {
        val current = _uiState.value.exercises.toMutableList()
        if (index in current.indices) {
            val clamped = seconds.coerceIn(0, 3600)
            current[index] = current[index].copy(duration_seconds = clamped)
            _uiState.update { it.copy(exercises = current) }
        }
    }

    fun updateInstructions(index: Int, instructions: String) {
        val current = _uiState.value.exercises.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(specific_instructions = instructions)
            _uiState.update { it.copy(exercises = current) }
        }
    }

    fun saveRoutine(onSuccess: (WorkoutRoutine) -> Unit) {
        val state = _uiState.value
        if (!state.isValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val routineToSave = WorkoutRoutine(
                    id = state.initialRoutineId ?: 0,
                    name = state.name.trim(),
                    description = state.description.ifBlank { "Rutina personalizada creada por el usuario" },
                    goal = state.goal,
                    difficulty_level = state.difficultyLevel,
                    category = state.category,
                    estimated_duration_minutes = state.calculatedDurationMinutes,
                    cover_image_url = state.coverImageUrl,
                    exercises = state.exercises.mapIndexed { idx, ex -> ex.copy(sequence_order = idx + 1) },
                    is_custom = true
                )

                val saved = repository.saveCustomWorkout(routineToSave)
                _uiState.update { it.copy(isLoading = false, saveSuccess = true) }
                onSuccess(saved)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error al guardar: ${e.message}") }
            }
        }
    }
}
