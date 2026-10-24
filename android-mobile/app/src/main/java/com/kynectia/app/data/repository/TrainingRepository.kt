package com.kynectia.app.data.repository

import android.content.Context
import android.util.Log
import com.kynectia.app.data.api.ApiClient
import com.kynectia.app.data.api.MockDataProvider
import com.kynectia.app.data.api.SessionManager
import com.kynectia.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class TrainingRepository(private val context: Context) {
    private val api = ApiClient.getService(context)
    private val sessionManager = SessionManager.getInstance(context)

    // Caché en memoria para carga ultrarrápida instantánea (0ms)
    private val cachedWorkouts = MutableStateFlow<List<WorkoutRoutine>>(MockDataProvider.mockRoutines)
    private val cachedExercises = MutableStateFlow<List<Exercise>>(MockDataProvider.mockExercises)
    private val cachedMuscles = MutableStateFlow<List<MuscleGroup>>(MockDataProvider.mockMuscles)

    private val _completedWorkoutsHistory = MutableStateFlow<List<WorkoutSummaryResult>>(emptyList())
    val completedWorkoutsHistory: StateFlow<List<WorkoutSummaryResult>> = _completedWorkoutsHistory.asStateFlow()

    companion object {
        private const val TAG = "TrainingRepository"
        private const val FAST_NETWORK_TIMEOUT_MS = 6000L

        // Estado reactivo compartido en toda la aplicación para indicar si está en modo offline / fallback
        private val _isOfflineOrFallback = MutableStateFlow<Boolean>(false)
        val isOfflineOrFallback: StateFlow<Boolean> = _isOfflineOrFallback.asStateFlow()

        fun setOfflineMode(offline: Boolean) {
            _isOfflineOrFallback.value = offline
        }
    }

    init {
        // Si el usuario inició sesión offline con token mock, inicializar en modo offline
        if (sessionManager.authToken?.startsWith("mock_") == true) {
            _isOfflineOrFallback.value = true
        }
    }

    suspend fun getWorkouts(search: String? = null, difficultyLevel: String? = null, category: String? = null): List<WorkoutRoutine> = withContext(Dispatchers.IO) {
        // Intento de actualización en red con timeout rápido no bloqueante
        try {
            val response = withTimeoutOrNull(FAST_NETWORK_TIMEOUT_MS) {
                api.getWorkouts(search = search, difficultyLevel = difficultyLevel, category = category)
            }
            if (response?.success == true && !response.data.isNullOrEmpty()) {
                cachedWorkouts.value = response.data
                _isOfflineOrFallback.value = false
            } else if (response == null) {
                _isOfflineOrFallback.value = true
            }
        } catch (e: Exception) {
            Log.d(TAG, "Red no disponible para getWorkouts (${e.message}), usando datos cacheados")
            _isOfflineOrFallback.value = true
        }

        var list = cachedWorkouts.value
        if (!search.isNullOrBlank()) {
            list = list.filter { it.name.contains(search, ignoreCase = true) || (it.description?.contains(search, ignoreCase = true) == true) }
        }
        if (!difficultyLevel.isNullOrBlank()) {
            list = list.filter { it.difficulty_level.equals(difficultyLevel, ignoreCase = true) }
        }
        return@withContext list
    }

    suspend fun getWorkoutDetail(id: Int): WorkoutRoutine? = withContext(Dispatchers.IO) {
        // Consultar primero al backend para obtener los ejercicios reales actualizados de PostgreSQL
        try {
            val response = withTimeoutOrNull(FAST_NETWORK_TIMEOUT_MS) {
                api.getWorkoutById(id)
            }
            if (response?.success == true && response.data != null) {
                val fresh = response.data
                cachedWorkouts.value = cachedWorkouts.value.map { if (it.id == id) fresh else it }
                _isOfflineOrFallback.value = false
                return@withContext fresh
            } else if (response == null) {
                _isOfflineOrFallback.value = true
            }
        } catch (e: Exception) {
            Log.d(TAG, "Red no disponible para getWorkoutDetail ($id): ${e.message}")
            _isOfflineOrFallback.value = true
        }

        val local = cachedWorkouts.value.find { it.id == id }
        return@withContext local ?: MockDataProvider.mockRoutines.find { it.id == id }
    }

    suspend fun getExercises(search: String? = null): List<Exercise> = withContext(Dispatchers.IO) {
        try {
            val response = withTimeoutOrNull(FAST_NETWORK_TIMEOUT_MS) {
                api.getExercises(search = search)
            }
            if (response?.success == true && !response.data.isNullOrEmpty()) {
                cachedExercises.value = response.data
                _isOfflineOrFallback.value = false
            } else if (response == null) {
                _isOfflineOrFallback.value = true
            }
        } catch (e: Exception) {
            Log.d(TAG, "Red no disponible para getExercises")
            _isOfflineOrFallback.value = true
        }

        var list = cachedExercises.value
        if (!search.isNullOrBlank()) {
            list = list.filter { it.name.contains(search, ignoreCase = true) || it.primary_muscle_name?.contains(search, ignoreCase = true) == true }
        }
        return@withContext list
    }

    suspend fun getMuscles(): List<MuscleGroup> = withContext(Dispatchers.IO) {
        try {
            val response = withTimeoutOrNull(FAST_NETWORK_TIMEOUT_MS) {
                api.getMuscles()
            }
            if (response?.success == true && !response.data.isNullOrEmpty()) {
                cachedMuscles.value = response.data
                _isOfflineOrFallback.value = false
            } else if (response == null) {
                _isOfflineOrFallback.value = true
            }
        } catch (e: Exception) {
            Log.d(TAG, "Red no disponible para getMuscles")
            _isOfflineOrFallback.value = true
        }
        return@withContext cachedMuscles.value
    }

    suspend fun login(email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = withTimeoutOrNull(FAST_NETWORK_TIMEOUT_MS) {
                api.login(LoginRequest(email, password))
            }
            if (response?.success == true && response.data != null) {
                sessionManager.authToken = response.data.token
                val profile = response.data.user ?: response.data.admin ?: UserProfile(email = email)
                sessionManager.saveUserProfile(profile)

                _isOfflineOrFallback.value = false

                // Sincronizar inmediatamente datos reales de PostgreSQL
                syncFromBackend()
                return@withContext true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Backend login falló: ${e.message}")
        }

        if (email.isNotBlank() && password.length >= 6) {
            sessionManager.authToken = "mock_jwt_token_${System.currentTimeMillis()}"
            sessionManager.saveUserProfile(UserProfile(email = email, fullName = "Atleta Kynectia"))
            _isOfflineOrFallback.value = true
            return@withContext true
        }
        return@withContext false
    }

    suspend fun syncFromBackend() = withContext(Dispatchers.IO) {
        try {
            val workouts = withTimeoutOrNull(FAST_NETWORK_TIMEOUT_MS) { api.getWorkouts() }
            if (workouts?.success == true && !workouts.data.isNullOrEmpty()) {
                cachedWorkouts.value = workouts.data
                _isOfflineOrFallback.value = false
            }
            val exercises = withTimeoutOrNull(FAST_NETWORK_TIMEOUT_MS) { api.getExercises() }
            if (exercises?.success == true && !exercises.data.isNullOrEmpty()) {
                cachedExercises.value = exercises.data
            }
            val muscles = withTimeoutOrNull(FAST_NETWORK_TIMEOUT_MS) { api.getMuscles() }
            if (muscles?.success == true && !muscles.data.isNullOrEmpty()) {
                cachedMuscles.value = muscles.data
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error sincronizando datos reales del backend: ${e.message}")
            _isOfflineOrFallback.value = true
        }
    }

    /**
     * Prueba rápida de conectividad con el servidor backend
     */
    suspend fun checkServerConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = withTimeoutOrNull(2500L) {
                api.getWorkouts(limit = 1)
            }
            val isOnline = response?.success == true
            _isOfflineOrFallback.value = !isOnline
            if (isOnline) {
                syncFromBackend()
            }
            return@withContext isOnline
        } catch (e: Exception) {
            _isOfflineOrFallback.value = true
            return@withContext false
        }
    }

    fun saveWorkoutResult(result: WorkoutSummaryResult) {
        _completedWorkoutsHistory.value = _completedWorkoutsHistory.value + result
    }

    fun getSessionManager(): SessionManager = sessionManager
}
