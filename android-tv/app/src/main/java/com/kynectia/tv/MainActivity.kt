package com.kynectia.tv

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kynectia.tv.theme.*
import com.kynectia.tv.ui.TvDisplayOrientation
import com.kynectia.tv.ui.TvUiState
import com.kynectia.tv.ui.TvViewModel
import com.kynectia.tv.ui.catalog.TvRoutinesCatalogScreen
import com.kynectia.tv.ui.connected.TvConnectedScreen
import com.kynectia.tv.ui.pairing.TvPairingScreen
import com.kynectia.tv.ui.summary.TvSummaryScreen
import com.kynectia.tv.ui.workout.TvLiveWorkoutScreen

class MainActivity : ComponentActivity() {

    private val viewModel: TvViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KynectiaTvTheme {
                val uiState by viewModel.uiState.collectAsState()
                val orientationMode by viewModel.orientationMode.collectAsState()
                val isSoundEnabled by viewModel.isSoundEnabled.collectAsState()

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground)
                ) {
                    val isRotatedSoftware = orientationMode == TvDisplayOrientation.VERTICAL_90 ||
                            orientationMode == TvDisplayOrientation.VERTICAL_270

                    val contentModifier = if (isRotatedSoftware) {
                        Modifier
                            .align(Alignment.Center)
                            .requiredSize(width = maxHeight, height = maxWidth)
                            .graphicsLayer {
                                rotationZ = orientationMode.rotationDegrees
                            }
                    } else {
                        Modifier.fillMaxSize()
                    }

                    Box(modifier = contentModifier) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = DarkBackground
                        ) {
                            when (val state = uiState) {
                                is TvUiState.Loading -> {
                                    TvLoadingScreen()
                                }
                                is TvUiState.RoutinesCatalog -> {
                                    TvRoutinesCatalogScreen(
                                        routines = state.routines,
                                        selectedCategory = state.selectedCategory,
                                        selectedDifficulty = state.selectedDifficulty,
                                        isOffline = state.isOffline,
                                        orientationMode = orientationMode,
                                        isSoundEnabled = isSoundEnabled,
                                        onFilterChange = { cat, diff ->
                                            viewModel.openRoutinesCatalog(category = cat, difficulty = diff)
                                        },
                                        onStartRoutine = { routine ->
                                            viewModel.startStandaloneWorkout(routine)
                                        },
                                        onOpenPairing = {
                                            viewModel.forceNewTvSession()
                                        },
                                        onRotateScreen = { viewModel.cycleOrientation(this@MainActivity) },
                                        onToggleSound = { viewModel.toggleSound() }
                                    )
                                }
                                is TvUiState.Pairing -> {
                                    TvPairingScreen(
                                        pinCode = state.pinCode,
                                        qrPayload = state.qrPayload,
                                        isLoading = false,
                                        orientationMode = orientationMode,
                                        isSoundEnabled = isSoundEnabled,
                                        onRefreshCode = { viewModel.forceNewTvSession() },
                                        onRotateScreen = { viewModel.cycleOrientation(this@MainActivity) },
                                        onToggleSound = { viewModel.toggleSound() },
                                        onOpenCatalog = { viewModel.openRoutinesCatalog() }
                                    )
                                }
                                is TvUiState.Connected -> {
                                    TvConnectedScreen(
                                        athleteName = state.athleteName,
                                        pinCode = state.pinCode,
                                        orientationMode = orientationMode,
                                        isSoundEnabled = isSoundEnabled,
                                        onRotateScreen = { viewModel.cycleOrientation(this@MainActivity) },
                                        onToggleSound = { viewModel.toggleSound() },
                                        onDisconnect = { viewModel.disconnect() }
                                    )
                                }
                                is TvUiState.LiveWorkout -> {
                                    TvLiveWorkoutScreen(
                                        athleteName = state.athleteName,
                                        state = state.state,
                                        orientationMode = orientationMode,
                                        isSoundEnabled = isSoundEnabled,
                                        isStandalone = state.isStandalone,
                                        onRotateScreen = { viewModel.cycleOrientation(this@MainActivity) },
                                        onToggleSound = { viewModel.toggleSound() },
                                        onTogglePause = { viewModel.toggleWorkoutPause() },
                                        onSkipRest = {
                                            if (state.isStandalone) viewModel.skipStandaloneRest() else viewModel.sendRemoteCommand("SKIP_REST")
                                        },
                                        onNextExercise = { viewModel.nextExercise() },
                                        onPrevExercise = { viewModel.previousExercise() },
                                        onDisconnect = {
                                            if (state.isStandalone) viewModel.exitWorkoutToCatalog() else viewModel.disconnect()
                                        },
                                        onLogSet = { viewModel.logStandaloneSet() }
                                    )
                                }
                                is TvUiState.Summary -> {
                                    TvSummaryScreen(
                                        athleteName = state.athleteName,
                                        routineName = state.routineName,
                                        summary = state.summary,
                                        orientationMode = orientationMode,
                                        isStandalone = state.isStandalone,
                                        onReturnToPairing = {
                                            if (state.isStandalone) {
                                                viewModel.openRoutinesCatalog()
                                            } else {
                                                viewModel.disconnect()
                                            }
                                        }
                                    )
                                }
                                is TvUiState.Error -> {
                                    TvErrorScreen(
                                        message = state.message,
                                        onRetry = { viewModel.restoreOrInitSession() },
                                        onContinueOffline = { viewModel.openRoutinesCatalog() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Captura prioritaria de eventos del control remoto antes de que cualquier vista secundaria los consuma
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        android.util.Log.d("KynectiaKey", "dispatchKeyEvent: action=${event.action}, keyCode=${event.keyCode}, state=${viewModel.uiState.value::class.simpleName}")
        if (event.action == KeyEvent.ACTION_DOWN) {
            val current = viewModel.uiState.value

            when (event.keyCode) {
                // Atajos de rotación de pantalla
                KeyEvent.KEYCODE_MENU,
                KeyEvent.KEYCODE_PROG_YELLOW,
                KeyEvent.KEYCODE_BUTTON_Y,
                KeyEvent.KEYCODE_1 -> {
                    viewModel.cycleOrientation(this)
                    return true
                }

                // Atajos de sonido
                KeyEvent.KEYCODE_PROG_GREEN,
                KeyEvent.KEYCODE_VOLUME_MUTE,
                KeyEvent.KEYCODE_2 -> {
                    viewModel.toggleSound()
                    return true
                }

                // Atajo directo para abrir catálogo de rutinas
                KeyEvent.KEYCODE_PROG_RED,
                KeyEvent.KEYCODE_C,
                KeyEvent.KEYCODE_3 -> {
                    if (current is TvUiState.Pairing || current is TvUiState.Connected || current is TvUiState.Error) {
                        viewModel.openRoutinesCatalog()
                        return true
                    }
                }
            }

            if (current is TvUiState.RoutinesCatalog) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                    KeyEvent.KEYCODE_MEDIA_PLAY -> {
                        val routine = current.routines.firstOrNull()
                        if (routine != null) {
                            viewModel.startStandaloneWorkout(routine)
                            return true
                        }
                    }
                }
            }

            if (current is TvUiState.LiveWorkout) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER,
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        viewModel.handleDpadCenter()
                        return true
                    }

                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (viewModel.handleDpadUp()) return true
                    }

                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (viewModel.handleDpadDown()) return true
                    }

                    KeyEvent.KEYCODE_DPAD_RIGHT,
                    KeyEvent.KEYCODE_MEDIA_NEXT,
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                    KeyEvent.KEYCODE_PAGE_DOWN -> {
                        viewModel.handleDpadRight()
                        return true
                    }

                    KeyEvent.KEYCODE_DPAD_LEFT,
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                    KeyEvent.KEYCODE_MEDIA_REWIND,
                    KeyEvent.KEYCODE_PAGE_UP -> {
                        viewModel.handleDpadLeft()
                        return true
                    }

                    KeyEvent.KEYCODE_MEDIA_PLAY,
                    KeyEvent.KEYCODE_MEDIA_PAUSE,
                    KeyEvent.KEYCODE_P -> {
                        viewModel.toggleWorkoutPause()
                        return true
                    }

                    KeyEvent.KEYCODE_BACK,
                    KeyEvent.KEYCODE_ESCAPE -> {
                        if (current.isStandalone) {
                            viewModel.exitWorkoutToCatalog()
                        } else {
                            viewModel.disconnect()
                        }
                        return true
                    }
                }
            } else if (current is TvUiState.Summary) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_BACK,
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        if (current.isStandalone) {
                            viewModel.openRoutinesCatalog()
                        } else {
                            viewModel.disconnect()
                        }
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
private fun TvLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = KynectiaLime,
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(
                color = KynectiaLime,
                strokeWidth = 4.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "INICIALIZANDO KYNECTIA TV...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = TextSecondary,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
private fun TvErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onContinueOffline: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = KynectiaOrange,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ERROR DE CONEXIÓN",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.6f)
            )
            Spacer(modifier = Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KynectiaLime,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reintentar Conexión", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onContinueOffline,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurfaceVariant,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(20.dp), tint = CianTech)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Modo Sin Conexión", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
