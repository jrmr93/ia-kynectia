package com.kynectia.app.ui.player

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import org.junit.Test

class WorkoutPlayerViewModelTest {

    @Test
    fun testDefaultPlayerUiState() {
        val state = PlayerUiState()
        assertEquals(0, state.currentExerciseIndex)
        assertEquals(1, state.currentSetNumber)
        assertFalse(state.isResting)
        assertEquals(10, state.targetReps)
    }

    @Test
    fun testPlayerUiStateUpdate() {
        val state = PlayerUiState(
            currentExerciseIndex = 2,
            currentSetNumber = 3,
            isResting = true,
            restSecondsRemaining = 45
        )
        assertEquals(2, state.currentExerciseIndex)
        assertEquals(3, state.currentSetNumber)
        assertEquals(true, state.isResting)
        assertEquals(45, state.restSecondsRemaining)
    }
}
