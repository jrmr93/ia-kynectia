package com.kynectia.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kynectia.app.data.api.SessionManager
import com.kynectia.app.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    sessionManager: SessionManager,
    onBack: () -> Unit,
    onLogout: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var urlInput by remember { mutableStateOf(sessionManager.baseUrl) }
    var connectionStatus by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }

    val userProfile = remember { sessionManager.getUserProfile() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AJUSTES Y CONEXIÓN",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = KynectiaLime
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Tarjeta de Perfil de Atleta
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(50.dp),
                            color = Color(0xFF1E3300),
                            shape = RoundedCornerShape(25.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = KynectiaLime)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = userProfile.fullName ?: "Atleta Kynectia",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = userProfile.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = DarkBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Objetivo:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(userProfile.fitnessGoal ?: "General", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = KynectiaLime)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Métricas:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("${userProfile.heightCm ?: 175.0} cm | ${userProfile.weightKg ?: 75.0} kg", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    }
                }
            }

            // Tarjeta de Conexión al Backend API
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔌 Conexión con Backend REST API",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Configura la URL base para comunicar la app móvil con el backend de Kynectia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("URL Base del API", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KynectiaLime,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Botones de presets rápidos
                    Text("Presets rápidos:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = urlInput == SessionManager.HOST_LAN_URL,
                            onClick = { urlInput = SessionManager.HOST_LAN_URL },
                            label = { Text("Wi-Fi (192.168.0.24)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KynectiaLime,
                                selectedLabelColor = Color(0xFF0D1117),
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                        FilterChip(
                            selected = urlInput == SessionManager.ADB_REVERSE_URL,
                            onClick = { urlInput = SessionManager.ADB_REVERSE_URL },
                            label = { Text("localhost:4000", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KynectiaLime,
                                selectedLabelColor = Color(0xFF0D1117),
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                sessionManager.baseUrl = urlInput.trim()
                                isTestingConnection = true
                                connectionStatus = null

                                coroutineScope.launch {
                                    val status = testHealthConnection(sessionManager.baseUrl)
                                    connectionStatus = status
                                    isTestingConnection = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = KynectiaLime, contentColor = Color(0xFF0D1117)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(color = Color(0xFF0D1117), modifier = Modifier.size(20.dp))
                            } else {
                                Text("Guardar y Probar Conexión", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (connectionStatus != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = if (connectionStatus!!.startsWith("✅")) Color(0x3310B981) else Color(0x33EF4444),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = connectionStatus!!,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (connectionStatus!!.startsWith("✅")) SuccessGreen else Color(0xFFFFB4A1)
                            )
                        }
                    }
                }
            }

            // Botón Cerrar Sesión
            OutlinedButton(
                onClick = {
                    sessionManager.clearSession()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FF6B6B))
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("CERRAR SESIÓN", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

suspend fun testHealthConnection(baseUrl: String): String = withContext(Dispatchers.IO) {
    try {
        val healthUrl = if (baseUrl.endsWith("/api/v1/")) {
            baseUrl.replace("/api/v1/", "/api/health")
        } else {
            "${baseUrl.trimEnd('/')}/health"
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(2500, TimeUnit.MILLISECONDS)
            .readTimeout(2500, TimeUnit.MILLISECONDS)
            .build()

        val request = Request.Builder().url(healthUrl).build()
        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            com.kynectia.app.data.repository.TrainingRepository.setOfflineMode(false)
            "✅ Conexión exitosa con el Backend de Kynectia (${response.code})"
        } else {
            com.kynectia.app.data.repository.TrainingRepository.setOfflineMode(true)
            "⚠️ Servidor respondió con código ${response.code}"
        }
    } catch (e: Exception) {
        com.kynectia.app.data.repository.TrainingRepository.setOfflineMode(true)
        "❌ Servidor no alcanzable: ${e.localizedMessage ?: "Error de conexión"}. La app continúa en modo local offline ultrarrápido."
    }
}
