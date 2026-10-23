package com.kynectia.app.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kynectia.app.data.api.MockDataProvider
import com.kynectia.app.data.model.Exercise
import com.kynectia.app.data.model.MuscleGroup
import com.kynectia.app.data.repository.TrainingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExercisesUiState(
    val isLoading: Boolean = false,
    val exercises: List<Exercise> = MockDataProvider.mockExercises,
    val muscles: List<MuscleGroup> = MockDataProvider.mockMuscles,
    val searchQuery: String = "",
    val selectedMuscle: String? = null,
    val selectedExerciseDetail: Exercise? = null
)

class ExercisesViewModel(private val repository: TrainingRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ExercisesUiState())
    val uiState: StateFlow<ExercisesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val exercises = repository.getExercises(search = _uiState.value.searchQuery.ifBlank { null })
            val muscles = repository.getMuscles()

            var filtered = exercises
            _uiState.value.selectedMuscle?.let { muscleName ->
                filtered = filtered.filter { it.primary_muscle_name.equals(muscleName, ignoreCase = true) }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    exercises = filtered,
                    muscles = muscles
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadData()
    }

    fun onMuscleSelected(muscleName: String?) {
        _uiState.update { it.copy(selectedMuscle = muscleName) }
        loadData()
    }

    fun showExerciseDetail(exercise: Exercise) {
        _uiState.update { it.copy(selectedExerciseDetail = exercise) }
    }

    fun dismissExerciseDetail() {
        _uiState.update { it.copy(selectedExerciseDetail = null) }
    }
}
