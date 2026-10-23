package com.kynectia.app.ui.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.kynectia.app.data.api.ApiClient
import com.kynectia.app.data.model.Exercise
import com.kynectia.app.theme.*
import com.kynectia.app.ui.player.ExerciseVideoPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    viewModel: ExercisesViewModel
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CATÁLOGO DE EJERCICIOS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = KynectiaLime,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Técnica, anatomía y recomendaciones",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground),
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recargar", tint = TextPrimary)
                    }
                }
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
                    placeholder = { Text("Buscar por nombre o músculo...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
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

            // Filtro por Músculo
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedMuscle == null,
                            onClick = { viewModel.onMuscleSelected(null) },
                            label = { Text("Todos") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KynectiaLime,
                                selectedLabelColor = Color(0xFF0D1117),
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = state.selectedMuscle == null,
                                borderColor = DarkBorder,
                                selectedBorderColor = KynectiaLime
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    items(state.muscles) { muscle ->
                        val isSelected = state.selectedMuscle == muscle.name
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onMuscleSelected(if (isSelected) null else muscle.name) },
                            label = { Text(muscle.name) },
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

            if (state.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = KynectiaLime)
                    }
                }
            } else if (state.exercises.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No se encontraron ejercicios.", color = TextSecondary)
                    }
                }
            } else {
                items(state.exercises) { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        onClick = { viewModel.showExerciseDetail(exercise) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Modal de Detalle de Ejercicio
        state.selectedExerciseDetail?.let { exercise ->
            ExerciseDetailDialog(
                exercise = exercise,
                onDismiss = { viewModel.dismissExerciseDetail() }
            )
        }
    }
}

@Composable
fun ExerciseCard(
    exercise: Exercise,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = LocalContext.current
            val resolvedVideoUrl = ApiClient.resolveMediaUrl(context, exercise.demo_video_url)
            val resolvedImageUrl = ApiClient.resolveMediaUrl(context, exercise.demo_image_url)
            val hasVideo = !resolvedVideoUrl.isNullOrBlank()

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!resolvedImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = resolvedImageUrl,
                        contentDescription = exercise.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = KynectiaLime.copy(alpha = 0.7f),
                        modifier = Modifier.size(36.dp)
                    )
                }

                if (hasVideo) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.75f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = KynectiaLime,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "VIDEO",
                                color = KynectiaLime,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                exercise.primary_muscle_name?.let { muscle ->
                    Text(
                        text = "Músculo: $muscle",
                        style = MaterialTheme.typography.bodySmall,
                        color = KynectiaLime
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = exercise.description ?: "Toca para ver instrucciones de ejecución.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2
                )
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
        }
    }
}

@Composable
fun ExerciseDetailDialog(
    exercise: Exercise,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val context = LocalContext.current
                val resolvedVideoUrl = ApiClient.resolveMediaUrl(context, exercise.demo_video_url)
                val resolvedImageUrl = ApiClient.resolveMediaUrl(context, exercise.demo_image_url)

                if (!resolvedVideoUrl.isNullOrBlank()) {
                    ExerciseVideoPlayer(
                        videoUrl = resolvedVideoUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                } else if (!resolvedImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = resolvedImageUrl,
                        contentDescription = exercise.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                exercise.primary_muscle_name?.let {
                    Text(
                        text = "🎯 Músculo Principal: $it",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = KynectiaLime
                    )
                }

                if (exercise.equipment_names.isNotEmpty()) {
                    Text(
                        text = "🏋️ Equipamiento: ${exercise.equipment_names.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                HorizontalDivider(color = DarkBorder)

                Text(
                    text = "Instrucciones de Ejecución:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = exercise.execution_instructions ?: "Sigue el rango de movimiento controlado respetando la técnica biomecánica adecuada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                if (!exercise.safety_recommendations.isNullOrBlank()) {
                    Surface(
                        color = Color(0x33FF5E3A),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ Seguridad: ${exercise.safety_recommendations}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFB4A1),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = KynectiaLime, contentColor = Color(0xFF0D1117)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("CERRAR", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
