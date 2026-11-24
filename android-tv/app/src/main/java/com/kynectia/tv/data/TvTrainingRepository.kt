package com.kynectia.tv.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class TvTrainingRepository(private val context: Context) {
    private val api = TvApiClient.getService()

    private val _cachedWorkouts = MutableStateFlow<List<WorkoutRoutine>>(TvMockDataProvider.mockRoutines)
    val cachedWorkouts: StateFlow<List<WorkoutRoutine>> = _cachedWorkouts.asStateFlow()

    private val _cachedExercises = MutableStateFlow<List<Exercise>>(TvMockDataProvider.mockExercises)
    val cachedExercises: StateFlow<List<Exercise>> = _cachedExercises.asStateFlow()

    companion object {
        private const val TAG = "TvTrainingRepository"
        private const val FAST_NETWORK_TIMEOUT_MS = 2500L

        private val _isOffline = MutableStateFlow(false)
        val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

        fun setOfflineMode(offline: Boolean) {
            _isOffline.value = offline
        }
    }

    suspend fun getWorkouts(
        search: String? = null,
        difficultyLevel: String? = null,
        category: String? = null
    ): List<WorkoutRoutine> = withContext(Dispatchers.IO) {
        try {
            val response = withTimeoutOrNull(FAST_NETWORK_TIMEOUT_MS) {
                api.getWorkouts(search = search, difficultyLevel = difficultyLevel, category = category)
            }
            if (response?.success == true && !response.data.isNullOrEmpty()) {
                _cachedWorkouts.value = response.data
                _isOffline.value = false
            } else if (response == null) {
                _isOffline.value = true
            }
        } catch (e: Exception) {
            Log.d(TAG, "Servidor no alcanzable para getWorkouts (${e.message}), activando modo sin conexión")
            _isOffline.value = true
        }

        var list = _cachedWorkouts.value
        if (!search.isNullOrBlank()) {
            list = list.filter {
                it.name.contains(search, ignoreCase = true) ||
                        (it.description?.contains(search, ignoreCase = true) == true)
            }
        }
        if (!difficultyLevel.isNullOrBlank() && !difficultyLevel.equals("Todas", ignoreCase = true)) {
            list = list.filter { it.difficulty_level.equals(difficultyLevel, ignoreCase = true) }
        }
        if (!category.isNullOrBlank() && !category.equals("Todas", ignoreCase = true)) {
            list = list.filter {
                it.category.contains(category, ignoreCase = true) ||
                        it.goal.contains(category, ignoreCase = true)
            }
        }
        return@withContext list
    }

    suspend fun getWorkoutDetail(id: Int): WorkoutRoutine? = withContext(Dispatchers.IO) {
        val existing = _cachedWorkouts.value.find { it.id == id }
        if (existing != null && existing.exercises.isNotEmpty()) {
            return@withContext existing
        }

        try {
            val response = withTimeoutOrNull(FAST_NETWORK_TIMEOUT_MS) {
                api.getWorkoutById(id)
            }
            if (response?.success == true && response.data != null) {
                val fresh = response.data
                _cachedWorkouts.value = _cachedWorkouts.value.map { if (it.id == id) fresh else it }
                _isOffline.value = false
                return@withContext fresh
            }
        } catch (_: Exception) {
            _isOffline.value = true
        }

        return@withContext existing ?: TvMockDataProvider.mockRoutines.find { it.id == id }
    }
}
