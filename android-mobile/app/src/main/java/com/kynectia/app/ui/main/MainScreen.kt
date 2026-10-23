package com.kynectia.app.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import com.kynectia.app.Player
import com.kynectia.app.data.repository.TrainingRepository
import com.kynectia.app.theme.*
import com.kynectia.app.ui.exercises.ExercisesScreen
import com.kynectia.app.ui.exercises.ExercisesViewModel
import com.kynectia.app.ui.settings.SettingsScreen
import com.kynectia.app.ui.workouts.WorkoutsScreen
import com.kynectia.app.ui.workouts.WorkoutsViewModel

enum class MainTab {
    WORKOUTS,
    EXERCISES,
    SETTINGS
}

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { TrainingRepository(context) }
    val workoutsViewModel = remember { WorkoutsViewModel(repository) }
    val exercisesViewModel = remember { ExercisesViewModel(repository) }

    var selectedTab by remember { mutableStateOf(MainTab.WORKOUTS) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                contentColor = TextPrimary
            ) {
                NavigationBarItem(
                    selected = selectedTab == MainTab.WORKOUTS,
                    onClick = { selectedTab = MainTab.WORKOUTS },
                    icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Rutinas") },
                    label = { Text("Rutinas") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DarkBackground,
                        selectedTextColor = KynectiaLime,
                        indicatorColor = KynectiaLime,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == MainTab.EXERCISES,
                    onClick = { selectedTab = MainTab.EXERCISES },
                    icon = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Ejercicios") },
                    label = { Text("Ejercicios") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DarkBackground,
                        selectedTextColor = KynectiaLime,
                        indicatorColor = KynectiaLime,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == MainTab.SETTINGS,
                    onClick = { selectedTab = MainTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                    label = { Text("Ajustes") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DarkBackground,
                        selectedTextColor = KynectiaLime,
                        indicatorColor = KynectiaLime,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                MainTab.WORKOUTS -> {
                    WorkoutsScreen(
                        viewModel = workoutsViewModel,
                        onStartWorkout = { routineId ->
                            onItemClick(Player(routineId))
                        }
                    )
                }
                MainTab.EXERCISES -> {
                    ExercisesScreen(viewModel = exercisesViewModel)
                }
                MainTab.SETTINGS -> {
                    SettingsScreen(
                        sessionManager = repository.getSessionManager(),
                        onBack = { selectedTab = MainTab.WORKOUTS },
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}
