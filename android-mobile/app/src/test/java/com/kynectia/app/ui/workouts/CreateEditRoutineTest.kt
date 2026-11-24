package com.kynectia.app.ui.workouts

import com.kynectia.app.data.model.RoutineExerciseDetail
import com.kynectia.app.data.model.WorkoutRoutine
import com.kynectia.app.ui.workouts.create.CreateEditRoutineUiState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class CreateEditRoutineTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    @Test
    fun testValidationRequiresNameAndExercises() {
        val emptyState = CreateEditRoutineUiState(name = "", exercises = emptyList())
        assertFalse("Debe ser inválida si no tiene nombre ni ejercicios", emptyState.isValid)

        val nameOnlyState = CreateEditRoutineUiState(name = "Mi Rutina", exercises = emptyList())
        assertFalse("Debe ser inválida si no tiene ejercicios", nameOnlyState.isValid)

        val exercisesOnlyState = CreateEditRoutineUiState(
            name = "",
            exercises = listOf(RoutineExerciseDetail(exercise_id = 1, exercise_name = "Press Banca"))
        )
        assertFalse("Debe ser inválida si el nombre está en blanco", exercisesOnlyState.isValid)

        val validState = CreateEditRoutineUiState(
            name = "Torso Pesado",
            exercises = listOf(RoutineExerciseDetail(exercise_id = 1, exercise_name = "Press Banca"))
        )
        assertTrue("Debe ser válida cuando tiene nombre y ejercicios", validState.isValid)
    }

    @Test
    fun testCalculatedSetsAndDuration() {
        val exercise1 = RoutineExerciseDetail(
            exercise_id = 1,
            exercise_name = "Press de Banca",
            sets = 4,
            reps = 10,
            rest_seconds = 60,
            recommended_weight_kg = 80.0
        )
        val exercise2 = RoutineExerciseDetail(
            exercise_id = 2,
            exercise_name = "Dominadas",
            sets = 3,
            reps = 8,
            rest_seconds = 90,
            recommended_weight_kg = 0.0
        )

        val state = CreateEditRoutineUiState(
            name = "Fuerza Tren Superior",
            exercises = listOf(exercise1, exercise2)
        )

        assertEquals("Total de series debe ser 7", 7, state.totalSets)
        assertTrue("La duración calculada debe ser mayor a 0", state.calculatedDurationMinutes >= 15)
    }

    @Test
    fun testCustomRoutineSerialization() {
        val routine = WorkoutRoutine(
            id = -1050,
            name = "Rutina Personalizada",
            description = "Mi rutina customizada para piernas",
            category = "Tren Inferior",
            difficulty_level = "intermediate",
            estimated_duration_minutes = 45,
            is_custom = true,
            exercises = listOf(
                RoutineExerciseDetail(
                    id = -1,
                    sequence_order = 1,
                    exercise_id = 2,
                    exercise_name = "Sentadilla Libre Profunda",
                    sets = 4,
                    reps = 8,
                    rest_seconds = 90,
                    recommended_weight_kg = 100.0,
                    specific_instructions = "Bajar profundo rompiendo paralelo"
                )
            )
        )

        val encoded = json.encodeToString(routine)
        val decoded = json.decodeFromString<WorkoutRoutine>(encoded)

        assertEquals(routine.id, decoded.id)
        assertEquals(routine.name, decoded.name)
        assertTrue("Debe mantener la marca is_custom = true", decoded.is_custom)
        assertEquals(1, decoded.exercises.size)
        assertEquals(4, decoded.exercises[0].sets)
        assertEquals(8, decoded.exercises[0].reps)
        assertEquals(90, decoded.exercises[0].rest_seconds)
        assertEquals(100.0, decoded.exercises[0].recommended_weight_kg)
        assertEquals("Bajar profundo rompiendo paralelo", decoded.exercises[0].specific_instructions)
    }
}
