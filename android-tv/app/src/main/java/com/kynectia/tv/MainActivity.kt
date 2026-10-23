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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kynectia.tv.theme.DarkBackground
import com.kynectia.tv.theme.KynectiaLime
import com.kynectia.tv.theme.KynectiaOrange
import com.kynectia.tv.theme.KynectiaTvTheme
import com.kynectia.tv.theme.TextPrimary
import com.kynectia.tv.theme.TextSecondary
import com.kynectia.tv.ui.TvDisplayOrientation
import com.kynectia.tv.ui.TvUiState
import com.kynectia.tv.ui.TvViewModel
import com.kynectia.tv.ui.pairing.TvPairingScreen
import com.kynectia.tv.ui.connected.TvConnectedScreen
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
                        // Invertimos las dimensiones desvinculándolas de las restricciones del padre para que el layout vertical 9:16 ocupe el ancho y alto del TV al 100%
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
                                is TvUiState.Pairing -> {
                                    TvPairingScreen(
                                        pinCode = state.pinCode,
                                        qrPayload = state.qrPayload,
                                        isLoading = false,
                                        orientationMode = orientationMode,
                                        isSoundEnabled = isSoundEnabled,
                                        onRefreshCode = { viewModel.createTvSession() },
                                        onRotateScreen = { viewModel.cycleOrientation(this@MainActivity) },
                                        onToggleSound = { viewModel.toggleSound() }
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
                                        onRotateScreen = { viewModel.cycleOrientation(this@MainActivity) },
                                        onToggleSound = { viewModel.toggleSound() },
                                        onTogglePause = { viewModel.sendRemoteCommand("TOGGLE_PAUSE") },
                                        onSkipRest = { viewModel.sendRemoteCommand("SKIP_REST") },
                                        onNextExercise = { viewModel.sendRemoteCommand("NEXT_EXERCISE") },
                                        onPrevExercise = { viewModel.sendRemoteCommand("PREV_EXERCISE") },
                                        onDisconnect = { viewModel.disconnect() }
                                    )
                                }
                                is TvUiState.Summary -> {
                                    TvSummaryScreen(
                                        athleteName = state.athleteName,
                                        routineName = state.routineName,
                                        summary = state.summary,
                                        orientationMode = orientationMode,
                                        onReturnToPairing = { viewModel.createTvSession() }
                                    )
                                }
                                is TvUiState.Error -> {
                                    TvErrorScreen(
                                        message = state.message,
                                        onRetry = { viewModel.createTvSession() }
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
        if (event.action == KeyEvent.ACTION_DOWN) {
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

                // Atajos multimedia de control de reproducción
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    viewModel.handleDpadCenter()
                    return true
                }

                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    viewModel.sendRemoteCommand("TOGGLE_PAUSE")
                    return true
                }

                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    viewModel.sendRemoteCommand("TOGGLE_PAUSE")
                    return true
                }

                KeyEvent.KEYCODE_MEDIA_NEXT,
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                KeyEvent.KEYCODE_PAGE_DOWN -> {
                    viewModel.handleDpadRight()
                    return true
                }

                KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                KeyEvent.KEYCODE_MEDIA_REWIND,
                KeyEvent.KEYCODE_PAGE_UP -> {
                    viewModel.handleDpadLeft()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                viewModel.handleDpadCenter()
                return true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                viewModel.handleDpadRight()
                return true
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                viewModel.handleDpadLeft()
                return true
            }
        }
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
    onRetry: () -> Unit
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
        }
    }
}
