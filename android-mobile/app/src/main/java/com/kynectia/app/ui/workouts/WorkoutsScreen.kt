package com.kynectia.app.ui.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kynectia.app.data.model.WorkoutRoutine
import com.kynectia.app.data.tv.TvCastManager
import com.kynectia.app.theme.*
import com.kynectia.app.ui.tv.TvCastDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
    viewModel: WorkoutsViewModel,
    onStartWorkout: (Int) -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val isTvConnected by TvCastManager.isTvConnected.collectAsState()
    var showTvCastDialog by remember { mutableStateOf(false) }

    if (showTvCastDialog) {
        TvCastDialog(
            athleteName = "Atleta Kynectia",
            onDismissRequest = { showTvCastDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "KYNECTIA",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = KynectiaLime,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Planes de Entrenamiento y Rutinas",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    if (isTvConnected) {
                        IconButton(onClick = { TvCastManager.rotateTvScreen(context) }) {
                            Icon(
                                Icons.Default.ScreenRotation,
                                contentDescription = "Girar Pantalla TV",
                                tint = KynectiaLime
                            )
                        }
                    }

                    IconButton(onClick = { showTvCastDialog = true }) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Tv,
                                contentDescription = "Transmitir a TV",
                                tint = if (isTvConnected) KynectiaLime else TextSecondary
                            )
                            if (isTvConnected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(KynectiaLime)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }

                    IconButton(onClick = { viewModel.loadRoutines() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recargar", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },

        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Buscador
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Buscar rutina o músculo...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = TextSecondary)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KynectiaLime,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
            }

            // Chips de filtro de dificultad
            item {
                val difficulties = listOf(
                    "Todos" to null,
                    "Principiante" to "beginner",
                    "Intermedio" to "intermediate",
                    "Avanzado" to "advanced"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(difficulties) { (label, value) ->
                        val isSelected = state.selectedDifficulty == value
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onDifficultySelected(value) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KynectiaLime,
                                selectedLabelColor = Color(0xFF0D1117),
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = DarkBorder,
                                selectedBorderColor = KynectiaLime
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Estado de carga o lista vacía
            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = KynectiaLime)
                    }
                }
            } else if (state.routines.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron rutinas disponibles.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(state.routines) { routine ->
                    WorkoutRoutineCard(
                        routine = routine,
                        onStartClick = { onStartWorkout(routine.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun WorkoutRoutineCard(
    routine: WorkoutRoutine,
    onStartClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onStartClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // Portada con gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                if (!routine.cover_image_url.isNullOrBlank()) {
                    AsyncImage(
                        model = routine.cover_image_url,
                        contentDescription = routine.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF1E293B), DarkSurface)
                                )
                            )
                    )
                }

                // Overlay oscuro para legibilidad
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xCC0D1117))
                            )
                        )
                )

                // Badge de Dificultad
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd),
                    color = when (routine.difficulty_level.lowercase()) {
                        "advanced" -> KynectiaOrange
                        "intermediate" -> Color(0xFFF59E0B)
                        else -> SuccessGreen
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = routine.difficulty_level.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Información de la rutina
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = routine.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                if (!routine.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = routine.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = KynectiaLime,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${routine.estimated_duration_minutes} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = KynectiaLime,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${routine.exercises.size} ejercicios",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = onStartClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KynectiaLime,
                            contentColor = Color(0xFF0D1117)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "ENTRENAR",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
