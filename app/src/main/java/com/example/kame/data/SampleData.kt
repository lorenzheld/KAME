package com.example.kame.data

import android.content.Context
import android.util.Log
import com.example.kame.data.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object SampleData {
    fun insertSampleDataIfNeeded(context: Context, dao: WorkoutDao, scope: CoroutineScope) {
        scope.launch {
            try {
                // Prüfen ob bereits Workouts existieren
                val existingWorkouts = dao.getAllWorkouts().first()

                if (existingWorkouts.isEmpty()) {
                    Log.d("SampleData", "Inserting sample data...")
                    insertSampleData(dao)
                    Log.d("SampleData", "Sample data inserted!")
                } else {
                    Log.d("SampleData", "Sample data already exists (${existingWorkouts.size} workouts)")
                }
            } catch (e: Exception) {
                Log.e("SampleData", "Error inserting sample data", e)
            }
        }
    }

    private suspend fun insertSampleData(dao: WorkoutDao) {
        // ========== Push Day ==========
        Log.d("SampleData", "Inserting Push Day workout...")
        dao.insertWorkout(
            WorkoutEntity(
                id = "workout_1",
                name = "Push Day Oberkörper",
                muscleGroups = listOf("Brust", "Schulter", "Trizeps"),
                exerciseCount = 4,
                createdAt = "2025-10-15T08:00:00Z",
                lastPerformed = null
            )
        )

        Log.d("SampleData", "Inserting Push Day exercises...")
        val pushExercises = listOf(
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

        Log.d("SampleData", "About to insert ${pushExercises.size} exercises for Push Day")
        dao.insertExercises(pushExercises)
        Log.d("SampleData", "Push Day exercises inserted!")

        // Verify
        val verifyPush = dao.getExercisesForWorkout("workout_1").first()
        Log.d("SampleData", "Verification: workout_1 now has ${verifyPush.size} exercises")

        // ========== Pull Day ==========
        Log.d("SampleData", "Inserting Pull Day workout...")
        dao.insertWorkout(
            WorkoutEntity(
                id = "workout_2",
                name = "Pull Day Rücken",
                muscleGroups = listOf("Rücken", "Bizeps", "Nacken"),
                exerciseCount = 4,
                createdAt = "2025-10-15T08:00:00Z",
                lastPerformed = null
            )
        )

        Log.d("SampleData", "Inserting Pull Day exercises...")
        val pullExercises = listOf(
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

        Log.d("SampleData", "About to insert ${pullExercises.size} exercises for Pull Day")
        dao.insertExercises(pullExercises)
        Log.d("SampleData", "Pull Day exercises inserted!")

        // Verify
        val verifyPull = dao.getExercisesForWorkout("workout_2").first()
        Log.d("SampleData", "Verification: workout_2 now has ${verifyPull.size} exercises")

        Log.d("SampleData", "All sample data inserted successfully!")
    }
}