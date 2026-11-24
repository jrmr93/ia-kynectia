package com.kynectia.app.ui.workouts.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kynectia.app.data.model.RoutineExerciseDetail
import com.kynectia.app.theme.*

@Composable
fun RoutineExerciseItemCard(
    index: Int,
    exercise: RoutineExerciseDetail,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onSetsChange: (Int) -> Unit,
    onRepsChange: (Int) -> Unit,
    onWeightChange: (Double) -> Unit,
    onRestChange: (Int) -> Unit,
    onDurationChange: (Int) -> Unit,
    onInstructionsChange: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    var showInstructionsInput by remember { mutableStateOf(!exercise.specific_instructions.isNullOrBlank()) }
    var showWeightDialog by remember { mutableStateOf(false) }

    if (showWeightDialog) {
        CustomWeightInputDialog(
            initialWeight = exercise.recommended_weight_kg ?: 0.0,
            onConfirm = { weight ->
                onWeightChange(weight)
                showWeightDialog = false
            },
            onDismiss = { showWeightDialog = false }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header del ejercicio
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Número de orden
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(KynectiaLime),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        color = Color(0xFF0D1117),
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Miniatura
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!exercise.demo_image_url.isNullOrBlank()) {
                        AsyncImage(
                            model = exercise.demo_image_url,
                            contentDescription = exercise.exercise_name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("🏋️", fontSize = 20.sp)
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Nombre y resumen rápido
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isExpanded = !isExpanded }
                ) {
                    Text(
                        text = exercise.exercise_name ?: "Ejercicio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "${exercise.sets} series × ${exercise.reps} reps • ${exercise.recommended_weight_kg ?: 0.0} kg",
                        style = MaterialTheme.typography.bodySmall,
                        color = CianTech
                    )
                }

                // Acciones de Reordenamiento y Borrado
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = !isFirst,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = "Mover arriba",
                            tint = if (!isFirst) TextPrimary else DarkBorder,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onMoveDown,
                        enabled = !isLast,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowDownward,
                            contentDescription = "Mover abajo",
                            tint = if (!isLast) TextPrimary else DarkBorder,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar",
                            tint = KynectiaOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expandir/Colapsar",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(color = DarkBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Series y Repeticiones
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Series
                        StepperControl(
                            label = "SERIES",
                            value = exercise.sets,
                            unit = "series",
                            onDecrement = { onSetsChange(exercise.sets - 1) },
                            onIncrement = { onSetsChange(exercise.sets + 1) },
                            modifier = Modifier.weight(1f)
                        )

                        // Repeticiones
                        StepperControl(
                            label = "REPS",
                            value = exercise.reps,
                            unit = "reps",
                            onDecrement = { onRepsChange(exercise.reps - 1) },
                            onIncrement = { onRepsChange(exercise.reps + 1) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Chips rápidos de repeticiones
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Reps rápidas:", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        listOf(6, 8, 10, 12, 15, 20).forEach { r ->
                            val isSelected = exercise.reps == r
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onRepsChange(r) },
                                color = if (isSelected) KynectiaLime.copy(alpha = 0.2f) else DarkBackground,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "$r",
                                    color = if (isSelected) KynectiaLime else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Peso recomendado (kg)
                    val weight = exercise.recommended_weight_kg ?: 0.0
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBackground)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "PESO OBJETIVO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    letterSpacing = 0.5.sp
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { showWeightDialog = true }
                                ) {
                                    Text(
                                        text = if (weight > 0) "%.1f kg".format(weight) else "Corporal (0 kg)",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (weight > 0) KynectiaLime else CianTech,
                                        fontFamily = Exo2
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Editar peso exacto",
                                        tint = TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            // Stepper de peso
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = { onWeightChange((weight - 2.5).coerceAtLeast(0.0)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface, contentColor = TextPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("-2.5", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onWeightChange(weight + 2.5) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface, contentColor = TextPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("+2.5", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onWeightChange(weight + 5.0) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface, contentColor = TextPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("+5", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Presets de peso
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val presets = listOf(0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 80.0, 100.0)
                            items(presets) { p ->
                                val isSelected = weight == p
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onWeightChange(p) },
                                    color = if (isSelected) AzulElectrico else DarkSurface,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (p == 0.0) "0 kg" else "${p.toInt()} kg",
                                        color = if (isSelected) Blanco else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Descanso entre series
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBackground)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TIEMPO DE DESCANSO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = formatRestSeconds(exercise.rest_seconds),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary,
                                    fontFamily = Exo2
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = { onRestChange((exercise.rest_seconds - 15).coerceAtLeast(0)) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurface)
                                ) {
                                    Text("-15s", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }

                                IconButton(
                                    onClick = { onRestChange(exercise.rest_seconds + 15) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurface)
                                ) {
                                    Text("+15s", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                        }

                        // Presets de descanso
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val restPresets = listOf(30, 45, 60, 90, 120, 180)
                            items(restPresets) { sec ->
                                val isSelected = exercise.rest_seconds == sec
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onRestChange(sec) },
                                    color = if (isSelected) CianTech else DarkSurface,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${sec}s",
                                        color = if (isSelected) Color(0xFF0D1117) else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Instrucciones o notas para el ejercicio
                    if (!showInstructionsInput) {
                        TextButton(
                            onClick = { showInstructionsInput = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = CianTech),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Agregar notas técnicas o instrucciones", fontSize = 12.sp)
                        }
                    } else {
                        OutlinedTextField(
                            value = exercise.specific_instructions ?: "",
                            onValueChange = onInstructionsChange,
                            label = { Text("Instrucciones específicas (opcional)", color = TextSecondary) },
                            placeholder = { Text("Ej. Pausa de 2 segundos en contracción máxima...", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CianTech,
                                unfocusedBorderColor = DarkBorder,
                                focusedContainerColor = DarkBackground,
                                unfocusedContainerColor = DarkBackground,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            maxLines = 3,
                            trailingIcon = {
                                IconButton(onClick = {
                                    onInstructionsChange("")
                                    showInstructionsInput = false
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Eliminar notas", tint = TextMuted)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepperControl(
    label: String,
    value: Int,
    unit: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkBackground)
            .padding(10.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Restar", tint = TextPrimary, modifier = Modifier.size(16.dp))
            }

            Text(
                text = "$value",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                fontFamily = Exo2
            )

            IconButton(
                onClick = onIncrement,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Sumar", tint = TextPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun CustomWeightInputDialog(
    initialWeight: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember { mutableStateOf(initialWeight.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                "Ingresar Peso Objetivo",
                color = TextPrimary,
                fontFamily = Exo2,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "Introduce el peso en kilogramos (kg) para este ejercicio:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    suffix = { Text("kg", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KynectiaLime,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = textValue.toDoubleOrNull() ?: initialWeight
                    onConfirm(parsed.coerceAtLeast(0.0))
                },
                colors = ButtonDefaults.buttonColors(containerColor = KynectiaLime, contentColor = Color(0xFF0D1117))
            ) {
                Text("Confirmar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

private fun formatRestSeconds(seconds: Int): String {
    return if (seconds < 60) {
        "${seconds} s"
    } else {
        val min = seconds / 60
        val sec = seconds % 60
        if (sec > 0) "$min min $sec s" else "$min min"
    }
}
