package com.example.kame.data

import android.content.Context
import com.example.kame.data.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object SampleData {
    fun insertSampleDataIfNeeded(context: Context, dao: WorkoutDao, scope: CoroutineScope) {
        scope.launch {
            // ✅ Prüfen ob bereits Workouts existieren
            val existingWorkouts = dao.getAllWorkouts().first()

            if (existingWorkouts.isEmpty()) {
                // Nur einfügen wenn DB leer ist
                insertSampleData(dao)
            }
        }
    }

    private suspend fun insertSampleData(dao: WorkoutDao) {
        // Insert Push Day
        dao.insertWorkout(
            WorkoutEntity(
                id = "workout_1",
                name = "Push Day Oberkörper",
                muscleGroups = listOf("Brust", "Schulter", "Trizeps"),
                exerciseCount = 4,
                createdAt = "2025-10-15T08:00:00Z",
                lastPerformed = "2025-11-01T14:30:00Z"
            )
        )

        dao.insertExercises(
            listOf(
                ExerciseEntity(
                    id = "exercise_1",
                    workoutId = "workout_1",
                    name = "Bankdrücken",
                    muscleGroups = listOf("Brust", "Trizeps"),
                    order = 1,
                    sets = listOf(
                        PlannedSet(1, 8, 80f),
                        PlannedSet(2, 8, 80f),
                        PlannedSet(3, 8, 80f)
                    )
                ),
                ExerciseEntity(
                    id = "exercise_2",
                    workoutId = "workout_1",
                    name = "Schrägbankdrücken",
                    muscleGroups = listOf("Brust"),
                    order = 2,
                    sets = listOf(
                        PlannedSet(1, 10, 55f),
                        PlannedSet(2, 10, 55f),
                        PlannedSet(3, 10, 55f)
                    )
                ),
                ExerciseEntity(
                    id = "exercise_3",
                    workoutId = "workout_1",
                    name = "Schulterdrücken",
                    muscleGroups = listOf("Schulter"),
                    order = 3,
                    sets = listOf(
                        PlannedSet(1, 10, 40f),
                        PlannedSet(2, 10, 40f),
                        PlannedSet(3, 10, 40f)
                    )
                ),
                ExerciseEntity(
                    id = "exercise_4",
                    workoutId = "workout_1",
                    name = "Trizeps Dips",
                    muscleGroups = listOf("Trizeps"),
                    order = 4,
                    sets = listOf(
                        PlannedSet(1, 12, 0f),
                        PlannedSet(2, 12, 0f),
                        PlannedSet(3, 12, 0f)
                    )
                )
            )
        )

        // Insert Pull Day
        dao.insertWorkout(
            WorkoutEntity(
                id = "workout_2",
                name = "Pull Day Rücken",
                muscleGroups = listOf("Rücken", "Bizeps", "Nacken"),
                exerciseCount = 4,
                createdAt = "2025-10-15T08:00:00Z",
                lastPerformed = "2025-10-31T16:00:00Z"
            )
        )

        dao.insertExercises(
            listOf(
                ExerciseEntity(
                    id = "exercise_5",
                    workoutId = "workout_2",
                    name = "Kreuzheben",
                    muscleGroups = listOf("Rücken", "Gesäß", "Beine"),
                    order = 1,
                    sets = listOf(
                        PlannedSet(1, 6, 120f),
                        PlannedSet(2, 6, 120f),
                        PlannedSet(3, 6, 120f)
                    )
                ),
                ExerciseEntity(
                    id = "exercise_6",
                    workoutId = "workout_2",
                    name = "Klimmzüge",
                    muscleGroups = listOf("Rücken", "Bizeps"),
                    order = 2,
                    sets = listOf(
                        PlannedSet(1, 10, 0f),
                        PlannedSet(2, 10, 0f),
                        PlannedSet(3, 10, 0f)
                    )
                ),
                ExerciseEntity(
                    id = "exercise_7",
                    workoutId = "workout_2",
                    name = "Rudern Langhantel",
                    muscleGroups = listOf("Rücken"),
                    order = 3,
                    sets = listOf(
                        PlannedSet(1, 10, 70f),
                        PlannedSet(2, 10, 70f),
                        PlannedSet(3, 10, 70f)
                    )
                ),
                ExerciseEntity(
                    id = "exercise_8",
                    workoutId = "workout_2",
                    name = "Bizeps Curls",
                    muscleGroups = listOf("Bizeps"),
                    order = 4,
                    sets = listOf(
                        PlannedSet(1, 12, 15f),
                        PlannedSet(2, 12, 15f),
                        PlannedSet(3, 12, 15f)
                    )
                )
            )
        )

        // Insert sample session
        dao.insertSession(
            WorkoutSessionEntity(
                exerciseId = "exercise_1",
                timestamp = "2025-11-01T14:30:00Z",
                sets = listOf(
                    CompletedSet(1, 8, 80f, true),
                    CompletedSet(2, 8, 80f, true),
                    CompletedSet(3, 7, 80f, true)
                ),
                notes = "Sehr gutes Training!"
            )
        )
    }
}