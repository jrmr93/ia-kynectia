package com.kynectia.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val errors: List<String>? = null,
    val meta: PaginationMeta? = null
)

@Serializable
data class PaginationMeta(
    val page: Int = 1,
    val limit: Int = 10,
    val total: Int = 0,
    val totalPages: Int = 1
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
    val exercises: List<RoutineExerciseDetail> = emptyList()
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
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponseData(
    val user: UserProfile? = null,
    val admin: UserProfile? = null,
    val token: String
)

@Serializable
data class UserProfile(
    val id: Int = 1,
    val email: String = "atleta@kynectia.com",
    val fullName: String? = "Atleta Kynectia",
    val phone: String? = null,
    val fitnessGoal: String? = "Hipertrofia y Resistencia",
    val heightCm: Double? = 178.0,
    val weightKg: Double? = 76.5
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
data class CompletedExerciseSummary(
    val exerciseId: Int,
    val exerciseName: String,
    val sets: List<CompletedSet>
)

@Serializable
data class WorkoutSummaryResult(
    val routineId: Int,
    val routineName: String,
    val totalDurationSeconds: Int,
    val totalSetsCompleted: Int,
    val totalVolumeKg: Double,
    val exercisesSummary: List<CompletedExerciseSummary>
)

@Serializable
data class TvPairRequest(
    val pinCode: String? = null,
    val sessionId: String? = null,
    val athleteName: String? = null,
    val athleteEmail: String? = null
)

@Serializable
data class TvPairResponse(
    val sessionId: String,
    val pinCode: String,
    val isPaired: Boolean,
    val pairedAthleteName: String? = null
)

@Serializable
data class TvLiveWorkoutPayload(
    val sessionId: String,
    val routineName: String,
    val exerciseName: String,
    val demoImageUrl: String? = null,
    val demoVideoUrl: String? = null,
    val currentExerciseIndex: Int,
    val totalExercises: Int,
    val currentSetNumber: Int,
    val totalSets: Int,
    val reps: Int,
    val weightKg: Double,
    val isResting: Boolean,
    val restSecondsRemaining: Int,
    val restSecondsTotal: Int,
    val elapsedWorkoutSeconds: Int,
    val isPaused: Boolean,
    val isFinished: Boolean,
    val summary: TvWorkoutSummaryPayload? = null
)

@Serializable
data class TvWorkoutSummaryPayload(
    val totalDurationSeconds: Int,
    val totalSetsCompleted: Int,
    val totalVolumeKg: Double
)

@Serializable
data class TvRemoteCommandItem(
    val id: String,
    val command: String,
    val timestamp: Long = 0L
)

@Serializable
data class TvConsumeCommandsResponse(
    val commands: List<TvRemoteCommandItem> = emptyList()
)

@Serializable
data class TvRotateRequest(
    val orientation: String? = null
)

@Serializable
data class TvRotateResponse(
    val rotateCount: Int = 0,
    val orientation: String? = null
)


