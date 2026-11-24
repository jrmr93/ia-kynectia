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

@Serializable
data class WorkoutRoutine(
    val id: Int,
    val name: String,
    val description: String? = null,
    val cover_image_url: String? = null,
    val goal: String = "Condición física general",
    val difficulty_level: String = "intermediate",
    val category: String = "Fuerza",
    val estimated_duration_minutes: Int = 45,
    val publication_status: String = "published",
    val version: Int = 1,
    val is_active: Boolean = true,
    val exercises: List<RoutineExerciseDetail> = emptyList(),
    val is_custom: Boolean = false
)

@Serializable
data class RoutineExerciseDetail(
    val id: Int = 0,
    val sequence_order: Int = 1,
    val sets: Int = 3,
    val reps: Int = 10,
    val duration_seconds: Int? = 0,
    val rest_seconds: Int = 60,
    val recommended_weight_kg: Double? = null,
    val specific_instructions: String? = null,
    val exercise_id: Int = 0,
    val exercise_name: String? = null,
    val difficulty_level: String? = null,
    val exercise_type: String? = null,
    val demo_image_url: String? = null,
    val demo_video_url: String? = null
)

@Serializable
data class Exercise(
    val id: Int,
    val name: String,
    val description: String? = null,
    val execution_instructions: String? = null,
    val difficulty_level: String = "beginner",
    val exercise_type: String = "strength",
    val demo_image_url: String? = null,
    val demo_video_url: String? = null,
    val safety_recommendations: String? = null,
    val primary_muscle_group_id: Int? = null,
    val primary_muscle_name: String? = null,
    val equipment_names: List<String> = emptyList()
)

@Serializable
data class MuscleGroup(
    val id: Int,
    val name: String,
    val description: String? = null,
    val image_url: String? = null
)

@Serializable
data class EquipmentItem(
    val id: Int,
    val name: String,
    val category: String? = null
)

@Serializable
data class CompletedSet(
    val setNumber: Int,
    val targetReps: Int,
    val completedReps: Int,
    val weightKg: Double,
    val isCompleted: Boolean = true
)

@Serializable
data class WorkoutSummaryResult(
    val routineId: Int,
    val routineName: String,
    val durationSeconds: Int,
    val totalSetsCompleted: Int,
    val totalVolumeKg: Double,
    val exerciseSummaries: List<ExerciseSummaryItem> = emptyList()
)

@Serializable
data class ExerciseSummaryItem(
    val exerciseId: Int,
    val exerciseName: String,
    val setsCompleted: Int,
    val topWeightKg: Double
)
