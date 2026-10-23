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
                val list = repository.getWorkouts(
                    search = _uiState.value.searchQuery.ifBlank { null },
                    difficultyLevel = _uiState.value.selectedDifficulty
                )
                _uiState.update { it.copy(isLoading = false, routines = list) }
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
        _uiState.update { it.copy(selectedDifficulty = difficulty) }
        loadRoutines()
    }
}
