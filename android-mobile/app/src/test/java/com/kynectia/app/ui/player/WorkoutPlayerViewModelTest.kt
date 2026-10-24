package com.kynectia.app.ui.player

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class WorkoutPlayerViewModelTest {

    @Test
    fun testDefaultPlayerUiState() {
        val state = PlayerUiState()
        assertEquals(0, state.currentExerciseIndex)
        assertEquals(1, state.currentSetNumber)
        assertFalse(state.isResting)
        assertEquals(10, state.targetReps)
        assertEquals(0, state.selectedSetIndex)
        assertTrue(state.exerciseSetsMap.isEmpty())
    }

    @Test
    fun testPlayerUiStateUpdate() {
        val sets = listOf(
            ExerciseSetState(1, 60.0, 8, isCompleted = true),
            ExerciseSetState(2, 60.0, 8, isCompleted = false)
        )
        val state = PlayerUiState(
            currentExerciseIndex = 2,
            currentSetNumber = 3,
            isResting = true,
            restSecondsRemaining = 45,
            exerciseSetsMap = mapOf(101 to sets),
            selectedSetIndex = 1
        )
        assertEquals(2, state.currentExerciseIndex)
        assertEquals(3, state.currentSetNumber)
        assertTrue(state.isResting)
        assertEquals(45, state.restSecondsRemaining)
        assertEquals(1, state.selectedSetIndex)
        assertEquals(2, state.exerciseSetsMap[101]?.size)
    }

    @Test
    fun testEpleyOneRmCalculationMatchesDesign() {
        // En la imagen: 60 kg x 8 reps -> 1RM: 76
        val weight = 60.0
        val reps = 8
        val calculatedOneRm = (weight * (1.0 + reps / 30.0)).toInt()
        assertEquals(76, calculatedOneRm)
    }
}
