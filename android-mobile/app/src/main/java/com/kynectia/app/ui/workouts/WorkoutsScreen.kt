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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kynectia.app.data.model.WorkoutRoutine
import com.kynectia.app.data.tv.TvCastManager
import com.kynectia.app.theme.*
import com.kynectia.app.ui.common.KynectiaBrandLogo
import com.kynectia.app.ui.tv.TvCastDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
    viewModel: WorkoutsViewModel,
    onStartWorkout: (Int) -> Unit,
    onCreateRoutine: () -> Unit = {},
    onEditRoutine: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val isTvConnected by TvCastManager.isTvConnected.collectAsState()
    var showTvCastDialog by remember { mutableStateOf(false) }
    var routineToDelete by remember { mutableStateOf<WorkoutRoutine?>(null) }

    // Diálogo para transmitir a TV
    if (showTvCastDialog) {
        TvCastDialog(
            athleteName = "Atleta Kynectia",
            onDismissRequest = { showTvCastDialog = false }
        )
    }

    // Diálogo de confirmación para eliminar rutina personalizada
    if (routineToDelete != null) {
        AlertDialog(
            onDismissRequest = { routineToDelete = null },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "¿Eliminar rutina?",
                    fontFamily = Exo2,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas eliminar permanentemente la rutina '${routineToDelete?.name}'?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        routineToDelete?.let { viewModel.deleteRoutine(it.id) }
                        routineToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KynectiaOrange, contentColor = Blanco)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { routineToDelete = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    KynectiaBrandLogo(
                        size = 32.dp,
                        showText = true,
                        showTagline = true,
                        horizontal = true
                    )
                },
                actions = {
                    if (isTvConnected) {
                        IconButton(onClick = { TvCastManager.rotateTvScreen(context) }) {
                            Icon(
                                Icons.Default.ScreenRotation,
                                contentDescription = "Girar Pantalla TV",
                                tint = CianTech
                            )
                        }
                    }

                    IconButton(onClick = { showTvCastDialog = true }) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Tv,
                                contentDescription = "Transmitir a TV",
                                tint = if (isTvConnected) CianTech else TextSecondary
                            )
                            if (isTvConnected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(CianTech)
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateRoutine,
                containerColor = KynectiaLime,
                contentColor = Color(0xFF0D1117),
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = {
                    Text(
                        text = "CREAR RUTINA",
                        fontFamily = Exo2,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
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

            // Chips de filtro: Todas, Mis Rutinas, Dificultades
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Todas
                    item {
                        val isAllSelected = !state.showOnlyCustom && state.selectedDifficulty == null
                        FilterChip(
                            selected = isAllSelected,
                            onClick = { viewModel.onShowAllSelected() },
                            label = { Text("Todas") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KynectiaLime,
                                selectedLabelColor = Color(0xFF0D1117),
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isAllSelected,
                                borderColor = DarkBorder,
                                selectedBorderColor = KynectiaLime
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Mis Rutinas (Personalizadas)
                    item {
                        FilterChip(
                            selected = state.showOnlyCustom,
                            onClick = { viewModel.onShowOnlyCustomSelected() },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (state.customCount > 0) "Mis Rutinas (${state.customCount})" else "Mis Rutinas"
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CianTech,
                                selectedLabelColor = Color(0xFF0D1117),
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = state.showOnlyCustom,
                                borderColor = DarkBorder,
                                selectedBorderColor = CianTech
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Dificultades
                    val difficulties = listOf(
                        "Principiante" to "beginner",
                        "Intermedio" to "intermediate",
                        "Avanzado" to "advanced"
                    )
                    items(difficulties) { (label, value) ->
                        val isSelected = !state.showOnlyCustom && state.selectedDifficulty == value
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
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
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = if (state.showOnlyCustom) CianTech else KynectiaLime,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (state.showOnlyCustom) "Aún no tienes rutinas personalizadas" else "No se encontraron rutinas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (state.showOnlyCustom)
                                    "Crea tu primera rutina a medida seleccionando ejercicios, series, repeticiones, descansos y pesos."
                                else
                                    "Intenta cambiar los filtros de búsqueda o dificultad.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            if (state.showOnlyCustom) {
                                Spacer(modifier = Modifier.height(18.dp))
                                Button(
                                    onClick = onCreateRoutine,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CianTech,
                                        contentColor = Color(0xFF0D1117)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Crear Mi Rutina", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                items(state.routines) { routine ->
                    WorkoutRoutineCard(
                        routine = routine,
                        onStartClick = { onStartWorkout(routine.id) },
                        onEditClick = if (routine.is_custom) { { onEditRoutine(routine.id) } } else null,
                        onDeleteClick = if (routine.is_custom) { { routineToDelete = routine } } else null,
                        onDuplicateClick = { viewModel.duplicateRoutine(routine) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun WorkoutRoutineCard(
    routine: WorkoutRoutine,
    onStartClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    onDuplicateClick: (() -> Unit)? = null
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

                // Badges en la parte superior
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge si es Rutina Personalizada
                    if (routine.is_custom) {
                        Surface(
                            color = CianTech,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = Color(0xFF0D1117),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PERSONALIZADA",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0D1117)
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = DarkBackground.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = routine.category,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    // Badge de Dificultad
                    Surface(
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
            }

            // Información de la rutina
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
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
                    }

                    // Acciones de Editar / Eliminar si es personalizada
                    if (routine.is_custom) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (onEditClick != null) {
                                IconButton(
                                    onClick = onEditClick,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Editar rutina",
                                        tint = CianTech,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (onDeleteClick != null) {
                                IconButton(
                                    onClick = onDeleteClick,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Eliminar rutina",
                                        tint = KynectiaOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    } else if (onDuplicateClick != null) {
                        IconButton(
                            onClick = onDuplicateClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Duplicar y personalizar",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
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
                            containerColor = AzulElectrico,
                            contentColor = Blanco
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "ENTRENAR",
                            fontFamily = Exo2,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}
