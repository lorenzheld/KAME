package com.example.kame.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    // Workouts
    @Query("SELECT * FROM workouts ORDER BY createdAt DESC")
    fun getAllWorkouts(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    fun getWorkout(workoutId: String): Flow<WorkoutEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity)

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)  // ✅ NEU

    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntity)

    // Exercises
    @Query("SELECT * FROM exercises WHERE workoutId = :workoutId ORDER BY `order` ASC")
    fun getExercisesForWorkout(workoutId: String): Flow<List<ExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)

    // Workout Sessions
    @Query("SELECT * FROM workout_sessions WHERE exerciseId = :exerciseId ORDER BY timestamp DESC")
    fun getSessionsForExercise(exerciseId: String): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE exerciseId IN (SELECT id FROM exercises WHERE workoutId = :workoutId) ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastSessionForWorkout(workoutId: String): WorkoutSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity)

    @Delete
    suspend fun deleteSession(session: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE exerciseId IN (SELECT id FROM exercises WHERE workoutId = :workoutId)")
    suspend fun deleteSessionsForWorkout(workoutId: String)
}