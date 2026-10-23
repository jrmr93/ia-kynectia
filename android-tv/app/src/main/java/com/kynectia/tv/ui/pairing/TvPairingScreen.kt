package com.kynectia.tv.ui.pairing

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.kynectia.tv.theme.*
import com.kynectia.tv.ui.TvDisplayOrientation
import com.kynectia.tv.ui.workout.TvRemoteActionButton

@Composable
fun TvPairingScreen(
    pinCode: String,
    qrPayload: String,
    isLoading: Boolean,
    orientationMode: TvDisplayOrientation,
    isSoundEnabled: Boolean,
    onRefreshCode: () -> Unit,
    onRotateScreen: () -> Unit,
    onToggleSound: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val qrBitmap = remember(qrPayload) {
        generateRealQrBitmap(qrPayload, 512)
    }

    if (orientationMode.isVerticalLayout) {
        // DISEÑO VERTICAL PARA VINCULACIÓN
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        color = Color(0xFF1E3300),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Tv, contentDescription = null, tint = KynectiaLime, modifier = Modifier.size(26.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("KYNECTIA TV", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = KynectiaLime)
                        Text("MODO SALA VERTICAL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextSecondary)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvRemoteActionButton(
                        icon = Icons.Default.ScreenRotation,
                        label = "Girar",
                        shortcutKey = "MENU",
                        onClick = onRotateScreen
                    )
                    TvRemoteActionButton(
                        icon = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        label = if (isSoundEnabled) "Audio" else "Mudo",
                        shortcutKey = "2",
                        onClick = onToggleSound
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // PIN y QR en el centro ocupando el espacio principal
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = "CÓDIGO PIN DE VINCULACIÓN",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = KynectiaLime,
                        letterSpacing = 1.5.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val digits = if (pinCode.isNotBlank()) pinCode.toCharArray() else "------".toCharArray()
                        digits.forEach { char ->
                            Box(
                                modifier = Modifier
                                    .size(width = 46.dp, height = 64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurfaceVariant)
                                    .border(2.dp, KynectiaLime, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char.toString(),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "QR de Vinculación",
                                modifier = Modifier.fillMaxSize(),
                                filterQuality = FilterQuality.None
                            )
                        } else {
                            CircularProgressIndicator(color = Color(0xFF090D12), modifier = Modifier.size(36.dp))
                        }
                    }

                    Text(
                        text = "Escanea el código QR con la app móvil para iniciar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Indicador inferior
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(KynectiaLime.copy(alpha = alphaAnim))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Esperando que inicies entrenamiento en el móvil...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                TvRemoteActionButton(
                    icon = Icons.Default.Refresh,
                    label = "Generar nuevo PIN",
                    shortcutKey = "OK",
                    onClick = onRefreshCode
                )
            }
        }
    } else {
        // DISEÑO HORIZONTAL ESTÁNDAR
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(40.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Columna Izquierda
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(54.dp),
                        color = Color(0xFF1E3300),
                        shape = RoundedCornerShape(27.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Tv,
                                contentDescription = null,
                                tint = KynectiaLime,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "KYNECTIA TV",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = KynectiaLime,
                            letterSpacing = 3.sp
                        )
                        Text(
                            text = "MODO SALA & GIMNASIO INTELIGENTE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Column {
                    Text(
                        text = "Entrena en Pantalla Gigante",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "1. Abre la app Kynectia en tu móvil.\n" +
                               "2. Inicia tu entrenamiento y presiona 'Transmitir a TV'.\n" +
                               "3. Escanea el código QR o ingresa el PIN de 6 dígitos.\n" +
                               "4. Controla la rutina con el control remoto de tu TV.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        lineHeight = 26.sp
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(KynectiaLime.copy(alpha = alphaAnim))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Esperando vinculación desde el móvil...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvRemoteActionButton(
                            icon = Icons.Default.Refresh,
                            label = "Nuevo Código",
                            shortcutKey = "OK",
                            onClick = onRefreshCode
                        )

                        TvRemoteActionButton(
                            icon = Icons.Default.ScreenRotation,
                            label = "Girar Pantalla",
                            shortcutKey = "MENU",
                            onClick = onRotateScreen
                        )

                        TvRemoteActionButton(
                            icon = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            label = if (isSoundEnabled) "Audio ON" else "Audio OFF",
                            shortcutKey = "2",
                            onClick = onToggleSound
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(36.dp))

            // Columna Derecha: Tarjeta de PIN y QR
            Card(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(26.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "CÓDIGO PIN DE VINCULACIÓN",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = KynectiaLime,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val digits = if (pinCode.isNotBlank()) pinCode.toCharArray() else "------".toCharArray()
                        digits.forEach { char ->
                            Box(
                                modifier = Modifier
                                    .size(width = 46.dp, height = 62.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurfaceVariant)
                                    .border(2.dp, KynectiaLime, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char.toString(),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Código QR de Vinculación",
                                modifier = Modifier.fillMaxSize(),
                                filterQuality = FilterQuality.None
                            )
                        } else {
                            CircularProgressIndicator(
                                color = Color(0xFF090D12),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Apunta la cámara de tu móvil para escanear",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun generateRealQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
    if (content.isBlank()) return null
    return try {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 1
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        bitmap
    } catch (_: Exception) {
        null
    }
}
