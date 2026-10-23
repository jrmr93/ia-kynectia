package com.kynectia.app.data.api

import com.kynectia.app.data.model.*
import retrofit2.http.*

interface KynectiaApiService {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<LoginResponseData>

    @GET("auth/me")
    suspend fun getProfile(): ApiResponse<UserProfile>

    @GET("workouts")
    suspend fun getWorkouts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null,
        @Query("difficultyLevel") difficultyLevel: String? = null,
        @Query("category") category: String? = null
    ): ApiResponse<List<WorkoutRoutine>>

    @GET("workouts/{id}")
    suspend fun getWorkoutById(@Path("id") id: Int): ApiResponse<WorkoutRoutine>

    @GET("exercises")
    suspend fun getExercises(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
        @Query("search") search: String? = null
    ): ApiResponse<List<Exercise>>

    @GET("exercises/{id}")
    suspend fun getExerciseById(@Path("id") id: Int): ApiResponse<Exercise>

    @GET("muscles")
    suspend fun getMuscles(): ApiResponse<List<MuscleGroup>>

    @GET("equipment")
    suspend fun getEquipment(): ApiResponse<List<EquipmentItem>>

    @POST("tv/pair")
    suspend fun pairWithTv(@Body body: TvPairRequest): ApiResponse<TvPairResponse>

    @POST("tv/state")
    suspend fun updateTvState(@Body body: TvLiveWorkoutPayload): ApiResponse<Map<String, Boolean>>

    @POST("tv/disconnect/{sessionId}")
    suspend fun disconnectTv(@Path("sessionId") sessionId: String): ApiResponse<Map<String, Boolean>>

    @POST("tv/commands/consume/{sessionId}")
    suspend fun consumeTvCommands(@Path("sessionId") sessionId: String): ApiResponse<TvConsumeCommandsResponse>
}

