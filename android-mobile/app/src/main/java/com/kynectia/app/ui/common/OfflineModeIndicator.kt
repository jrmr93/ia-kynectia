package com.kynectia.app.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kynectia.app.data.api.SessionManager
import com.kynectia.app.theme.*
import kotlinx.coroutines.launch

/**
 * Indicador visual discreto y no invasivo que señala cuándo la app está operando
 * en modo local / offline / fallback sin conexión al servidor backend.
 */
@Composable
fun OfflineModeIndicator(
    onRetryConnection: suspend () -> Boolean,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    // Animación suave de pulsación ámbar
    val infiniteTransition = rememberInfiniteTransition(label = "offline_dot_pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Píldora compacta, translúcida y no invasiva
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { showDialog = true },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xCC13171F),
        border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.6f)),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Icono de nube desconectada
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = "Modo Offline",
                tint = WarningAmber,
                modifier = Modifier.size(13.dp)
            )

            // Punto ámbar con pulsación sutil
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(WarningAmber.copy(alpha = dotAlpha))
            )

            // Etiqueta compacta
            Text(
                text = "Offline",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = WarningAmber,
                fontSize = 11.sp,
                letterSpacing = 0.3.sp
            )
        }
    }

    if (showDialog) {
        OfflineInfoDialog(
            onDismiss = { showDialog = false },
            onRetryConnection = onRetryConnection
        )
    }
}

@Composable
fun OfflineInfoDialog(
    onDismiss: () -> Unit,
    onRetryConnection: suspend () -> Boolean
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var connectionResult by remember { mutableStateOf<Boolean?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color(0x33F59E0B),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Modo Local / Fallback",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Sin conexión con el servidor",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningAmber,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "La app está funcionando con los datos cacheados en tu dispositivo. Puedes realizar tus entrenamientos, registrar tus series y navegar rutinas con total normalidad.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Servidor configurado:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = sessionManager.baseUrl,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1
                        )
                    }
                }

                if (connectionResult != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (connectionResult == true) Color(0x3310B981) else Color(0x33EF4444),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (connectionResult == true) Icons.Default.CheckCircle else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (connectionResult == true) SuccessGreen else ErrorRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (connectionResult == true) "¡Conexión restablecida con éxito!" else "El servidor sigue sin responder.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (connectionResult == true) SuccessGreen else Color(0xFFFFB4A1)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isChecking = true
                    connectionResult = null
                    scope.launch {
                        val connected = onRetryConnection()
                        isChecking = false
                        connectionResult = connected
                        if (connected) {
                            kotlinx.coroutines.delay(1000)
                            onDismiss()
                        }
                    }
                },
                enabled = !isChecking,
                colors = ButtonDefaults.buttonColors(containerColor = AzulElectrico, contentColor = Blanco),
                shape = RoundedCornerShape(20.dp)
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        color = Blanco,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Comprobando...", fontSize = 12.sp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reintentar conexión", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}
