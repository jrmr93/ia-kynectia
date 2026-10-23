package com.kynectia.app.data.api

import com.kynectia.app.data.model.*

object MockDataProvider {

    val mockMuscles = listOf(
        MuscleGroup(id = 1, name = "Pectorales", description = "Músculo pectoral mayor y menor"),
        MuscleGroup(id = 2, name = "Dorsales", description = "Espalda alta y dorsal ancho"),
        MuscleGroup(id = 3, name = "Cuádriceps", description = "Parte frontal de las piernas"),
        MuscleGroup(id = 4, name = "Deltoides", description = "Hombros anteriores, laterales y posteriores"),
        MuscleGroup(id = 5, name = "Bíceps", description = "Bíceps braquial"),
        MuscleGroup(id = 6, name = "Tríceps", description = "Tríceps braquial"),
        MuscleGroup(id = 7, name = "Core / Abdominales", description = "Zona media y estabilidad lumbar")
    )

    val mockExercises = listOf(
        Exercise(
            id = 1,
            name = "Press de Banca con Barra",
            description = "Ejercicio compuesto fundamental para el desarrollo del tren superior.",
            execution_instructions = "1. Acuéstate en el banco plano con los ojos bajo la barra.\n2. Agarra la barra con ancho mayor a los hombros.\n3. Desciende la barra con control hasta tocar el pecho medio.\n4. Empuja con fuerza explosiva hacia arriba sin bloquear codos.",
            difficulty_level = "intermediate",
            exercise_type = "strength",
            demo_image_url = "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=800",
            demo_video_url = "/uploads/videos/exercise-curl-biceps-mancuerna-1792723125436-422268080.mp4",
            safety_recommendations = "Mantén los pies firmes en el suelo y no arquees la espalda excesivamente.",
            primary_muscle_name = "Pectorales",
            equipment_names = listOf("Barra Olímpica", "Banco Plano")
        ),
        Exercise(
            id = 2,
            name = "Sentadilla Libre Profunda",
            description = "El rey de los ejercicios de piernas para hipertrofia y fuerza funcional.",
            execution_instructions = "1. Coloca la barra sobre los trapecios.\n2. Pies a la anchura de los hombros, puntas ligeramente hacia afuera.\n3. Desciende doblando rodillas y caderas hasta romper el paralelo.\n4. Sube empujando con los talones.",
            difficulty_level = "advanced",
            exercise_type = "strength",
            demo_image_url = "https://images.unsplash.com/photo-1574680096145-d05b474e2155?w=800",
            demo_video_url = "/uploads/videos/exercise-curl-biceps-mancuerna-1792723125436-422268080.mp4",
            safety_recommendations = "Mantén la mirada al frente y la columna neutra durante todo el recorrido.",
            primary_muscle_name = "Cuádriceps",
            equipment_names = listOf("Barra Olímpica", "Rack de Sentadillas")
        ),
        Exercise(
            id = 3,
            name = "Dominadas Pronadas",
            description = "Tracción vertical para ensanchar la espalda y desarrollar la fuerza de agarre.",
            execution_instructions = "1. Agarra la barra con palmas al frente.\n2. Inicia el movimiento retrayendo escápulas.\n3. Tracciona hasta que la barbilla supere la barra.\n4. Desciende de forma controlada hasta extensión completa.",
            difficulty_level = "intermediate",
            exercise_type = "strength",
            demo_image_url = "https://images.unsplash.com/photo-1598971639058-fab3c3109a00?w=800",
            demo_video_url = "/uploads/videos/exercise-curl-biceps-mancuerna-1792723125436-422268080.mp4",
            safety_recommendations = "Evita el balanceo con las piernas para no perder tensión en los dorsales.",
            primary_muscle_name = "Dorsales",
            equipment_names = listOf("Barra de Dominadas")
        ),
        Exercise(
            id = 4,
            name = "Press Militar con Mancuernas",
            description = "Empuje vertical para hombros fuertes y estabilidad escapular.",
            execution_instructions = "1. Sentado en banco a 90 grados, mancuernas a la altura de las orejas.\n2. Empuja las mancuernas hacia arriba en arco suave.\n3. Junta casi al tope sin chocar.\n4. Desciende con control.",
            difficulty_level = "intermediate",
            exercise_type = "strength",
            demo_image_url = "https://images.unsplash.com/photo-1583454110551-21f2fa2afe61?w=800",
            demo_video_url = "/uploads/videos/exercise-curl-biceps-mancuerna-1792723125436-422268080.mp4",
            safety_recommendations = "No impulses con la espalda baja; mantén el abdomen contraído.",
            primary_muscle_name = "Deltoides",
            equipment_names = listOf("Mancuernas", "Banco Ajustable")
        ),
        Exercise(
            id = 5,
            name = "Curl de Bíceps en Banco Inclinado",
            description = "Aislamiento del bíceps con énfasis en la cabeza larga gracias al estiramiento inicial.",
            execution_instructions = "1. Siéntate en banco inclinado a 60 grados con mancuernas colgando.\n2. Flexiona los codos manteniendo los brazos fijos hacia atrás.\n3. Aprieta en la cima 1 segundo y baja lento en 3 segundos.",
            difficulty_level = "beginner",
            exercise_type = "strength",
            demo_image_url = "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=800",
            demo_video_url = "/uploads/videos/exercise-curl-biceps-mancuerna-1792723125436-422268080.mp4",
            safety_recommendations = "No uses impulso de los hombros para subir el peso.",
            primary_muscle_name = "Bíceps",
            equipment_names = listOf("Mancuernas", "Banco Ajustable")
        ),
        Exercise(
            id = 6,
            name = "Fondos en Paralelas (Dips)",
            description = "Constructor masivo de tríceps y porción inferior del pectoral.",
            execution_instructions = "1. Suspéndete en las barras paralelas con brazos extendidos.\n2. Desciende inclinando el torso ligeramente hasta 90 grados en codos.\n3. Empuja con tríceps y pecho hasta la posición inicial.",
            difficulty_level = "advanced",
            exercise_type = "strength",
            demo_image_url = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800",
            demo_video_url = "/uploads/videos/exercise-curl-biceps-mancuerna-1792723125436-422268080.mp4",
            safety_recommendations = "No desciendas más allá de los 90 grados si sientes molestias en los hombros.",
            primary_muscle_name = "Tríceps",
            equipment_names = listOf("Barras Paralelas")
        )
    )

