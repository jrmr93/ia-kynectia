package com.kynectia.app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Login : NavKey

@Serializable data object Main : NavKey

@Serializable data class Player(val routineId: Int) : NavKey

@Serializable data class Summary(
    val routineId: Int,
    val routineName: String,
    val totalDurationSeconds: Int,
    val totalSetsCompleted: Int,
    val totalVolumeKg: Double
) : NavKey
