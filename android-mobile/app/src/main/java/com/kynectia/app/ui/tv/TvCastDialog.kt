package com.kynectia.app.ui.tv

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kynectia.app.data.tv.TvCastManager
import com.kynectia.app.theme.*
import kotlinx.coroutines.launch

@Composable
fun TvCastDialog(
    athleteName: String,
    onDismissRequest: () -> Unit
) {
    val isConnected by TvCastManager.isTvConnected.collectAsState()
    val isConnecting by TvCastManager.isConnecting.collectAsState()
    val pairedPin by TvCastManager.pairedPin.collectAsState()
    val errorMessage by TvCastManager.errorMessage.collectAsState()

    var pinInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = PIN, 1 = QR
    var qrInput by remember { mutableStateOf("") }
    var isScannerOpen by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    // Modal de Escáner de Cámara QR a pantalla completa
    if (isScannerOpen) {
        Dialog(
            onDismissRequest = { isScannerOpen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            QrCameraScanner(
                onQrCodeScanned = { payload ->
                    isScannerOpen = false
                    qrInput = payload
                    coroutineScope.launch {
                        TvCastManager.pairWithQrPayload(context, payload, athleteName)
                    }
                },
                onClose = { isScannerOpen = false }
            )
        }
    } else {
        Dialog(onDismissRequest = onDismissRequest) {


        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header con Icono TV
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) Color(0xFF1B2E00) else DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Tv,
                        contentDescription = null,
                        tint = if (isConnected) KynectiaLime else TextSecondary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isConnected) "VINCULADO A PANTALLA TV" else "TRANSMITIR A ANDROID TV",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = if (isConnected) KynectiaLime else TextPrimary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isConnected)
                        "La rutina y los temporizadores se sincronizan en vivo en tu Android TV"
                    else
                        "Sigue tu rutina con temporizadores gigantes en tu televisor de sala o gimnasio",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (isConnected) {
                    // Estado Ya Conectado
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF132007),
                        border = androidx.compose.foundation.BorderStroke(1.dp, KynectiaLime.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(KynectiaLime)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "EN VIVO - PIN: ${pairedPin ?: "ACTIVO"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = KynectiaLime
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Atleta emisor: $athleteName",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón para rotar la pantalla de Android TV remotamente desde el móvil
                    Button(
                        onClick = {
                            TvCastManager.rotateTvScreen(context)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KynectiaLime,
                            contentColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.ScreenRotation,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = DarkBackground
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Girar Pantalla TV",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Alterna entre orientación horizontal (16:9) y vertical (9:16) en tu pantalla Android TV",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedButton(
                        onClick = {
                            TvCastManager.disconnect(context)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = KynectiaOrange),
                        border = androidx.compose.foundation.BorderStroke(1.dp, KynectiaOrange.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.TvOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Desconectar de Pantalla", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = onDismissRequest) {
                        Text("Continuar Entrenando", color = TextSecondary)
                    }
                } else {
                    // Pestañas PIN vs QR
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = DarkSurfaceVariant,
                        contentColor = KynectiaLime,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Código PIN", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                isScannerOpen = true
                            },
                            text = { Text("Código QR", fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (selectedTab == 0) {
                        // Formulario de Código PIN
                        Text(
                            text = "Ingresa los 6 dígitos que aparecen en tu TV:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = {
                                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                    pinInput = it
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Ej: 489215", color = TextMuted) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (pinInput.length == 6) {
                                        coroutineScope.launch {
                                            TvCastManager.pairWithPin(context, pinInput, athleteName)
                                        }
                                    }
                                }
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KynectiaLime,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant
                            ),
                            textStyle = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                letterSpacing = 6.sp
                            )
                        )
                    } else {
                        // Sección de Escaneo QR con Cámara
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isScannerOpen = true },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF162502)),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, KynectiaLime)
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(KynectiaLime),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Escanear QR con Cámara",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Apunta al código QR en la pantalla de la TV",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { isScannerOpen = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KynectiaLime,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Abrir Cámara para Escanear", fontWeight = FontWeight.Black)
                        }

                        if (qrInput.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DarkSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = KynectiaLime,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Código QR detectado y procesado",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }


                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF2C100A),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = KynectiaOrange,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón Vincular / Abrir Cámara
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (selectedTab == 1 && qrInput.isBlank()) {
                                isScannerOpen = true
                            } else {
                                coroutineScope.launch {
                                    if (selectedTab == 0) {
                                        TvCastManager.pairWithPin(context, pinInput, athleteName)
                                    } else {
                                        TvCastManager.pairWithQrPayload(context, qrInput, athleteName)
                                    }
                                }
                            }
                        },
                        enabled = !isConnecting && (
                            (selectedTab == 0 && pinInput.length == 6) ||
                            (selectedTab == 1)
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KynectiaLime,
                            contentColor = Color.Black,
                            disabledContainerColor = DarkSurfaceVariant,
                            disabledContentColor = TextMuted
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Vinculando con TV...", fontWeight = FontWeight.Bold)
                        } else if (selectedTab == 1 && qrInput.isBlank()) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Abrir Cámara para Escanear", fontWeight = FontWeight.Black)
                        } else {
                            Icon(Icons.Default.Cast, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vincular y Transmitir", fontWeight = FontWeight.Black)
                        }
                    }


                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = onDismissRequest) {
                        Text("Cancelar", color = TextSecondary)
                    }
                }
            }
        }
    }
}
}