    val mockRoutines = listOf(
        WorkoutRoutine(
            id = 1,
            name = "Torso Potencia e Hipertrofia",
            description = "Sesión completa para desarrollar fuerza y volumen en pecho, espalda y hombros.",
            cover_image_url = "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=800",
            goal = "Hipertrofia y Fuerza",
            difficulty_level = "intermediate",
            category = "Fuerza / Tren Superior",
            estimated_duration_minutes = 50,
            exercises = listOf(
                RoutineExerciseDetail(
                    id = 101,
                    sequence_order = 1,
                    exercise_id = 1,
                    exercise_name = "Press de Banca con Barra",
                    sets = 4,
                    reps = 8,
                    rest_seconds = 90,
                    recommended_weight_kg = 70.0,
                    specific_instructions = "Pausa de 1 segundo en el pecho antes de empujar.",
                    demo_video_url = "/uploads/videos/exercise-curl-biceps-mancuerna-1792723125436-422268080.mp4"
                ),
                RoutineExerciseDetail(
                    id = 102,
                    sequence_order = 2,
                    exercise_id = 3,
                    exercise_name = "Dominadas Pronadas",
                    sets = 4,
                    reps = 10,
                    rest_seconds = 75,
                    recommended_weight_kg = 0.0,
                    specific_instructions = "Si puedes más de 10, usa cinturón de lastre.",
                    demo_video_url = "/uploads/videos/exercise-curl-biceps-mancuerna-1792723125436-422268080.mp4"
                ),
                RoutineExerciseDetail(
                    id = 103,
                    sequence_order = 3,
                    exercise_id = 4,
                    exercise_name = "Press Militar con Mancuernas",
                    sets = 3,
                    reps = 10,
                    rest_seconds = 60,
                    recommended_weight_kg = 20.0,
                    specific_instructions = "Cadencia 2-0-1 controlada.",
                    demo_video_url = "/uploads/videos/exercise-curl-biceps-mancuerna-1792723125436-422268080.mp4"
                ),
                RoutineExerciseDetail(
                    id = 104,
                    sequence_order = 4,
                    exercise_id = 5,
                    exercise_name = "Curl de Bíceps en Banco Inclinado",
                    sets = 3,
                    reps = 12,
                    rest_seconds = 45,
                    recommended_weight_kg = 14.0,
                    specific_instructions = "Enfócate en el estiramiento en la parte inferior.",
                    demo_video_url = "/uploads/videos/exercise-curl-biceps-mancuerna-1792723125436-422268080.mp4"
                ),
                RoutineExerciseDetail(
                    id = 105,
                    sequence_order = 5,
                    exercise_id = 6,
                    exercise_name = "Fondos en Paralelas (Dips)",
                    sets = 3,
                    reps = 12,
                    rest_seconds = 60,
                    recommended_weight_kg = 0.0,
                    specific_instructions = "Bloqueo suave de tríceps en la cima.",
                    demo_video_url = "/uploads/videos/exercise-curl-biceps-mancuerna-1792723125436-422268080.mp4"
                )
            )
        ),
        WorkoutRoutine(
            id = 2,
            name = "Pierna Fuerte & Explosiva",
            description = "Enfoque en cuádriceps, glúteos e isquiosurales para potencia y rendimiento.",
            cover_image_url = "https://images.unsplash.com/photo-1574680096145-d05b474e2155?w=800",
            goal = "Fuerza de Piernas",
            difficulty_level = "advanced",
            category = "Tren Inferior",
            estimated_duration_minutes = 55,
            exercises = listOf(
                RoutineExerciseDetail(
                    id = 201,
                    sequence_order = 1,
                    exercise_id = 2,
                    exercise_name = "Sentadilla Libre Profunda",
                    sets = 5,
                    reps = 5,
                    rest_seconds = 120,
                    recommended_weight_kg = 90.0,
                    specific_instructions = "Series de fuerza pesadas con técnica impecable.",
                    demo_video_url = "/uploads/videos/exercise-curl-biceps-mancuerna-1792723125436-422268080.mp4"
                )
            )
        )
    )
}
