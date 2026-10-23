package com.kynectia.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.kynectia.app.data.model.WorkoutSummaryResult
import com.kynectia.app.data.repository.TrainingRepository
import com.kynectia.app.ui.auth.LoginScreen
import com.kynectia.app.ui.main.MainScreen
import com.kynectia.app.ui.player.WorkoutPlayerScreen
import com.kynectia.app.ui.player.WorkoutPlayerViewModel
import com.kynectia.app.ui.summary.WorkoutSummaryScreen

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val repository = remember { TrainingRepository(context) }
    val sessionManager = remember { repository.getSessionManager() }

    val initialDestination = if (sessionManager.isLoggedIn) Main else Login
    val backStack = rememberNavBackStack(initialDestination)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Login> {
                LoginScreen(
                    repository = repository,
                    onLoginSuccess = {
                        while (backStack.isNotEmpty()) {
                            backStack.removeLastOrNull()
                        }
                        backStack.add(Main)
                    },
                    onContinueOffline = {
                        while (backStack.isNotEmpty()) {
                            backStack.removeLastOrNull()
                        }
                        backStack.add(Main)
                    }
                )
            }

            entry<Main> {
                MainScreen(
                    onItemClick = { navKey -> backStack.add(navKey) },
                    onLogout = {
                        while (backStack.isNotEmpty()) {
                            backStack.removeLastOrNull()
                        }
                        backStack.add(Login)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            entry<Player> { key ->
                val playerViewModel = remember(key.routineId) {
                    WorkoutPlayerViewModel(repository, context).apply {
                        loadRoutine(key.routineId)
                    }
                }

                WorkoutPlayerScreen(
                    viewModel = playerViewModel,
                    onWorkoutFinished = { result ->
                        backStack.removeLastOrNull()
                        backStack.add(
                            Summary(
                                routineId = result.routineId,
                                routineName = result.routineName,
                                totalDurationSeconds = result.totalDurationSeconds,
                                totalSetsCompleted = result.totalSetsCompleted,
                                totalVolumeKg = result.totalVolumeKg
                            )
                        )
                    },
                    onBack = { backStack.removeLastOrNull() }
                )
            }

            entry<Summary> { key ->
                WorkoutSummaryScreen(
                    summary = WorkoutSummaryResult(
                        routineId = key.routineId,
                        routineName = key.routineName,
                        totalDurationSeconds = key.totalDurationSeconds,
                        totalSetsCompleted = key.totalSetsCompleted,
                        totalVolumeKg = key.totalVolumeKg,
                        exercisesSummary = emptyList()
                    ),
                    onFinish = {
                        while (backStack.size > 1) {
                            backStack.removeLastOrNull()
                        }
                    }
                )
            }
        }
    )
}
