package com.kynectia.app.ui.workouts.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import coil.compose.AsyncImage
import com.kynectia.app.data.model.WorkoutRoutine
import com.kynectia.app.data.repository.TrainingRepository
import com.kynectia.app.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditRoutineScreen(
    routineId: Int? = null,
    repository: TrainingRepository,
    onNavigateBack: () -> Unit,
    onRoutineSaved: (WorkoutRoutine) -> Unit
) {
    val viewModel = remember(routineId) {
        CreateEditRoutineViewModel(repository, routineId)
    }
    val state by viewModel.uiState.collectAsState()

    var showExitConfirmDialog by remember { mutableStateOf(false) }

    // Diálogo de confirmación para salir sin guardar
    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "¿Descartar cambios?",
                    color = TextPrimary,
                    fontFamily = Exo2,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Tienes cambios sin guardar en esta rutina personalizada. ¿Estás seguro de que deseas salir?",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KynectiaOrange, contentColor = Blanco)
                ) {
                    Text("Descartar y Salir", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text("Continuar editando", color = TextPrimary)
                }
            }
        )
    }

    // Modal BottomSheet del Catálogo de Ejercicios
    if (state.isPickerOpen) {
        ExercisePickerBottomSheet(
            exercises = state.catalogExercises,
            muscles = state.catalogMuscles,
            searchQuery = state.pickerSearchQuery,
            selectedMuscle = state.pickerMuscleFilter,
            addedExerciseIds = state.exercises.map { it.exercise_id },
            onSearchChange = { viewModel.setPickerSearchQuery(it) },
            onMuscleSelect = { viewModel.setPickerMuscleFilter(it) },
            onExerciseSelected = { exercise ->
                viewModel.addExercise(exercise)
            },
            onDismissRequest = { viewModel.closeExercisePicker() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (routineId == null || routineId == 0) "NUEVA RUTINA" else "EDITAR RUTINA",
                            fontFamily = Exo2,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = KynectiaLime,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Personaliza ejercicios, series y descansos",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.name.isNotBlank() || state.exercises.isNotEmpty()) {
                            showExitConfirmDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.saveRoutine { savedRoutine ->
                                onRoutineSaved(savedRoutine)
                            }
                        },
                        enabled = state.isValid && !state.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KynectiaLime,
                            contentColor = Color(0xFF0D1117),
                            disabledContainerColor = DarkBorder,
                            disabledContentColor = TextMuted
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF0D1117),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("GUARDAR", fontFamily = Exo2, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
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
            // 1. Live Stats Banner
            item {
                RoutineLiveStatsBanner(
                    durationMinutes = state.calculatedDurationMinutes,
                    exercisesCount = state.exercises.size,
                    totalSets = state.totalSets
                )
            }

            // 2. Información General de la Rutina
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "INFORMACIÓN GENERAL",
                            fontFamily = Exo2,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CianTech,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Nombre de la rutina
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = { viewModel.setName(it) },
                            label = { Text("Nombre de la rutina *", color = TextSecondary) },
                            placeholder = { Text("Ej. Torso Pesado, Pierna & Glúteos...", color = TextMuted) },
                            leadingIcon = { Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = KynectiaLime) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KynectiaLime,
                                unfocusedBorderColor = DarkBorder,
                                focusedContainerColor = DarkBackground,
                                unfocusedContainerColor = DarkBackground,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Objetivo / Descripción
                        OutlinedTextField(
                            value = state.description,
                            onValueChange = { viewModel.setDescription(it) },
                            label = { Text("Descripción u objetivo (opcional)", color = TextSecondary) },
                            placeholder = { Text("Ej. Hipertrofia y fuerza máxima enfocada en press plano", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KynectiaLime,
                                unfocusedBorderColor = DarkBorder,
                                focusedContainerColor = DarkBackground,
                                unfocusedContainerColor = DarkBackground,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Nivel de Dificultad
                        Text(
                            text = "Nivel de dificultad",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val difficulties = listOf(
                                "beginner" to "Principiante",
                                "intermediate" to "Intermedio",
                                "advanced" to "Avanzado"
                            )
                            difficulties.forEach { (key, label) ->
                                val isSelected = state.difficultyLevel.equals(key, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setDifficulty(key) },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = when (key) {
                                            "advanced" -> KynectiaOrange
                                            "intermediate" -> Color(0xFFF59E0B)
                                            else -> SuccessGreen
                                        },
                                        selectedLabelColor = Color.White,
                                        containerColor = DarkBackground,
                                        labelColor = TextSecondary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Categoría
                        Text(
                            text = "Categoría de entrenamiento",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val categories = listOf("Fuerza", "Hipertrofia", "Tren Superior", "Tren Inferior", "Full Body", "Funcional", "Cardio")
                            items(categories) { cat ->
                                val isSelected = state.category.equals(cat, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setCategory(cat) },
                                    label = { Text(cat) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AzulElectrico,
                                        selectedLabelColor = Blanco,
                                        containerColor = DarkBackground,
                                        labelColor = TextSecondary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Portada temática
                        Text(
                            text = "Imagen de portada",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val presetCovers = listOf(
                                "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=800" to "Pecho/Fuerza",
                                "https://images.unsplash.com/photo-1574680096145-d05b474e2155?w=800" to "Piernas",
                                "https://images.unsplash.com/photo-1598971639058-fab3c3109a00?w=800" to "Espalda",
                                "https://images.unsplash.com/photo-1583454110551-21f2fa2afe61?w=800" to "Hombros",
                                "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800" to "Calistenia"
                            )
                            items(presetCovers) { (url, label) ->
                                val isSelected = state.coverImageUrl == url
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.setCoverImage(url) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 70.dp, height = 48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) KynectiaLime else DarkBorder,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                    ) {
                                        AsyncImage(
                                            model = url,
                                            contentDescription = label,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        color = if (isSelected) KynectiaLime else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Sección de Ejercicios
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "EJERCICIOS DE LA RUTINA",
                            fontFamily = Exo2,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "${state.exercises.size} ejercicios • ${state.totalSets} series totales",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Button(
                        onClick = { viewModel.openExercisePicker() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AzulElectrico,
                            contentColor = Blanco
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AGREGAR",
                            fontFamily = Exo2,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Si está vacía la lista de ejercicios
            if (state.exercises.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(DarkBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = KynectiaLime,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Aún no has agregado ejercicios",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Explora el catálogo para agregar ejercicios y configurar tus series, repeticiones, pesos y tiempos de descanso.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { viewModel.openExercisePicker() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = KynectiaLime,
                                    contentColor = Color(0xFF0D1117)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Explorar Catálogo de Ejercicios",
                                    fontFamily = Exo2,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            } else {
                items(state.exercises.size) { index ->
                    val exercise = state.exercises[index]
                    RoutineExerciseItemCard(
                        index = index,
                        exercise = exercise,
                        isFirst = index == 0,
                        isLast = index == state.exercises.lastIndex,
                        onMoveUp = { viewModel.moveExerciseUp(index) },
                        onMoveDown = { viewModel.moveExerciseDown(index) },
                        onDelete = { viewModel.removeExercise(index) },
                        onSetsChange = { viewModel.updateSets(index, it) },
                        onRepsChange = { viewModel.updateReps(index, it) },
                        onWeightChange = { viewModel.updateWeight(index, it) },
                        onRestChange = { viewModel.updateRestSeconds(index, it) },
                        onDurationChange = { viewModel.updateDurationSeconds(index, it) },
                        onInstructionsChange = { viewModel.updateInstructions(index, it) }
                    )
                }

                item {
                    OutlinedButton(
                        onClick = { viewModel.openExercisePicker() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = KynectiaLime),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = androidx.compose.ui.graphics.SolidColor(KynectiaLime))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+ AGREGAR OTRO EJERCICIO",
                            fontFamily = Exo2,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Espacio inferior para scroll cómodo
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun RoutineLiveStatsBanner(
    durationMinutes: Int,
    exercisesCount: Int,
    totalSets: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Duración
            StatItem(
                icon = Icons.Default.Timer,
                iconTint = KynectiaLime,
                value = "$durationMinutes min",
                label = "Estimado"
            )

            Box(
                modifier = Modifier
                    .height(28.dp)
                    .width(1.dp)
                    .background(DarkBorder)
            )

            // Ejercicios
            StatItem(
                icon = Icons.Default.FitnessCenter,
                iconTint = CianTech,
                value = "$exercisesCount",
                label = "Ejercicios"
            )

            Box(
                modifier = Modifier
                    .height(28.dp)
                    .width(1.dp)
                    .background(DarkBorder)
            )

            // Series
            StatItem(
                icon = Icons.Default.Repeat,
                iconTint = AzulElectrico,
                value = "$totalSets",
                label = "Series Totales"
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    value: String,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                fontFamily = Exo2
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }
}
