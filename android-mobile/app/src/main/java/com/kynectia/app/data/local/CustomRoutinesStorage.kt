package com.kynectia.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.kynectia.app.data.model.WorkoutRoutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CustomRoutinesStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("kynectia_custom_routines_prefs", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    companion object {
        private const val KEY_CUSTOM_ROUTINES = "user_custom_routines_list"
    }

    @Synchronized
    fun getAll(): List<WorkoutRoutine> {
        val serialized = prefs.getString(KEY_CUSTOM_ROUTINES, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<WorkoutRoutine>>(serialized)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun getById(id: Int): WorkoutRoutine? {
        return getAll().find { it.id == id }
    }

    @Synchronized
    fun save(routine: WorkoutRoutine): WorkoutRoutine {
        val currentList = getAll().toMutableList()
        val routineToSave: WorkoutRoutine
        val existingIndex = currentList.indexOfFirst { it.id == routine.id && routine.id != 0 }

        if (existingIndex >= 0) {
            routineToSave = routine.copy(is_custom = true)
            currentList[existingIndex] = routineToSave
        } else {
            // Generar ID negativo único para evitar colisiones con IDs autoincrementales de PostgreSQL
            val rawId = -(System.currentTimeMillis() % 1_000_000L).toInt() - 1000
            val newId = if (routine.id < 0) routine.id else rawId
            routineToSave = routine.copy(id = newId, is_custom = true)
            currentList.add(0, routineToSave)
        }

        val serialized = json.encodeToString(currentList)
        prefs.edit().putString(KEY_CUSTOM_ROUTINES, serialized).apply()
        return routineToSave
    }

    @Synchronized
    fun delete(id: Int): Boolean {
        val currentList = getAll().toMutableList()
        val removed = currentList.removeAll { it.id == id }
        if (removed) {
            val serialized = json.encodeToString(currentList)
            prefs.edit().putString(KEY_CUSTOM_ROUTINES, serialized).apply()
        }
        return removed
    }

    @Synchronized
    fun clear() {
        prefs.edit().remove(KEY_CUSTOM_ROUTINES).apply()
    }
}
