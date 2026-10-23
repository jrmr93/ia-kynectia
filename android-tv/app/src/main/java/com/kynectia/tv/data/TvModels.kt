package com.kynectia.tv.data

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val errors: List<String>? = null
)

@Serializable
data class TvSessionData(
    val sessionId: String,
    val pinCode: String,
    val qrPayload: String,
    val expiresAt: Long
)

@Serializable
data class TvSessionStatus(
    val sessionId: String,
    val pinCode: String,
    val isPaired: Boolean = false,
    val pairedAthleteName: String? = null,
    val state: TvLiveWorkoutState? = null,
    val rotateCount: Int = 0,
    val orientation: String? = null
)

@Serializable
data class TvLiveWorkoutState(
    val routineName: String,
    val exerciseName: String,
    val demoImageUrl: String? = null,
    val demoVideoUrl: String? = null,
    val currentExerciseIndex: Int = 0,
    val totalExercises: Int = 1,
    val currentSetNumber: Int = 1,
    val totalSets: Int = 3,
    val reps: Int = 10,
    val weightKg: Double = 0.0,
    val isResting: Boolean = false,
    val restSecondsRemaining: Int = 60,
    val restSecondsTotal: Int = 60,
    val elapsedWorkoutSeconds: Int = 0,
    val isPaused: Boolean = false,
    val isFinished: Boolean = false,
    val summary: TvWorkoutSummary? = null,
    val lastUpdated: Long = 0L
)

@Serializable
data class TvWorkoutSummary(
    val totalDurationSeconds: Int = 0,
    val totalSetsCompleted: Int = 0,
    val totalVolumeKg: Double = 0.0
)

@Serializable
data class TvCommandRequest(
    val sessionId: String,
    val command: String
)

@Serializable
data class TvCommandResponse(
    val command: String? = null,
    val success: Boolean = false
)
