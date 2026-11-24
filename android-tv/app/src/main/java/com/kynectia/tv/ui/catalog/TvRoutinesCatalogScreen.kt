package com.kynectia.tv.ui.catalog

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kynectia.tv.data.WorkoutRoutine
import com.kynectia.tv.theme.*
import com.kynectia.tv.ui.TvDisplayOrientation
import com.kynectia.tv.ui.common.TvBrandLogo
import com.kynectia.tv.ui.workout.TvRemoteActionButton

@Composable
fun TvRoutinesCatalogScreen(
    routines: List<WorkoutRoutine>,
    selectedCategory: String?,
    selectedDifficulty: String?,
    isOffline: Boolean,
    orientationMode: TvDisplayOrientation,
    isSoundEnabled: Boolean,
    onFilterChange: (category: String?, difficulty: String?) -> Unit,
    onStartRoutine: (WorkoutRoutine) -> Unit,
    onOpenPairing: () -> Unit,
    onRotateScreen: () -> Unit,
    onToggleSound: () -> Unit
) {
    var activeRoutine by remember(routines) {
        mutableStateOf(routines.firstOrNull())
    }

    val filterOptions = listOf(
        "Todas" to (null to null),
        "Fuerza" to ("Fuerza" to null),
        "Tren Inferior" to ("Tren Inferior" to null),
        "Full Body" to ("Full Body" to null),
        "Principiante" to (null to "beginner"),
        "Intermedio" to (null to "intermediate"),
        "Avanzado" to (null to "advanced")
    )

    val firstCardFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(350)
        try {
            firstCardFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        // TOP HEADER BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TvBrandLogo(
                    size = 42.dp,
                    showText = true,
                    subtitle = "ENTRENADOR AUTÓNOMO TV"
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Offline / Online Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isOffline) Color(0x33FF5E3A) else Color(0x3300E0FF),
                    border = BorderStroke(1.dp, if (isOffline) KynectiaOrange else CianTech)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isOffline) KynectiaOrange else CianTech)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOffline) "MODO LOCAL / SIN CONEXIÓN" else "CONECTADO A LA RED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOffline) KynectiaOrange else CianTech
                        )
                    }
                }
            }

            // Remote Actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TvRemoteActionButton(
                    icon = Icons.Default.Tv,
                    label = "Vincular Móvil",
                    shortcutKey = "V",
                    onClick = onOpenPairing
                )

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

        // CATEGORY CHIPS ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            filterOptions.forEach { (label, pair) ->
                val (cat, diff) = pair
                val isSelected = selectedCategory == cat && selectedDifficulty == diff
                TvFilterChip(
                    label = label,
                    isSelected = isSelected,
                    onClick = { onFilterChange(cat, diff) }
                )
            }
        }

        // MAIN CONTENT AREA: List of routines (Left) + Detail Preview (Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // LEFT COLUMN: Routine Cards Carousel / List
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
            ) {
                if (routines.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No se encontraron rutinas para este filtro",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(routines) { index, routine ->
                            TvRoutineCard(
                                routine = routine,
                                isSelected = activeRoutine?.id == routine.id,
                                onFocused = { activeRoutine = routine },
                                onActionClick = { onStartRoutine(routine) },
                                modifier = if (index == 0) Modifier.focusRequester(firstCardFocusRequester) else Modifier
                            )
                        }
                    }
                }
            }

            // RIGHT COLUMN: Selected Routine Deep Breakdown & Play Button
            Card(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.5.dp, DarkBorder)
            ) {
                val routine = activeRoutine
                if (routine != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            // Goal Badge and Duration
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = DarkSurfaceVariant
                                ) {
                                    Text(
                                        text = routine.category.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = KynectiaLime,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = CianTech,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${routine.estimated_duration_minutes} min",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = routine.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = routine.description ?: "Rutina diseñada para máxima hipertrofia y acondicionamiento biomecánico.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "SECUENCIA DE EJERCICIOS (${routine.exercises.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = KynectiaLime,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Exercise sequence list
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed(routine.exercises) { idx, ex ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = DarkSurfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = "#${idx + 1}",
                                                    fontWeight = FontWeight.Black,
                                                    color = CianTech,
                                                    fontSize = 12.sp
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = ex.exercise_name ?: "Ejercicio",
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = "${ex.sets}x${ex.reps} • ${ex.recommended_weight_kg?.toInt() ?: 0}kg",
                                                fontWeight = FontWeight.Bold,
                                                color = KynectiaLime,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Big Play / Start Workout on TV Button
                        TvRemoteActionButton(
                            icon = Icons.Default.PlayArrow,
                            label = "INICIAR ENTRENAMIENTO EN TV",
                            shortcutKey = "OK",
                            isPrimary = true,
                            onClick = { onStartRoutine(routine) }
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Selecciona una rutina para ver el desglose",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TvFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = tween(150),
        label = "chip_scale"
    )

    val bgColor by animateColorAsState(
        targetValue = when {
            isSelected -> KynectiaLime
            isFocused -> Color(0xFF223610)
            else -> DarkSurface
        },
        label = "chip_bg"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color(0xFF0D1117)
            isFocused -> KynectiaLime
            else -> TextSecondary
        },
        label = "chip_text"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> KynectiaLime
            isSelected -> KynectiaLime
            else -> DarkBorder
        },
        label = "chip_border"
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(if (isFocused || isSelected) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected || isFocused) FontWeight.Black else FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun TvRoutineCard(
    routine: WorkoutRoutine,
    isSelected: Boolean,
    onFocused: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.03f else 1.0f,
        animationSpec = tween(160),
        label = "card_scale"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> KynectiaLime
            isSelected -> Color(0xFF72A700)
            else -> DarkBorder
        },
        label = "card_border"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .clickable {
                onFocused()
                onActionClick()
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(if (isFocused) 2.5.dp else if (isSelected) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover Image thumbnail with rounded corners
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 75.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
            ) {
                AsyncImage(
                    model = routine.cover_image_url ?: "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=800",
                    contentDescription = routine.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xAA000000))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Routine Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val diffLabel = when (routine.difficulty_level.lowercase()) {
                        "beginner" -> "Principiante"
                        "advanced" -> "Avanzado"
                        else -> "Intermedio"
                    }
                    val diffColor = when (routine.difficulty_level.lowercase()) {
                        "beginner" -> Color(0xFF10B981)
                        "advanced" -> KynectiaOrange
                        else -> Color(0xFFF59E0B)
                    }

                    Text(
                        text = diffLabel.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = diffColor
                    )

                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = TextMuted
                    )

                    Text(
                        text = "${routine.exercises.size} ejercicios",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = routine.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused) KynectiaLime else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = routine.goal,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Action Indicator
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = null,
                tint = if (isFocused) KynectiaLime else TextMuted,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
