package com.kynectia.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.kynectia.app.data.model.WorkoutSummaryResult
import com.kynectia.app.data.repository.TrainingRepository
import com.kynectia.app.ui.auth.LoginScreen
import com.kynectia.app.ui.common.OfflineModeIndicator
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

    val isOffline by TrainingRepository.isOfflineOrFallback.collectAsState()

    // Comprobación inicial no bloqueante de conectividad con el servidor backend
    LaunchedEffect(Unit) {
        repository.checkServerConnection()
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                            TrainingRepository.setOfflineMode(true)
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

        // Indicador discreto y no invasivo siempre presente en la parte superior cuando está en modo fallback / offline
        AnimatedVisibility(
            visible = isOffline,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            OfflineModeIndicator(
                onRetryConnection = {
                    repository.checkServerConnection()
                }
            )
        }
    }
}
