package com.kynectia.app.ui.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kynectia.app.data.api.MockDataProvider
import com.kynectia.app.data.model.WorkoutRoutine
import com.kynectia.app.data.repository.TrainingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkoutsUiState(
    val isLoading: Boolean = false,
    val routines: List<WorkoutRoutine> = MockDataProvider.mockRoutines,
    val searchQuery: String = "",
    val selectedDifficulty: String? = null,
    val showOnlyCustom: Boolean = false,
    val customCount: Int = 0,
    val errorMessage: String? = null
)

class WorkoutsViewModel(private val repository: TrainingRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutsUiState())
    val uiState: StateFlow<WorkoutsUiState> = _uiState.asStateFlow()

    init {
        loadRoutines()
    }

    fun loadRoutines() {
        viewModelScope.launch {
            try {
                val customRoutines = repository.getCustomWorkouts()
                val allList = repository.getWorkouts(
                    search = _uiState.value.searchQuery.ifBlank { null },
                    difficultyLevel = _uiState.value.selectedDifficulty
                )

                val displayList = if (_uiState.value.showOnlyCustom) {
                    allList.filter { it.is_custom }
                } else {
                    allList
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        routines = displayList,
                        customCount = customRoutines.size
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadRoutines()
    }

    fun onDifficultySelected(difficulty: String?) {
        _uiState.update { it.copy(selectedDifficulty = difficulty, showOnlyCustom = false) }
        loadRoutines()
    }

    fun onShowOnlyCustomSelected() {
        _uiState.update { it.copy(showOnlyCustom = true, selectedDifficulty = null) }
        loadRoutines()
    }

    fun onShowAllSelected() {
        _uiState.update { it.copy(showOnlyCustom = false, selectedDifficulty = null) }
        loadRoutines()
    }

    fun deleteRoutine(routineId: Int) {
        viewModelScope.launch {
            repository.deleteCustomWorkout(routineId)
            loadRoutines()
        }
    }

    fun duplicateRoutine(routine: WorkoutRoutine) {
        viewModelScope.launch {
            val duplicated = routine.copy(
                id = 0,
                name = "${routine.name} (Mi versión)",
                description = routine.description ?: "Rutina personalizada basada en ${routine.name}",
                is_custom = true
            )
            repository.saveCustomWorkout(duplicated)
            loadRoutines()
        }
    }
}
